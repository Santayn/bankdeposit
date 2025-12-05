package org.santayn.bankdeposit.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.santayn.bankdeposit.models.DepositProduct;
import org.santayn.bankdeposit.service.CustomerService;
import org.santayn.bankdeposit.service.DepositContractService;
import org.santayn.bankdeposit.service.DepositProductService;
import org.santayn.bankdeposit.service.EntityNotFoundException;
import org.santayn.bankdeposit.service.InvalidOperationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Контроллер вкладки "Вклады".
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
    private TableColumn<DepositContract, Long> idColumn;

    @FXML
    private TableColumn<DepositContract, String> contractNumberColumn;

    @FXML
    private TableColumn<DepositContract, String> customerColumn;

    @FXML
    private TableColumn<DepositContract, String> productColumn;

    @FXML
    private TableColumn<DepositContract, String> openDateColumn;

    @FXML
    private TableColumn<DepositContract, String> statusColumn;

    @FXML
    private TableColumn<DepositContract, String> balanceColumn;

    // Блок "Открытие вклада"
    @FXML
    private ComboBox<Customer> customerComboBox;

    @FXML
    private ComboBox<DepositProduct> productComboBox;

    @FXML
    private DatePicker openDatePicker;

    @FXML
    private TextField initialAmountField;

    @FXML
    private Button openContractButton;

    // Блок "Операции"
    @FXML
    private TextField operationAmountField;

    @FXML
    private TextField operationDescriptionField;

    @FXML
    private Button depositButton;

    @FXML
    private Button withdrawButton;

    @FXML
    private Button closeContractButton;

    private final ObservableList<DepositContract> contracts = FXCollections.observableArrayList();
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<DepositProduct> products = FXCollections.observableArrayList();

    /**
     * Текущий выбранный договор.
     */
    private DepositContract selectedContract;

    @FXML
    public void initialize() {
        setupTable();
        setupComboBoxes();
        loadData();
        setupSelectionListener();
        clearOperationFields();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        contractNumberColumn.setCellValueFactory(new PropertyValueFactory<>("contractNumber"));

        customerColumn.setCellValueFactory(cellData -> {
            DepositContract c = cellData.getValue();
            Customer cust = c != null ? c.getCustomer() : null;
            if (cust == null) {
                return new SimpleStringProperty("");
            }
            String text = (cust.getLastName() + " " + cust.getFirstName()).trim();
            return new SimpleStringProperty(text);
        });

        productColumn.setCellValueFactory(cellData -> {
            DepositContract c = cellData.getValue();
            DepositProduct p = c != null ? c.getProduct() : null;
            return new SimpleStringProperty(p != null ? p.getName() : "");
        });

        openDateColumn.setCellValueFactory(cellData -> {
            DepositContract c = cellData.getValue();
            LocalDate od = c != null ? c.getOpenDate() : null;
            return new SimpleStringProperty(od != null ? od.toString() : "");
        });

        statusColumn.setCellValueFactory(cellData -> {
            DepositContract c = cellData.getValue();
            DepositContractStatus st = c != null ? c.getStatus() : null;
            return new SimpleStringProperty(st != null ? st.name() : "");
        });

        balanceColumn.setCellValueFactory(cellData -> {
            DepositContract c = cellData.getValue();
            BigDecimal bal = c != null ? c.getCurrentBalance() : null;
            return new SimpleStringProperty(bal != null ? bal.toPlainString() : "0.00");
        });

        contractsTable.setItems(contracts);
    }

    private void setupComboBoxes() {
        // Красивое отображение клиентов
        StringConverter<Customer> customerConverter = new StringConverter<>() {
            @Override
            public String toString(Customer c) {
                if (c == null) {
                    return "";
                }
                return (c.getLastName() + " " + c.getFirstName()).trim();
            }

            @Override
            public Customer fromString(String string) {
                return null;
            }
        };

        customerComboBox.setConverter(customerConverter);
        customerComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                } else {
                    setText(customerConverter.toString(c));
                }
            }
        });

        // Отображение продуктов
        StringConverter<DepositProduct> productConverter = new StringConverter<>() {
            @Override
            public String toString(DepositProduct product) {
                if (product == null) {
                    return "";
                }
                return product.getName();
            }

            @Override
            public DepositProduct fromString(String string) {
                return null;
            }
        };

        productComboBox.setConverter(productConverter);
        productComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(DepositProduct p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    setText(productConverter.toString(p));
                }
            }
        });

        customerComboBox.setItems(customers);
        productComboBox.setItems(products);
    }

    private void loadData() {
        List<Customer> customerList = customerService.getAllCustomers();
        customers.setAll(customerList);

        List<DepositProduct> productList = depositProductService.getAllProducts();
        products.setAll(productList);

        List<DepositContract> contractList = depositContractService.getAllContracts();
        contracts.setAll(contractList);

        if (!contracts.isEmpty()) {
            contractsTable.getSelectionModel().selectFirst();
        }
    }

    private void setupSelectionListener() {
        contractsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> {
                    selectedContract = newSel;
                    updateButtonsState();
                }
        );
        updateButtonsState();
    }

    private void updateButtonsState() {
        boolean hasSelection = selectedContract != null;
        depositButton.setDisable(!hasSelection);
        withdrawButton.setDisable(!hasSelection);
        closeContractButton.setDisable(!hasSelection);

        if (hasSelection && selectedContract.getStatus() != DepositContractStatus.OPEN) {
            depositButton.setDisable(true);
            withdrawButton.setDisable(true);
            closeContractButton.setDisable(true);
        }
    }

    private void clearOperationFields() {
        operationAmountField.clear();
        operationDescriptionField.clear();
    }

    // ----------------------- Обработчики кнопок -----------------------

    /**
     * Открытие нового вклада.
     */
    @FXML
    private void onOpenContract() {
        try {
            Customer customer = customerComboBox.getValue();
            DepositProduct product = productComboBox.getValue();
            LocalDate openDate = openDatePicker.getValue();
            String amountText = initialAmountField.getText();

            if (customer == null) {
                showError("Открытие вклада", "Выберите клиента");
                return;
            }
            if (product == null) {
                showError("Открытие вклада", "Выберите депозитный продукт");
                return;
            }
            if (amountText == null || amountText.isBlank()) {
                showError("Открытие вклада", "Введите сумму вклада");
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountText.trim().replace(',', '.'));
            } catch (NumberFormatException ex) {
                showError("Открытие вклада", "Неверный формат суммы");
                return;
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Открытие вклада", "Сумма должна быть больше нуля");
                return;
            }

            if (openDate == null) {
                openDate = LocalDate.now();
            }

            // Сервисная сигнатура: (customerId, productId, initialAmount, openDate)
            DepositContract created = depositContractService.openContract(
                    customer.getId(),
                    product.getId(),
                    amount,
                    openDate
            );

            reloadAndSelect(created.getId());
            clearOperationFields();
            initialAmountField.clear();

            showInfo("Открытие вклада",
                    "Вклад успешно открыт. Номер договора: " + created.getContractNumber());

        } catch (InvalidOperationException | EntityNotFoundException ex) {
            showError("Открытие вклада", ex.getMessage());
        } catch (Exception ex) {
            showError("Неожиданная ошибка", ex.toString());
        }
    }

    /**
     * Пополнение вклада.
     */
    @FXML
    private void onDeposit() {
        if (selectedContract == null) {
            showError("Пополнение вклада", "Сначала выберите договор в таблице");
            return;
        }

        try {
            BigDecimal amount = parseOperationAmount();
            if (amount == null) {
                return;
            }
            String description = operationDescriptionField.getText();

            DepositContract updated = depositContractService.deposit(
                    selectedContract.getId(),
                    amount,
                    description
            );

            reloadAndSelect(updated.getId());
            clearOperationFields();

        } catch (InvalidOperationException | EntityNotFoundException ex) {
            showError("Пополнение вклада", ex.getMessage());
        } catch (Exception ex) {
            showError("Неожиданная ошибка", ex.toString());
        }
    }

    /**
     * Снятие средств.
     */
    @FXML
    private void onWithdraw() {
        if (selectedContract == null) {
            showError("Снятие средств", "Сначала выберите договор в таблице");
            return;
        }

        try {
            BigDecimal amount = parseOperationAmount();
            if (amount == null) {
                return;
            }
            String description = operationDescriptionField.getText();

            DepositContract updated = depositContractService.withdraw(
                    selectedContract.getId(),
                    amount,
                    description
            );

            reloadAndSelect(updated.getId());
            clearOperationFields();

        } catch (InvalidOperationException | EntityNotFoundException ex) {
            showError("Снятие средств", ex.getMessage());
        } catch (Exception ex) {
            showError("Неожиданная ошибка", ex.toString());
        }
    }

    /**
     * Закрытие вклада.
     */
    @FXML
    private void onCloseContract() {
        if (selectedContract == null) {
            showError("Закрытие вклада", "Сначала выберите договор в таблице");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Закрытие вклада");
        confirm.setHeaderText(null);
        confirm.setContentText("Закрыть выбранный вклад?");

        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.YES) {
                try {
                    // Сервисная сигнатура: (contractId, closeDate)
                    DepositContract updated =
                            depositContractService.closeContract(selectedContract.getId(), LocalDate.now());
                    reloadAndSelect(updated.getId());
                    clearOperationFields();
                } catch (InvalidOperationException | EntityNotFoundException ex) {
                    showError("Закрытие вклада", ex.getMessage());
                } catch (Exception ex) {
                    showError("Неожиданная ошибка", ex.toString());
                }
            }
        });
    }

    // ----------------------- Вспомогательные методы -----------------------

    private BigDecimal parseOperationAmount() {
        String text = operationAmountField.getText();
        if (text == null || text.isBlank()) {
            showError("Сумма операции", "Введите сумму операции");
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(text.trim().replace(',', '.'));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Сумма операции", "Сумма должна быть больше нуля");
                return null;
            }
            return amount;
        } catch (NumberFormatException ex) {
            showError("Сумма операции", "Неверный формат суммы");
            return null;
        }
    }

    private void reloadAndSelect(Long id) {
        List<DepositContract> list = depositContractService.getAllContracts();
        contracts.setAll(list);

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

        updateButtonsState();
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
