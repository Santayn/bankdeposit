package org.santayn.bankdeposit.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.DepositProduct;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.service.*;
import org.santayn.bankdeposit.ui.MoneyUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Контроллер вкладки "Договоры вкладов".
 *
 * Работает с DepositContractsView.fxml:
 * - фильтры по клиенту и номеру договора
 * - таблица договоров
 * - таблица операций выбранного договора
 * - блок открытия нового вклада и быстрых операций
 *
 * UI-доступ по ролям:
 * - управление договорами: ADMIN, MANAGER, OPERATOR
 * - финансовые операции: ADMIN, OPERATOR
 * - остальные роли: только просмотр
 */
@Component
@RequiredArgsConstructor
public class DepositContractsController {

    private final CustomerService customerService;
    private final DepositProductService depositProductService;
    private final DepositContractService depositContractService;
    private final DepositOperationService depositOperationService;

    private final SessionContext sessionContext;
    private final UiAccessManager uiAccessManager;

    // ---------------------- Фильтры (верхняя панель) ----------------------

    @FXML
    private ComboBox<Customer> customerFilterCombo;

    @FXML
    private TextField contractNumberSearchField;

    @FXML
    private Button filterButton;

    @FXML
    private Button resetFilterButton;

    // ---------------------- Таблица договоров ----------------------

    @FXML
    private TableView<DepositContract> contractsTable;

    @FXML
    private TableColumn<DepositContract, String> contractNumberColumn;

    @FXML
    private TableColumn<DepositContract, String> customerColumn;

    @FXML
    private TableColumn<DepositContract, String> productColumn;

    @FXML
    private TableColumn<DepositContract, String> statusColumn;

    @FXML
    private TableColumn<DepositContract, String> openDateColumn;

    @FXML
    private TableColumn<DepositContract, String> closeDateColumn;

    @FXML
    private TableColumn<DepositContract, String> initialAmountColumn;

    @FXML
    private TableColumn<DepositContract, String> currentBalanceColumn;

    @FXML
    private TableColumn<DepositContract, String> rateColumn;

    // ---------------------- Таблица операций ----------------------

    @FXML
    private TableView<DepositOperation> operationsTable;

    @FXML
    private TableColumn<DepositOperation, LocalDateTime> opDateTimeColumn;

    @FXML
    private TableColumn<DepositOperation, String> opTypeColumn;

    @FXML
    private TableColumn<DepositOperation, BigDecimal> opAmountColumn;

    @FXML
    private TableColumn<DepositOperation, String> opDescriptionColumn;

    // ---------------------- Блок открытия вклада ----------------------

    @FXML
    private ComboBox<Customer> openCustomerCombo;

    @FXML
    private ComboBox<DepositProduct> openProductCombo;

    @FXML
    private TextField openInitialAmountField;

    @FXML
    private Button openContractButton;

    // ---------------------- Блок быстрых операций ----------------------

    @FXML
    private Button depositButton;

    @FXML
    private Button withdrawButton;

    @FXML
    private Button closeContractButton;

    // ---------------------- Данные ----------------------

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<DepositProduct> products = FXCollections.observableArrayList();
    private final ObservableList<DepositContract> contracts = FXCollections.observableArrayList();
    private final ObservableList<DepositOperation> operations = FXCollections.observableArrayList();

    private DepositContract selectedContract;

    // ---------------------- Init ----------------------

    @FXML
    public void initialize() {
        setupCustomerCombos();
        setupProductCombo();
        setupContractsTable();
        setupOperationsTable();
        setupSelectionListener();

        loadDictionaries();
        loadContractsAll();

        applyRoleUiAccess();
        updateButtonsState();
    }

    // ---------------------- Setup UI ----------------------

    private void setupCustomerCombos() {
        customerFilterCombo.setItems(customers);
        openCustomerCombo.setItems(customers);

        StringConverter<Customer> converter = new StringConverter<>() {
            @Override
            public String toString(Customer c) {
                if (c == null) {
                    return "";
                }
                String middle = c.getMiddleName() != null && !c.getMiddleName().isBlank()
                        ? " " + c.getMiddleName().trim()
                        : "";
                return (safe(c.getLastName()) + " " + safe(c.getFirstName()) + middle).trim();
            }

            @Override
            public Customer fromString(String string) {
                return null;
            }
        };

        configureCustomerCombo(customerFilterCombo, converter);
        configureCustomerCombo(openCustomerCombo, converter);
    }

    private void configureCustomerCombo(ComboBox<Customer> combo, StringConverter<Customer> converter) {
        combo.setConverter(converter);

        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(converter.toString(item));
            }
        });

        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(converter.toString(item));
            }
        });
    }

    private void setupProductCombo() {
        openProductCombo.setItems(products);

        StringConverter<DepositProduct> converter = new StringConverter<>() {
            @Override
            public String toString(DepositProduct p) {
                return p == null ? "" : safe(p.getName());
            }

            @Override
            public DepositProduct fromString(String string) {
                return null;
            }
        };

        openProductCombo.setConverter(converter);

        openProductCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DepositProduct item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(converter.toString(item));
            }
        });

        openProductCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DepositProduct item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(converter.toString(item));
            }
        });
    }

    private void setupContractsTable() {
        contractNumberColumn.setCellValueFactory(new PropertyValueFactory<>("contractNumber"));

        customerColumn.setCellValueFactory(cell -> {
            Customer c = cell.getValue().getCustomer();
            return new SimpleStringProperty(formatCustomerShort(c));
        });

        productColumn.setCellValueFactory(cell -> {
            DepositProduct p = cell.getValue().getProduct();
            return new SimpleStringProperty(p != null ? safe(p.getName()) : "");
        });

        statusColumn.setCellValueFactory(cell -> {
            DepositContractStatus st = cell.getValue().getStatus();
            return new SimpleStringProperty(st != null ? st.name() : "");
        });

        openDateColumn.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue().getOpenDate();
            return new SimpleStringProperty(d != null ? d.toString() : "");
        });

        closeDateColumn.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue().getCloseDate();
            return new SimpleStringProperty(d != null ? d.toString() : "");
        });

        initialAmountColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(MoneyUtil.formatMoney(cell.getValue().getInitialAmount()))
        );

        currentBalanceColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(MoneyUtil.formatMoney(cell.getValue().getCurrentBalance()))
        );

        rateColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(MoneyUtil.formatRate(cell.getValue().getInterestRate()))
        );

        contractsTable.setItems(contracts);
    }

    private void setupOperationsTable() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        opDateTimeColumn.setCellValueFactory(new PropertyValueFactory<>("operationDateTime"));
        opDateTimeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.format(fmt));
            }
        });

        opAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        opDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        opTypeColumn.setCellValueFactory(cell -> {
            try {
                var t = cell.getValue().getType();
                return new SimpleStringProperty(t != null ? t.name() : "");
            } catch (Exception e) {
                return new SimpleStringProperty("");
            }
        });

        operationsTable.setItems(operations);
    }

    private void setupSelectionListener() {
        contractsTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldV, newV) -> {
                    selectedContract = newV;
                    loadOperationsForSelected();
                    updateButtonsState();
                });
    }

    // ---------------------- Role UI access ----------------------

    private void applyRoleUiAccess() {
        User current = sessionContext.getCurrentUser();

        boolean canManageContracts = uiAccessManager.canManageContracts(current);
        boolean canOperate = uiAccessManager.canOperateDeposits(current);

        openCustomerCombo.setDisable(!canManageContracts);
        openProductCombo.setDisable(!canManageContracts);
        openInitialAmountField.setDisable(!canManageContracts);
        openContractButton.setDisable(!canManageContracts);

        depositButton.setDisable(!canOperate);
        withdrawButton.setDisable(!canOperate);

        closeContractButton.setDisable(!canManageContracts);
    }

    // ---------------------- Load data ----------------------

    private void loadDictionaries() {
        customers.setAll(customerService.getAllCustomers());
        products.setAll(depositProductService.getAllProducts());
    }

    private void loadContractsAll() {
        contracts.setAll(depositContractService.getAllContracts());

        if (!contracts.isEmpty()) {
            contractsTable.getSelectionModel().selectFirst();
            selectedContract = contractsTable.getSelectionModel().getSelectedItem();
        } else {
            selectedContract = null;
        }

        loadOperationsForSelected();
        updateButtonsState();
    }

    private void loadOperationsForSelected() {
        if (selectedContract == null || selectedContract.getId() == null) {
            operations.clear();
            return;
        }

        try {
            operations.setAll(depositOperationService.getOperationsByContract(selectedContract.getId()));
        } catch (Exception e) {
            operations.clear();
        }
    }

    // ---------------------- Filters ----------------------

    @FXML
    public void onApplyFilter() {
        try {
            Customer filterCustomer = customerFilterCombo.getValue();
            String numberPart = contractNumberSearchField.getText();

            List<DepositContract> all = depositContractService.getAllContracts();

            List<DepositContract> filtered = all.stream()
                    .filter(c -> {
                        if (filterCustomer == null || filterCustomer.getId() == null) {
                            return true;
                        }
                        Customer cc = c.getCustomer();
                        return cc != null && Objects.equals(cc.getId(), filterCustomer.getId());
                    })
                    .filter(c -> {
                        if (numberPart == null || numberPart.isBlank()) {
                            return true;
                        }
                        String num = c.getContractNumber();
                        return num != null && num.contains(numberPart.trim());
                    })
                    .collect(Collectors.toList());

            contracts.setAll(filtered);

            if (!contracts.isEmpty()) {
                contractsTable.getSelectionModel().selectFirst();
                selectedContract = contractsTable.getSelectionModel().getSelectedItem();
            } else {
                selectedContract = null;
            }

            loadOperationsForSelected();
            updateButtonsState();

        } catch (Exception ex) {
            showError("Фильтр", ex.toString());
        }
    }

    @FXML
    public void onResetFilter() {
        customerFilterCombo.getSelectionModel().clearSelection();
        contractNumberSearchField.clear();
        loadContractsAll();
    }

    // ---------------------- Open contract ----------------------

    @FXML
    public void onOpenContract() {
        try {
            User current = sessionContext.getCurrentUser();
            if (!uiAccessManager.canManageContracts(current)) {
                showInfo("Открытие вклада", "Недостаточно прав для открытия вкладов.");
                return;
            }

            Customer customer = openCustomerCombo.getValue();
            DepositProduct product = openProductCombo.getValue();

            if (customer == null || customer.getId() == null) {
                showError("Открытие вклада", "Выберите клиента.");
                return;
            }
            if (product == null || product.getId() == null) {
                showError("Открытие вклада", "Выберите депозитный продукт.");
                return;
            }

            BigDecimal amount = parseMoneyUI(openInitialAmountField.getText(), "Начальная сумма");
            if (amount == null) {
                return;
            }

            DepositContract created = depositContractService.openContract(
                    customer.getId(),
                    product.getId(),
                    amount,
                    LocalDate.now()
            );

            openInitialAmountField.clear();

            reloadContractsAndSelect(created.getId());
            showInfo("Открытие вклада",
                    "Вклад успешно открыт. Номер договора: " + safe(created.getContractNumber()));

        } catch (InvalidOperationException | EntityNotFoundException ex) {
            showError("Открытие вклада", ex.getMessage());
        } catch (Exception ex) {
            showError("Неожиданная ошибка", ex.toString());
        }
    }

    // ---------------------- Quick operations ----------------------

    @FXML
    public void onDeposit() {
        User current = sessionContext.getCurrentUser();
        if (!uiAccessManager.canOperateDeposits(current)) {
            showInfo("Пополнение", "Недостаточно прав для операций.");
            return;
        }

        if (!ensureOpenSelected("Пополнение")) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Пополнение");
        dialog.setHeaderText(null);
        dialog.setContentText("Введите сумму пополнения:");

        dialog.showAndWait().ifPresent(text -> {
            BigDecimal amount = parseMoneyUI(text, "Пополнение");
            if (amount == null) {
                return;
            }

            try {
                depositOperationService.deposit(selectedContract.getId(), amount);

                reloadContractsAndSelect(selectedContract.getId());
                loadOperationsForSelected();

            } catch (InvalidOperationException | EntityNotFoundException ex) {
                showError("Пополнение", ex.getMessage());
            } catch (Exception ex) {
                showError("Неожиданная ошибка", ex.toString());
            }
        });
    }

    @FXML
    public void onWithdraw() {
        User current = sessionContext.getCurrentUser();
        if (!uiAccessManager.canOperateDeposits(current)) {
            showInfo("Снятие", "Недостаточно прав для операций.");
            return;
        }

        if (!ensureOpenSelected("Снятие")) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Снятие");
        dialog.setHeaderText(null);
        dialog.setContentText("Введите сумму снятия:");

        dialog.showAndWait().ifPresent(text -> {
            BigDecimal amount = parseMoneyUI(text, "Снятие");
            if (amount == null) {
                return;
            }

            try {
                depositOperationService.withdraw(selectedContract.getId(), amount);

                reloadContractsAndSelect(selectedContract.getId());
                loadOperationsForSelected();

            } catch (InvalidOperationException | EntityNotFoundException ex) {
                showError("Снятие", ex.getMessage());
            } catch (Exception ex) {
                showError("Неожиданная ошибка", ex.toString());
            }
        });
    }

    @FXML
    public void onCloseContract() {
        User current = sessionContext.getCurrentUser();
        if (!uiAccessManager.canManageContracts(current)) {
            showInfo("Закрытие вклада", "Недостаточно прав для закрытия вкладов.");
            return;
        }

        if (selectedContract == null || selectedContract.getId() == null) {
            showInfo("Закрытие вклада", "Сначала выберите договор.");
            return;
        }

        if (selectedContract.getStatus() != DepositContractStatus.OPEN) {
            showInfo("Закрытие вклада", "Выбранный вклад уже не является активным.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Закрытие вклада");
        confirm.setHeaderText(null);
        confirm.setContentText("Закрыть выбранный вклад?");

        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    DepositContract updated =
                            depositContractService.closeContract(selectedContract.getId(), LocalDate.now());

                    reloadContractsAndSelect(updated.getId());
                    loadOperationsForSelected();

                } catch (InvalidOperationException | EntityNotFoundException ex) {
                    showError("Закрытие вклада", ex.getMessage());
                } catch (Exception ex) {
                    showError("Неожиданная ошибка", ex.toString());
                }
            }
        });
    }

    // ---------------------- Helpers ----------------------

    private boolean ensureOpenSelected(String actionName) {
        if (selectedContract == null || selectedContract.getId() == null) {
            showInfo(actionName, "Сначала выберите договор.");
            return false;
        }
        if (selectedContract.getStatus() != DepositContractStatus.OPEN) {
            showInfo(actionName, "Операции доступны только для активных вкладов.");
            return false;
        }
        return true;
    }

    private void reloadContractsAndSelect(Long id) {
        contracts.setAll(depositContractService.getAllContracts());
        selectedContract = null;

        if (id != null) {
            for (DepositContract c : contracts) {
                if (id.equals(c.getId())) {
                    contractsTable.getSelectionModel().select(c);
                    contractsTable.scrollTo(c);
                    selectedContract = c;
                    break;
                }
            }
        }

        if (selectedContract == null && !contracts.isEmpty()) {
            contractsTable.getSelectionModel().selectFirst();
            selectedContract = contractsTable.getSelectionModel().getSelectedItem();
        }

        updateButtonsState();
    }

    private void updateButtonsState() {
        User current = sessionContext.getCurrentUser();

        boolean canManageContracts = uiAccessManager.canManageContracts(current);
        boolean canOperate = uiAccessManager.canOperateDeposits(current);

        boolean hasSelection = selectedContract != null;

        openContractButton.setDisable(!canManageContracts);

        depositButton.setDisable(!canOperate || !hasSelection);
        withdrawButton.setDisable(!canOperate || !hasSelection);
        closeContractButton.setDisable(!canManageContracts || !hasSelection);

        if (hasSelection && selectedContract.getStatus() != DepositContractStatus.OPEN) {
            depositButton.setDisable(true);
            withdrawButton.setDisable(true);
            closeContractButton.setDisable(true);
        }
    }

    private BigDecimal parseMoneyUI(String text, String fieldName) {
        BigDecimal amount = MoneyUtil.parsePositiveMoney(text);
        if (amount == null) {
            showError(fieldName, "Неверный формат суммы или сумма должна быть больше нуля. Пример: 100000.50");
            return null;
        }
        return amount;
    }

    private String formatCustomerShort(Customer c) {
        if (c == null) {
            return "";
        }
        String middle = c.getMiddleName() != null && !c.getMiddleName().isBlank()
                ? " " + c.getMiddleName().trim()
                : "";
        return (safe(c.getLastName()) + " " + safe(c.getFirstName()) + middle).trim();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // ---------------------- Alerts ----------------------

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
