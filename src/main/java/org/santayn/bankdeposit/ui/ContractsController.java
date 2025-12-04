package org.santayn.bankdeposit.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.DepositOperationType;
import org.santayn.bankdeposit.models.DepositProduct;
import org.santayn.bankdeposit.service.CustomerService;
import org.santayn.bankdeposit.service.DepositContractService;
import org.santayn.bankdeposit.service.DepositProductService;
import org.santayn.bankdeposit.service.EntityNotFoundException;
import org.santayn.bankdeposit.service.InvalidOperationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Контроллер вкладки "Вклады".
 * Позволяет:
 * - просматривать список депозитных договоров;
 * - открывать новый вклад;
 * - пополнять вклад;
 * - снимать средства;
 * - начислять проценты;
 * - закрывать вклад;
 * - просматривать операции по выбранному договору.
 */
@Component
@RequiredArgsConstructor
public class ContractsController {

    private final DepositContractService depositContractService;
    private final CustomerService customerService;
    private final DepositProductService depositProductService;

    // Таблица договоров
    @FXML
    private TableView<DepositContract> contractsTable;

    @FXML
    private TableColumn<DepositContract, Long> colId;

    @FXML
    private TableColumn<DepositContract, String> colContractNumber;

    @FXML
    private TableColumn<DepositContract, String> colCustomer;

    @FXML
    private TableColumn<DepositContract, String> colProduct;

    @FXML
    private TableColumn<DepositContract, String> colOpenDate;

    @FXML
    private TableColumn<DepositContract, String> colCloseDate;

    @FXML
    private TableColumn<DepositContract, String> colBalance;

    @FXML
    private TableColumn<DepositContract, String> colRate;

    @FXML
    private TableColumn<DepositContract, String> colStatus;

    // Таблица операций
    @FXML
    private TableView<DepositOperation> operationsTable;

    @FXML
    private TableColumn<DepositOperation, String> colOpDateTime;

    @FXML
    private TableColumn<DepositOperation, String> colOpType;

    @FXML
    private TableColumn<DepositOperation, String> colOpAmount;

    @FXML
    private TableColumn<DepositOperation, String> colOpDescription;

    // Форма открытия нового вклада
    @FXML
    private ComboBox<Customer> openCustomerCombo;

    @FXML
    private ComboBox<DepositProduct> openProductCombo;

    @FXML
    private TextField initialAmountField;

    @FXML
    private DatePicker openDatePicker;

    // Кнопки
    @FXML
    private Button refreshButton;

    @FXML
    private Button openContractButton;

    @FXML
    private Button depositButton;

    @FXML
    private Button withdrawButton;

    @FXML
    private Button accrueInterestButton;

    @FXML
    private Button closeContractButton;

    private final ObservableList<DepositContract> contractsData = FXCollections.observableArrayList();
    private final ObservableList<DepositOperation> operationsData = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    public void initialize() {
        initializeContractsTable();
        initializeOperationsTable();
        initializeCombos();

        contractsTable.setItems(contractsData);
        operationsTable.setItems(operationsData);

        contractsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        loadOperationsForContract(newSelection);
                    } else {
                        operationsData.clear();
                    }
                }
        );

        // Дата открытия по умолчанию — сегодня
        openDatePicker.setValue(LocalDate.now());

        loadReferenceData();
        loadContracts();
    }

    private void initializeContractsTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colContractNumber.setCellValueFactory(new PropertyValueFactory<>("contractNumber"));

        colCustomer.setCellValueFactory(cellData -> {
            DepositContract contract = cellData.getValue();
            Customer customer = contract.getCustomer();
            String value = "";
            if (customer != null) {
                value = customer.getLastName() + " " + customer.getFirstName();
            }
            return new SimpleStringProperty(value);
        });

        colProduct.setCellValueFactory(cellData -> {
            DepositContract contract = cellData.getValue();
            DepositProduct product = contract.getProduct();
            String value = product != null ? product.getName() : "";
            return new SimpleStringProperty(value);
        });

        colOpenDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getOpenDate() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(cellData.getValue().getOpenDate().format(dateFormatter));
        });

        colCloseDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCloseDate() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(cellData.getValue().getCloseDate().format(dateFormatter));
        });

        colBalance.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCurrentBalance() == null) {
                return new SimpleStringProperty("0.00");
            }
            return new SimpleStringProperty(cellData.getValue().getCurrentBalance().toPlainString());
        });

        colRate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getInterestRate() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(cellData.getValue().getInterestRate().toPlainString());
        });

        colStatus.setCellValueFactory(cellData -> {
            if (cellData.getValue().getStatus() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(cellData.getValue().getStatus().name());
        });
    }

    private void initializeOperationsTable() {
        colOpDateTime.setCellValueFactory(cellData -> {
            if (cellData.getValue().getOperationDateTime() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(cellData.getValue().getOperationDateTime().format(dateTimeFormatter));
        });

        colOpType.setCellValueFactory(cellData -> {
            if (cellData.getValue().getType() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(cellData.getValue().getType().name());
        });

        colOpAmount.setCellValueFactory(cellData -> {
            if (cellData.getValue().getAmount() == null) {
                return new SimpleStringProperty("0.00");
            }
            return new SimpleStringProperty(cellData.getValue().getAmount().toPlainString());
        });

        colOpDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    private void initializeCombos() {
        // Как отображать клиентов в ComboBox
        openCustomerCombo.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getLastName() + " " + item.getFirstName());
                }
            }
        });
        openCustomerCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getLastName() + " " + item.getFirstName());
                }
            }
        });

        // Как отображать продукты
        openProductCombo.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(DepositProduct item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        openProductCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(DepositProduct item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
    }

    private void loadReferenceData() {
        try {
            List<Customer> customers = customerService.getAllCustomers();
            openCustomerCombo.getItems().setAll(customers);

            List<DepositProduct> products = depositProductService.getAllProducts();
            openProductCombo.getItems().setAll(products);
        } catch (Exception e) {
            showError("Ошибка загрузки справочников", e.getMessage());
        }
    }

    private void loadContracts() {
        try {
            List<DepositContract> contracts = depositContractService.getAllContracts();
            contractsData.setAll(contracts);
            operationsData.clear();
        } catch (Exception e) {
            showError("Ошибка загрузки вкладов", e.getMessage());
        }
    }

    private void loadOperationsForContract(DepositContract contract) {
        try {
            List<DepositOperation> operations =
                    depositContractService.getOperationsForContract(contract.getId());
            operationsData.setAll(operations);
        } catch (Exception e) {
            showError("Ошибка загрузки операций", e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        loadReferenceData();
        loadContracts();
    }

    @FXML
    private void onOpenContract() {
        Customer customer = openCustomerCombo.getValue();
        DepositProduct product = openProductCombo.getValue();
        String amountText = initialAmountField.getText();
        LocalDate openDate = openDatePicker.getValue();

        if (customer == null) {
            showError("Не выбран клиент", "Выберите клиента для открытия вклада.");
            return;
        }
        if (product == null) {
            showError("Не выбран продукт", "Выберите депозитный продукт.");
            return;
        }
        if (amountText == null || amountText.isBlank()) {
            showError("Не указана сумма", "Введите сумму первоначального взноса.");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText.replace(",", "."));
        } catch (NumberFormatException e) {
            showError("Неверный формат суммы", "Используйте числовой формат, при необходимости с точкой (например, 10000.50).");
            return;
        }

        try {
            DepositContract contract =
                    depositContractService.openContract(customer.getId(), product.getId(), amount, openDate);
            contractsData.add(contract);
            contractsTable.getSelectionModel().select(contract);
            showInfo("Вклад открыт", "Договор № " + contract.getContractNumber() + " успешно создан.");
        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка при открытии вклада", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка при открытии вклада", e.toString());
        }
    }

    @FXML
    private void onDeposit() {
        DepositContract contract = getSelectedContractOrShowError();
        if (contract == null) {
            return;
        }

        BigDecimal amount = askAmount("Пополнение вклада", "Введите сумму пополнения:");
        if (amount == null) {
            return;
        }

        try {
            DepositContract updated = depositContractService.deposit(contract.getId(), amount);
            replaceContractInList(contract, updated);
            contractsTable.getSelectionModel().select(updated);
            showInfo("Пополнение выполнено", "Счёт успешно пополнен.");
        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка при пополнении", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка при пополнении", e.toString());
        }
    }

    @FXML
    private void onWithdraw() {
        DepositContract contract = getSelectedContractOrShowError();
        if (contract == null) {
            return;
        }

        BigDecimal amount = askAmount("Снятие средств", "Введите сумму для снятия:");
        if (amount == null) {
            return;
        }

        try {
            DepositContract updated = depositContractService.withdraw(contract.getId(), amount);
            replaceContractInList(contract, updated);
            contractsTable.getSelectionModel().select(updated);
            showInfo("Снятие выполнено", "Сумма успешно списана со счёта.");
        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка при снятии средств", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка при снятии средств", e.toString());
        }
    }

    @FXML
    private void onAccrueInterest() {
        DepositContract contract = getSelectedContractOrShowError();
        if (contract == null) {
            return;
        }

        BigDecimal amount = askAmount("Начисление процентов",
                "Введите сумму начисленных процентов (упрощённо):");
        if (amount == null) {
            return;
        }

        try {
            DepositContract updated = depositContractService.accrueInterest(contract.getId(), amount);
            replaceContractInList(contract, updated);
            contractsTable.getSelectionModel().select(updated);
            showInfo("Проценты начислены", "Проценты успешно начислены на вклад.");
        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка при начислении процентов", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка при начислении процентов", e.toString());
        }
    }

    @FXML
    private void onCloseContract() {
        DepositContract contract = getSelectedContractOrShowError();
        if (contract == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение закрытия вклада");
        confirm.setHeaderText(null);
        confirm.setContentText("Вы уверены, что хотите закрыть вклад № " + contract.getContractNumber() + "?");
        Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
            return;
        }

        try {
            DepositContract updated = depositContractService.closeContract(contract.getId());
            replaceContractInList(contract, updated);
            contractsTable.getSelectionModel().select(updated);
            showInfo("Вклад закрыт", "Вклад успешно закрыт, средства выплачены клиенту.");
        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка при закрытии вклада", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка при закрытии вклада", e.toString());
        }
    }

    private DepositContract getSelectedContractOrShowError() {
        DepositContract contract = contractsTable.getSelectionModel().getSelectedItem();
        if (contract == null) {
            showError("Не выбран вклад", "Выберите вклад в таблице.");
        }
        return contract;
    }

    private void replaceContractInList(DepositContract oldContract, DepositContract newContract) {
        int index = contractsData.indexOf(oldContract);
        if (index >= 0) {
            contractsData.set(index, newContract);
        } else {
            loadContracts();
        }
        loadOperationsForContract(newContract);
    }

    private BigDecimal askAmount(String title, String header) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Сумма:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return null;
        }

        String text = result.get().trim();
        if (text.isEmpty()) {
            showError("Не указана сумма", "Сумма не может быть пустой.");
            return null;
        }

        try {
            return new BigDecimal(text.replace(",", "."));
        } catch (NumberFormatException e) {
            showError("Неверный формат суммы", "Используйте числовой формат, при необходимости с точкой (например, 10000.50).");
            return null;
        }
    }

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
