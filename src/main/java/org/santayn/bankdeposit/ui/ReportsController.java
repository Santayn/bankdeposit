package org.santayn.bankdeposit.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.DepositOperationType;
import org.santayn.bankdeposit.models.DepositProduct;
import org.santayn.bankdeposit.service.ReportService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Контроллер вкладки "Отчёты".
 * Позволяет получить:
 * 1) все договоры клиента;
 * 2) активные договоры клиента;
 * 3) операции за период (с фильтром по типу).
 */
@Component
@RequiredArgsConstructor
public class ReportsController {

    private final ReportService reportService;

    // Верхние элементы
    @FXML
    private Button refreshButton;

    @FXML
    private ComboBox<Customer> customerComboBox;

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private ComboBox<DepositOperationType> operationTypeComboBox;

    @FXML
    private Button buildContractsButton;

    @FXML
    private Button buildActiveContractsButton;

    @FXML
    private Button buildOperationsButton;

    @FXML
    private Button clearButton;

    // Таблица договоров
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

    // Таблица операций
    @FXML
    private TableView<DepositOperation> operationsTable;

    @FXML
    private TableColumn<DepositOperation, LocalDateTime> opDateTimeColumn;

    @FXML
    private TableColumn<DepositOperation, String> opTypeColumn;

    @FXML
    private TableColumn<DepositOperation, BigDecimal> opAmountColumn;

    @FXML
    private TableColumn<DepositOperation, String> opContractColumn;

    @FXML
    private TableColumn<DepositOperation, String> opDescriptionColumn;

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<DepositContract> contracts = FXCollections.observableArrayList();
    private final ObservableList<DepositOperation> operations = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupCustomerCombo();
        setupOperationTypeCombo();
        setupContractsTable();
        setupOperationsTable();
        initDefaultDates();
        loadReferenceData();
    }

    private void setupCustomerCombo() {
        StringConverter<Customer> converter = new StringConverter<>() {
            @Override
            public String toString(Customer c) {
                if (c == null) {
                    return "";
                }
                String middle = c.getMiddleName() != null && !c.getMiddleName().isBlank()
                        ? " " + c.getMiddleName().trim()
                        : "";
                return (c.getLastName() + " " + c.getFirstName() + middle).trim();
            }

            @Override
            public Customer fromString(String string) {
                return null;
            }
        };

        customerComboBox.setConverter(converter);
        customerComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : converter.toString(item));
            }
        });

        customerComboBox.setItems(customers);
    }

    private void setupOperationTypeCombo() {
        operationTypeComboBox.getItems().setAll(DepositOperationType.values());
        operationTypeComboBox.getSelectionModel().clearSelection();

        // Можно сделать "пустой" выбор для всех типов:
        operationTypeComboBox.setPromptText("Все типы");
    }

    private void setupContractsTable() {
        contractNumberColumn.setCellValueFactory(new PropertyValueFactory<>("contractNumber"));

        customerColumn.setCellValueFactory(cell -> {
            DepositContract c = cell.getValue();
            Customer cust = c != null ? c.getCustomer() : null;
            if (cust == null) {
                return new SimpleStringProperty("");
            }
            String middle = cust.getMiddleName() != null && !cust.getMiddleName().isBlank()
                    ? " " + cust.getMiddleName().trim()
                    : "";
            return new SimpleStringProperty((cust.getLastName() + " " + cust.getFirstName() + middle).trim());
        });

        productColumn.setCellValueFactory(cell -> {
            DepositProduct p = cell.getValue() != null ? cell.getValue().getProduct() : null;
            return new SimpleStringProperty(p != null ? p.getName() : "");
        });

        statusColumn.setCellValueFactory(cell -> {
            DepositContractStatus st = cell.getValue() != null ? cell.getValue().getStatus() : null;
            return new SimpleStringProperty(st != null ? st.name() : "");
        });

        openDateColumn.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue() != null ? cell.getValue().getOpenDate() : null;
            return new SimpleStringProperty(d != null ? d.toString() : "");
        });

        closeDateColumn.setCellValueFactory(cell -> {
            LocalDate d = cell.getValue() != null ? cell.getValue().getCloseDate() : null;
            return new SimpleStringProperty(d != null ? d.toString() : "");
        });

        initialAmountColumn.setCellValueFactory(cell -> {
            BigDecimal v = cell.getValue() != null ? cell.getValue().getInitialAmount() : null;
            return new SimpleStringProperty(v != null ? v.toPlainString() : "");
        });

        currentBalanceColumn.setCellValueFactory(cell -> {
            BigDecimal v = cell.getValue() != null ? cell.getValue().getCurrentBalance() : null;
            return new SimpleStringProperty(v != null ? v.toPlainString() : "");
        });

        rateColumn.setCellValueFactory(cell -> {
            BigDecimal v = cell.getValue() != null ? cell.getValue().getInterestRate() : null;
            return new SimpleStringProperty(v != null ? v.toPlainString() : "");
        });

        contractsTable.setItems(contracts);
    }

    private void setupOperationsTable() {
        opDateTimeColumn.setCellValueFactory(new PropertyValueFactory<>("operationDateTime"));
        opAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        opDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        opDateTimeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(fmt));
            }
        });

        opTypeColumn.setCellValueFactory(cell -> {
            DepositOperation op = cell.getValue();
            DepositOperationType type = op != null ? op.getType() : null;
            return new SimpleStringProperty(type != null ? type.name() : "");
        });

        opContractColumn.setCellValueFactory(cell -> {
            DepositOperation op = cell.getValue();
            DepositContract c = op != null ? op.getContract() : null;
            return new SimpleStringProperty(c != null ? c.getContractNumber() : "");
        });

        operationsTable.setItems(operations);
    }

    private void initDefaultDates() {
        LocalDate now = LocalDate.now();
        fromDatePicker.setValue(now.minusMonths(1));
        toDatePicker.setValue(now);
    }

    private void loadReferenceData() {
        List<Customer> list = reportService.getAllCustomers();
        customers.setAll(list);

        if (!customers.isEmpty() && customerComboBox.getValue() == null) {
            customerComboBox.getSelectionModel().selectFirst();
        }
    }

    // -------------------- Actions --------------------

    @FXML
    private void onRefresh() {
        loadReferenceData();
        showInfo("Отчёты", "Справочники обновлены.");
    }

    @FXML
    private void onBuildCustomerContracts() {
        Customer customer = customerComboBox.getValue();
        if (customer == null) {
            showError("Договоры клиента", "Выберите клиента.");
            return;
        }

        try {
            List<DepositContract> list = reportService.getContractsByCustomer(customer.getId());
            contracts.setAll(list);
            operations.clear();
        } catch (Exception ex) {
            showError("Договоры клиента", ex.toString());
        }
    }

    @FXML
    private void onBuildActiveCustomerContracts() {
        Customer customer = customerComboBox.getValue();
        if (customer == null) {
            showError("Активные вклады", "Выберите клиента.");
            return;
        }

        try {
            List<DepositContract> list = reportService.getActiveContractsByCustomer(customer.getId());
            contracts.setAll(list);
            operations.clear();
        } catch (Exception ex) {
            showError("Активные вклады", ex.toString());
        }
    }

    @FXML
    private void onBuildOperationsByPeriod() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        if (from == null || to == null) {
            showError("Операции за период", "Укажите период (дата с / дата по).");
            return;
        }
        if (to.isBefore(from)) {
            showError("Операции за период", "Дата 'по' не может быть раньше даты 'с'.");
            return;
        }

        DepositOperationType type = operationTypeComboBox.getValue();

        try {
            List<DepositOperation> list = reportService.getOperationsByPeriodAndType(from, to, type);
            operations.setAll(list);
            contracts.clear();
        } catch (Exception ex) {
            showError("Операции за период", ex.toString());
        }
    }

    @FXML
    private void onClearResults() {
        contracts.clear();
        operations.clear();
    }

    // -------------------- Alerts --------------------

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
