package org.santayn.bankdeposit.ui;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.service.DepositContractService;
import org.santayn.bankdeposit.service.DepositOperationService;
import org.santayn.bankdeposit.service.EntityNotFoundException;
import org.santayn.bankdeposit.service.InvalidOperationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Контроллер вкладки "Операции".
 *
 * Текущая концепция слоя:
 * - DepositContractService: бизнес-операции по договору (deposit/withdraw и т.п.).
 * - DepositOperationService: чтение истории операций.
 *
 * Начисление процентов пока не реализовано в сервисе,
 * поэтому в UI оставлена безопасная заглушка.
 */
@Component
@RequiredArgsConstructor
public class OperationsController {

    private final DepositContractService depositContractService;
    private final DepositOperationService depositOperationService;

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
    }

    private void setupContractComboBox() {
        contractComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(DepositContract item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    return;
                }

                Customer c = item.getCustomer();
                String customerName = "";
                if (c != null) {
                    String middle = c.getMiddleName() != null && !c.getMiddleName().isBlank()
                            ? " " + c.getMiddleName().trim()
                            : "";
                    customerName = (safe(c.getLastName()) + " " + safe(c.getFirstName()) + middle).trim();
                }

                String status = item.getStatus() != null ? item.getStatus().name() : "";
                setText("№ " + safe(item.getContractNumber()) + " / " + customerName + " / " + status);
            }
        });

        contractComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DepositContract item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    return;
                }

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
        });

        contractComboBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldV, newV) -> {
                    loadOperations();
                    updateButtonsState(newV);
                });
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
                    return;
                }
                setText(item.format(formatter));
            }
        });

        // Берём enum через getType()
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

        DepositContract selected = contractComboBox.getValue();
        updateButtonsState(selected);
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

    @FXML
    private void onRefresh() {
        loadContracts();
    }

    @FXML
    private void onDeposit() {
        DepositContract contract = contractComboBox.getValue();
        if (contract == null || contract.getId() == null) {
            showInfo("Пополнение", "Сначала выберите договор.");
            return;
        }
        if (contract.getStatus() != DepositContractStatus.OPEN) {
            showInfo("Пополнение", "Пополнение доступно только для активных вкладов.");
            return;
        }

        BigDecimal amount = parseAmount();
        if (amount == null) {
            return;
        }

        String description = descriptionField.getText();

        try {
            // Ожидаем, что сигнатура у сервиса:
            // deposit(Long contractId, BigDecimal amount, String description)
            depositContractService.deposit(contract.getId(), amount, description);

            clearInput();
            reloadContractInCombo(contract.getId());
            loadOperations();

        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка пополнения", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка", e.toString());
        }
    }

    @FXML
    private void onWithdraw() {
        DepositContract contract = contractComboBox.getValue();
        if (contract == null || contract.getId() == null) {
            showInfo("Снятие", "Сначала выберите договор.");
            return;
        }
        if (contract.getStatus() != DepositContractStatus.OPEN) {
            showInfo("Снятие", "Снятие доступно только для активных вкладов.");
            return;
        }

        BigDecimal amount = parseAmount();
        if (amount == null) {
            return;
        }

        String description = descriptionField.getText();

        try {
            // Ожидаем, что сигнатура у сервиса:
            // withdraw(Long contractId, BigDecimal amount, String description)
            depositContractService.withdraw(contract.getId(), amount, description);

            clearInput();
            reloadContractInCombo(contract.getId());
            loadOperations();

        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка снятия", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка", e.toString());
        }
    }

    /**
     * Начисление процентов.
     *
     * В текущей версии проекта метод бизнес-начисления
     * в DepositContractService отсутствует.
     * Поэтому оставляем безопасную заглушку.
     */
    @FXML
    private void onAccrueInterest() {
        DepositContract contract = contractComboBox.getValue();
        if (contract == null || contract.getId() == null) {
            showInfo("Начисление процентов", "Сначала выберите договор.");
            return;
        }
        if (contract.getStatus() != DepositContractStatus.OPEN) {
            showInfo("Начисление процентов", "Начисление доступно только для активных вкладов.");
            return;
        }

        showInfo("Начисление процентов",
                "Функция начисления процентов пока не реализована в сервисном слое.\n" +
                        "Как только добавишь метод в DepositContractService, " +
                        "мы подключим его сюда.");
    }

    private void reloadContractInCombo(Long contractId) {
        List<DepositContract> all = depositContractService.getAllContracts();
        contractComboBox.getItems().setAll(all);

        if (contractId == null) {
            if (!all.isEmpty()) {
                contractComboBox.getSelectionModel().selectFirst();
            }
            updateButtonsState(contractComboBox.getValue());
            return;
        }

        for (DepositContract c : all) {
            if (contractId.equals(c.getId())) {
                contractComboBox.getSelectionModel().select(c);
                break;
            }
        }

        updateButtonsState(contractComboBox.getValue());
    }

    private void updateButtonsState(DepositContract contract) {
        boolean enabled = contract != null && contract.getStatus() == DepositContractStatus.OPEN;

        depositButton.setDisable(!enabled);
        withdrawButton.setDisable(!enabled);

        // Пока начисление не реализовано — можно оставить выключенной всегда,
        // либо включать только для OPEN, но обработчик всё равно покажет заглушку.
        interestButton.setDisable(!enabled);
    }

    private BigDecimal parseAmount() {
        String text = amountField.getText();
        if (text == null || text.isBlank()) {
            showError("Неверная сумма", "Введите сумму операции.");
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(text.trim().replace(',', '.'));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Неверная сумма", "Сумма должна быть больше нуля.");
                return null;
            }
            return amount;
        } catch (Exception e) {
            showError("Неверная сумма", "Не удалось разобрать сумму. Пример: 100000.50");
            return null;
        }
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
