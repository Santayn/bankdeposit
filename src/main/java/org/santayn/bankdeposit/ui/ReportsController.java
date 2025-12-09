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
import java.util.ArrayList;
import java.util.List;

/**
 * Контроллер вкладки "Отчёты".
 *
 * Функции:
 * 1) Договоры выбранного клиента
 * 2) Активные вклады клиента
 * 3) Операции за период (+ фильтр по типу + фильтр по клиенту)
 *
 * Дополнительно:
 * - Возможность выбора "Все клиенты" для отчёта по операциям.
 *
 * Работает с ReportsView.fxml.
 */
@Component
@RequiredArgsConstructor
public class ReportsController {

    private final ReportService reportService;

    // ---------------------- Верхняя панель ----------------------

    @FXML
    private Button refreshButton;

    // ---------------------- Параметры отчёта ----------------------

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

    /**
     * Если в FXML есть fx:id="rateColumn",
     * тогда столбец "% годовых" тоже заполнится.
     */
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
    private TableColumn<DepositOperation, String> opContractColumn;

    @FXML
    private TableColumn<DepositOperation, String> opDescriptionColumn;

    // ---------------------- Данные ----------------------

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<DepositContract> contracts = FXCollections.observableArrayList();
    private final ObservableList<DepositOperation> operations = FXCollections.observableArrayList();

    // ---------------------- Init ----------------------

    @FXML
    public void initialize() {
        setupCustomerCombo();
        setupOperationTypeCombo();
        setupContractsTable();
        setupOperationsTable();
        bindItems();

        loadDictionaries();
        setupButtonsAvailability();

        LocalDate now = LocalDate.now();
        if (fromDatePicker != null) {
            fromDatePicker.setValue(now.minusMonths(1));
        }
        if (toDatePicker != null) {
            toDatePicker.setValue(now);
        }
    }

    private void bindItems() {
        if (contractsTable != null) {
            contractsTable.setItems(contracts);
        }
        if (operationsTable != null) {
            operationsTable.setItems(operations);
        }
        if (customerComboBox != null) {
            customerComboBox.setItems(customers);
        }
    }

    // ---------------------- Combo setup ----------------------

    private void setupCustomerCombo() {
        if (customerComboBox == null) {
            return;
        }

        StringConverter<Customer> converter = new StringConverter<>() {
            @Override
            public String toString(Customer c) {
                if (c == null) {
                    return "Все клиенты";
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

        customerComboBox.setConverter(converter);

        customerComboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(converter.toString(item));
                }
            }
        });

        customerComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(converter.toString(item));
                }
            }
        });
    }

    private void setupOperationTypeCombo() {
        if (operationTypeComboBox == null) {
            return;
        }

        List<DepositOperationType> list = new ArrayList<>();
        list.add(null); // "Все типы"
        for (DepositOperationType t : DepositOperationType.values()) {
            list.add(t);
        }

        operationTypeComboBox.setItems(FXCollections.observableArrayList(list));

        StringConverter<DepositOperationType> converter = new StringConverter<>() {
            @Override
            public String toString(DepositOperationType type) {
                if (type == null) {
                    return "Все";
                }
                return type.name();
            }

            @Override
            public DepositOperationType fromString(String string) {
                return null;
            }
        };

        operationTypeComboBox.setConverter(converter);

        operationTypeComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DepositOperationType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                setText(converter.toString(item));
            }
        });

        operationTypeComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DepositOperationType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                setText(converter.toString(item));
            }
        });

        operationTypeComboBox.getSelectionModel().selectFirst(); // "Все"
    }

    private void setupButtonsAvailability() {
        if (customerComboBox == null) {
            return;
        }

        customerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean конкретныйКлиент = newVal != null && newVal.getId() != null;

            if (buildContractsButton != null) {
                buildContractsButton.setDisable(!конкретныйКлиент);
            }
            if (buildActiveContractsButton != null) {
                buildActiveContractsButton.setDisable(!конкретныйКлиент);
            }
        });

        Customer current = customerComboBox.getValue();
        boolean конкретныйКлиент = current != null && current.getId() != null;

        if (buildContractsButton != null) {
            buildContractsButton.setDisable(!конкретныйКлиент);
        }
        if (buildActiveContractsButton != null) {
            buildActiveContractsButton.setDisable(!конкретныйКлиент);
        }
    }

    // ---------------------- Table setup ----------------------

    private void setupContractsTable() {
        if (contractsTable == null) {
            return;
        }

        if (contractNumberColumn != null) {
            contractNumberColumn.setCellValueFactory(new PropertyValueFactory<>("contractNumber"));
        }

        if (customerColumn != null) {
            customerColumn.setCellValueFactory(cell -> {
                Customer c = cell.getValue().getCustomer();
                return new SimpleStringProperty(formatCustomerShort(c));
            });
        }

        if (productColumn != null) {
            productColumn.setCellValueFactory(cell -> {
                DepositProduct p = cell.getValue().getProduct();
                return new SimpleStringProperty(p != null ? safe(p.getName()) : "");
            });
        }

        if (statusColumn != null) {
            statusColumn.setCellValueFactory(cell -> {
                DepositContractStatus st = cell.getValue().getStatus();
                return new SimpleStringProperty(st != null ? st.name() : "");
            });
        }

        if (openDateColumn != null) {
            openDateColumn.setCellValueFactory(cell -> {
                LocalDate d = cell.getValue().getOpenDate();
                return new SimpleStringProperty(d != null ? d.toString() : "");
            });
        }

        if (closeDateColumn != null) {
            closeDateColumn.setCellValueFactory(cell -> {
                LocalDate d = cell.getValue().getCloseDate();
                return new SimpleStringProperty(d != null ? d.toString() : "");
            });
        }

        if (initialAmountColumn != null) {
            initialAmountColumn.setCellValueFactory(cell -> {
                BigDecimal v = cell.getValue().getInitialAmount();
                return new SimpleStringProperty(v != null ? v.toPlainString() : "0.00");
            });
        }

        if (currentBalanceColumn != null) {
            currentBalanceColumn.setCellValueFactory(cell -> {
                BigDecimal v = cell.getValue().getCurrentBalance();
                return new SimpleStringProperty(v != null ? v.toPlainString() : "0.00");
            });
        }

        if (rateColumn != null) {
            rateColumn.setCellValueFactory(cell -> {
                BigDecimal v = cell.getValue().getInterestRate();
                return new SimpleStringProperty(v != null ? v.toPlainString() : "");
            });
        }
    }

    private void setupOperationsTable() {
        if (operationsTable == null) {
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        if (opDateTimeColumn != null) {
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
        }

        if (opTypeColumn != null) {
            opTypeColumn.setCellValueFactory(cell -> {
                DepositOperationType t = cell.getValue().getType();
                return new SimpleStringProperty(t != null ? t.name() : "");
            });
        }

        if (opAmountColumn != null) {
            opAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        }

        if (opContractColumn != null) {
            opContractColumn.setCellValueFactory(cell -> {
                DepositContract c = cell.getValue().getContract();
                String num = c != null ? c.getContractNumber() : "";
                return new SimpleStringProperty(num != null ? num : "");
            });
        }

        if (opDescriptionColumn != null) {
            opDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        }
    }

    // ---------------------- Load dictionaries ----------------------

    @FXML
    private void onRefresh() {
        loadDictionaries();
        showInfo("Отчёты", "Справочники обновлены.");
    }

    private void loadDictionaries() {
        try {
            List<Customer> cl = reportService.getAllCustomers();

            List<Customer> prepared = new ArrayList<>();
            prepared.add(null); // "Все клиенты"
            prepared.addAll(cl);

            customers.setAll(prepared);

            if (customerComboBox != null) {
                if (customerComboBox.getSelectionModel().getSelectedItem() == null) {
                    customerComboBox.getSelectionModel().selectFirst(); // "Все клиенты"
                }
            }

        } catch (Exception e) {
            customers.clear();
            if (customerComboBox != null) {
                customerComboBox.getSelectionModel().clearSelection();
            }
        }

        setupButtonsAvailability();
    }

    // ---------------------- Build reports ----------------------

    @FXML
    private void onBuildCustomerContracts() {
        Customer customer = customerComboBox != null ? customerComboBox.getValue() : null;
        if (customer == null || customer.getId() == null) {
            showError("Договоры клиента", "Выберите конкретного клиента.");
            return;
        }

        try {
            List<DepositContract> list = reportService.getContractsByCustomer(customer.getId());
            contracts.setAll(list);
            operations.clear();

            if (list.isEmpty()) {
                showInfo("Договоры клиента", "У клиента нет договоров.");
            }
        } catch (Exception ex) {
            showError("Договоры клиента", ex.toString());
        }
    }

    @FXML
    private void onBuildActiveCustomerContracts() {
        Customer customer = customerComboBox != null ? customerComboBox.getValue() : null;
        if (customer == null || customer.getId() == null) {
            showError("Активные вклады", "Выберите конкретного клиента.");
            return;
        }

        try {
            List<DepositContract> list = reportService.getActiveContractsByCustomer(customer.getId());
            contracts.setAll(list);
            operations.clear();

            if (list.isEmpty()) {
                showInfo("Активные вклады", "У клиента нет активных вкладов.");
            }
        } catch (Exception ex) {
            showError("Активные вклады", ex.toString());
        }
    }

    @FXML
    private void onBuildOperationsByPeriod() {
        LocalDate from = fromDatePicker != null ? fromDatePicker.getValue() : null;
        LocalDate to = toDatePicker != null ? toDatePicker.getValue() : null;

        if (from == null || to == null) {
            showError("Операции за период", "Укажите обе даты периода.");
            return;
        }

        if (to.isBefore(from)) {
            showError("Операции за период", "Дата 'по' не может быть раньше даты 'с'.");
            return;
        }

        DepositOperationType type = operationTypeComboBox != null
                ? operationTypeComboBox.getValue()
                : null;

        Customer customer = customerComboBox != null ? customerComboBox.getValue() : null;
        Long customerId = customer != null ? customer.getId() : null; // null = все клиенты

        try {
            List<DepositOperation> list = reportService.getOperationsByPeriodAndTypeForCustomer(
                    customerId,
                    from,
                    to,
                    type
            );

            operations.setAll(list);
            contracts.clear();

            if (list.isEmpty()) {
                showInfo("Операции за период", "За выбранный период операций не найдено.");
            }
        } catch (Exception ex) {
            showError("Операции за период", ex.toString());
        }
    }

    @FXML
    private void onClearResults() {
        contracts.clear();
        operations.clear();
    }

    // ---------------------- Helpers ----------------------

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
