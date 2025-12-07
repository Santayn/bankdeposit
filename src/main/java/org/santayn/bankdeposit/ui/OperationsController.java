package org.santayn.bankdeposit.ui;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.service.*;
import org.springframework.stereotype.Component;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Контроллер вкладки "Операции".
 *
 * UI-доступ:
 * - проводить операции могут: ADMIN, OPERATOR
 * - остальные роли: просмотр операций
 */
@Component
@RequiredArgsConstructor
public class OperationsController {

    private final DepositContractService depositContractService;
    private final DepositOperationService depositOperationService;

    private final SessionContext sessionContext;
    private final org.santayn.bankdeposit.service.UiAccessManager uiAccessManager;

    @FXML
    private ComboBox<DepositContract> contractComboBox;

    @FXML
    private TableView<DepositOperation> operationsTable;

    @FXML
    private TableColumn<DepositOperation, Long> idColumn;

    @FXML
    private TableColumn<DepositOperation, LocalDateTime> dateTimeColumn;

    @FXML
    private TableColumn<DepositOperation, String> typeColumn;

    @FXML
    private TableColumn<DepositOperation, BigDecimal> amountColumn;

    @FXML
    private TableColumn<DepositOperation, String> descriptionColumn;

    @FXML
    private TextField amountField;

    @FXML
    private TextField descriptionField;

    @FXML
    private Button depositButton;

    @FXML
    private Button withdrawButton;

    @FXML
    private Button interestButton;

    @FXML
    private Button refreshButton;

    private final ObservableList<DepositOperation> operations = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupContractComboBox();
        setupOperationsTable();
        loadContracts();

        applyRoleUiAccess();
    }

    private void setupContractComboBox() {
        contractComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(DepositContract item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    Customer c = item.getCustomer();
                    String customerName = "";
                    if (c != null) {
                        String middle = c.getMiddleName() != null && !c.getMiddleName().isBlank()
                                ? " " + c.getMiddleName().trim()
                                : "";
                        customerName = (safe(c.getLastName()) + " " + safe(c.getFirstName()) + middle).trim();
                    }
                    setText("№ " + safe(item.getContractNumber()) + " / " + customerName);
                }
            }
        });

        contractComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DepositContract item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    Customer c = item.getCustomer();
                    String customerName = "";
                    if (c != null) {
                        String middle = c.getMiddleName() != null && !c.getMiddleName().isBlank()
                                ? " " + c.getMiddleName().trim()
                                : "";
                        customerName = (safe(c.getLastName()) + " " + safe(c.getFirstName()) + middle).trim();
                    }
                    setText("№ " + safe(item.getContractNumber()) + " / " + customerName);
                }
            }
        });

        contractComboBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldV, newV) -> loadOperations());
    }

    private void setupOperationsTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        dateTimeColumn.setCellValueFactory(new PropertyValueFactory<>("operationDateTime"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        dateTimeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(formatter));
                }
            }
        });

        typeColumn.setCellValueFactory(cellData ->
                Bindings.createStringBinding(() ->
                        cellData.getValue().getType() != null
                                ? cellData.getValue().getType().name()
                                : "")
        );

        operationsTable.setItems(operations);
    }

    private void loadContracts() {
        List<DepositContract> all = depositContractService.getAllContracts();
        contractComboBox.getItems().setAll(all);
        if (!all.isEmpty()) {
            contractComboBox.getSelectionModel().selectFirst();
        }
        loadOperations();
    }

    private void loadOperations() {
        DepositContract contract = contractComboBox.getValue();
        if (contract == null || contract.getId() == null) {
            operations.clear();
            return;
        }
        operations.setAll(depositOperationService.getOperationsByContract(contract.getId()));
    }

    private void applyRoleUiAccess() {
        User current = sessionContext.getCurrentUser();
        boolean canOperate = uiAccessManager.canOperateDeposits(current);

        depositButton.setDisable(!canOperate);
        withdrawButton.setDisable(!canOperate);
        interestButton.setDisable(!canOperate);

        amountField.setDisable(!canOperate);
        descriptionField.setDisable(!canOperate);
    }

    @FXML
    private void onRefresh() {
        loadContracts();
    }

    @FXML
    private void onDeposit() {
        User current = sessionContext.getCurrentUser();
        if (!uiAccessManager.canOperateDeposits(current)) {
            showInfo("Пополнение", "Недостаточно прав для операций.");
            return;
        }

        DepositContract contract = contractComboBox.getValue();
        if (contract == null || contract.getId() == null) {
            showInfo("Пополнение", "Сначала выберите договор.");
            return;
        }

        BigDecimal amount = parseAmount();
        if (amount == null) {
            return;
        }

        try {
            depositOperationService.deposit(contract.getId(), amount);
            clearInput();
            loadOperations();
        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка пополнения", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка", e.toString());
        }
    }

    @FXML
    private void onWithdraw() {
        User current = sessionContext.getCurrentUser();
        if (!uiAccessManager.canOperateDeposits(current)) {
            showInfo("Снятие", "Недостаточно прав для операций.");
            return;
        }

        DepositContract contract = contractComboBox.getValue();
        if (contract == null || contract.getId() == null) {
            showInfo("Снятие", "Сначала выберите договор.");
            return;
        }

        BigDecimal amount = parseAmount();
        if (amount == null) {
            return;
        }

        try {
            depositOperationService.withdraw(contract.getId(), amount);
            clearInput();
            loadOperations();
        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка снятия", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка", e.toString());
        }
    }

    @FXML
    private void onAccrueInterest() {
        User current = sessionContext.getCurrentUser();
        if (!uiAccessManager.canOperateDeposits(current)) {
            showInfo("Начисление процентов", "Недостаточно прав для операций.");
            return;
        }

        DepositContract contract = contractComboBox.getValue();
        if (contract == null || contract.getId() == null) {
            showInfo("Начисление процентов", "Сначала выберите договор.");
            return;
        }

        BigDecimal amount = parseAmount();
        if (amount == null) {
            return;
        }

        try {
            depositOperationService.accrueInterest(contract.getId(), amount);
            clearInput();
            loadOperations();
        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка начисления процентов", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка", e.toString());
        }
    }

    private BigDecimal parseAmount() {
        String text = amountField.getText();
        BigDecimal amount = MoneyUtil.parsePositiveMoney(text);

        if (amount == null) {
            showError("Неверная сумма", "Введите корректную сумму больше нуля. Пример: 100000.50");
            return null;
        }

        return amount;
    }

    private void clearInput() {
        amountField.clear();
        descriptionField.clear();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
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
