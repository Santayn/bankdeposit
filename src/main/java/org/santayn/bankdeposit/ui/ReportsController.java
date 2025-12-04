package org.santayn.bankdeposit.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.DepositOperationType;
import org.santayn.bankdeposit.service.ReportService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Контроллер вкладки "Отчёты".
 * Содержит два блока:
 * - отчёт по вкладам клиента;
 * - отчёт по операциям за период.
 */
@Component
@RequiredArgsConstructor
public class ReportsController {

    private final ReportService reportService;

    // ----- Отчёт по вкладам клиента -----

    @FXML
    private ComboBox<Customer> reportCustomerCombo;

    @FXML
    private TableView<DepositContract> customerContractsTable;

    @FXML
    private TableColumn<DepositContract, String> colCustContrNumber;

    @FXML
    private TableColumn<DepositContract, String> colCustContrProduct;

    @FXML
    private TableColumn<DepositContract, String> colCustContrOpenDate;

    @FXML
    private TableColumn<DepositContract, String> colCustContrCloseDate;

    @FXML
    private TableColumn<DepositContract, String> colCustContrBalance;

    @FXML
    private TableColumn<DepositContract, String> colCustContrStatus;

    // ----- Отчёт по операциям за период -----

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private ComboBox<DepositOperationType> opTypeCombo;

    @FXML
    private TableView<DepositOperation> operationsReportTable;

    @FXML
    private TableColumn<DepositOperation, String> colRepOpDateTime;

    @FXML
    private TableColumn<DepositOperation, String> colRepOpType;

    @FXML
    private TableColumn<DepositOperation, String> colRepOpAmount;

    @FXML
    private TableColumn<DepositOperation, String> colRepOpContract;

    @FXML
    private TableColumn<DepositOperation, String> colRepOpCustomer;

    @FXML
    private TableColumn<DepositOperation, String> colRepOpDescription;

    @FXML
    private TextField totalAmountField;

    private final ObservableList<DepositContract> customerContractsData = FXCollections.observableArrayList();
    private final ObservableList<DepositOperation> operationsData = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    public void initialize() {
        configureCustomerCombo();
        configureCustomerContractsTable();
        configureOperationTypeCombo();
        configureOperationsTable();

        customerContractsTable.setItems(customerContractsData);
        operationsReportTable.setItems(operationsData);

        loadCustomers();

        // По умолчанию период — последние 7 дней
        LocalDate today = LocalDate.now();
        fromDatePicker.setValue(today.minusDays(7));
        toDatePicker.setValue(today);
    }

    private void configureCustomerCombo() {
        reportCustomerCombo.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
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
        reportCustomerCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
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
    }

    private void configureCustomerContractsTable() {
        colCustContrNumber.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getContractNumber()
        ));
        colCustContrProduct.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProduct() != null ? c.getValue().getProduct().getName() : ""
        ));
        colCustContrOpenDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOpenDate() != null ? c.getValue().getOpenDate().format(dateFormatter) : ""
        ));
        colCustContrCloseDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCloseDate() != null ? c.getValue().getCloseDate().format(dateFormatter) : ""
        ));
        colCustContrBalance.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCurrentBalance() != null ? c.getValue().getCurrentBalance().toPlainString() : "0.00"
        ));
        colCustContrStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() != null ? c.getValue().getStatus().name() : ""
        ));
    }

    private void configureOperationTypeCombo() {
        opTypeCombo.getItems().setAll(DepositOperationType.values());
        opTypeCombo.setPromptText("Все типы операций");
    }

    private void configureOperationsTable() {
        colRepOpDateTime.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOperationDateTime() != null
                        ? c.getValue().getOperationDateTime().format(dateTimeFormatter)
                        : ""
        ));
        colRepOpType.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getType() != null ? c.getValue().getType().name() : ""
        ));
        colRepOpAmount.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getAmount() != null ? c.getValue().getAmount().toPlainString() : "0.00"
        ));
        colRepOpContract.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getContract() != null ? c.getValue().getContract().getContractNumber() : ""
        ));
        colRepOpCustomer.setCellValueFactory(c -> {
            if (c.getValue().getContract() == null || c.getValue().getContract().getCustomer() == null) {
                return new SimpleStringProperty("");
            }
            Customer customer = c.getValue().getContract().getCustomer();
            String name = customer.getLastName() + " " + customer.getFirstName();
            return new SimpleStringProperty(name);
        });
        colRepOpDescription.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDescription() != null ? c.getValue().getDescription() : ""
        ));
    }

    private void loadCustomers() {
        try {
            List<Customer> customers = reportService.getAllCustomers();
            reportCustomerCombo.getItems().setAll(customers);
        } catch (Exception e) {
            showError("Ошибка загрузки клиентов", e.getMessage());
        }
    }

    @FXML
    private void onLoadCustomerContracts() {
        Customer customer = reportCustomerCombo.getValue();
        if (customer == null) {
            showError("Не выбран клиент", "Выберите клиента для формирования отчёта.");
            return;
        }

        try {
            List<DepositContract> contracts = reportService.getContractsByCustomer(customer.getId());
            customerContractsData.setAll(contracts);
        } catch (Exception e) {
            showError("Ошибка формирования отчёта по вкладам клиента", e.getMessage());
        }
    }

    @FXML
    private void onLoadOperationsReport() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        if (from == null || to == null) {
            showError("Не указан период", "Укажите дату начала и окончания периода.");
            return;
        }
        if (to.isBefore(from)) {
            showError("Неверный период", "Дата окончания не может быть раньше даты начала.");
            return;
        }

        DepositOperationType typeFilter = opTypeCombo.getValue();

        try {
            List<DepositOperation> operations =
                    reportService.getOperationsByPeriodAndType(from, to, typeFilter);
            operationsData.setAll(operations);

            BigDecimal total = operations.stream()
                    .map(DepositOperation::getAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalAmountField.setText(total.toPlainString());
        } catch (Exception e) {
            showError("Ошибка формирования отчёта по операциям", e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
