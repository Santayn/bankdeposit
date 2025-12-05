package org.santayn.bankdeposit.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.service.CustomerService;
import org.santayn.bankdeposit.service.EntityNotFoundException;
import org.santayn.bankdeposit.service.InvalidOperationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Контроллер вкладки "Клиенты".
 */
@Component
@RequiredArgsConstructor
public class CustomersController {

    private final CustomerService customerService;

    @FXML
    private TableView<Customer> customersTable;

    @FXML
    private TableColumn<Customer, Long> idColumn;

    @FXML
    private TableColumn<Customer, String> lastNameColumn;

    @FXML
    private TableColumn<Customer, String> firstNameColumn;

    @FXML
    private TableColumn<Customer, String> middleNameColumn;

    @FXML
    private TableColumn<Customer, String> passportColumn;

    @FXML
    private TableColumn<Customer, String> phoneColumn;

    @FXML
    private TableColumn<Customer, String> emailColumn;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button refreshButton;

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Настройка колонок
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        middleNameColumn.setCellValueFactory(new PropertyValueFactory<>("middleName"));
        passportColumn.setCellValueFactory(new PropertyValueFactory<>("passportNumber"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        customersTable.setItems(customers);

        loadCustomers();
    }

    @FXML
    private void onRefresh() {
        loadCustomers();
    }

    @FXML
    private void onAddCustomer() {
        Customer edited = showCustomerDialog(null);
        if (edited != null) {
            try {
                customerService.createCustomer(edited);
                loadCustomers();
            } catch (InvalidOperationException e) {
                showError("Ошибка создания клиента", e.getMessage());
            } catch (Exception e) {
                showError("Неожиданная ошибка", e.toString());
            }
        }
    }

    @FXML
    private void onEditCustomer() {
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Изменение клиента", "Сначала выберите клиента в таблице.");
            return;
        }

        Customer edited = showCustomerDialog(selected);
        if (edited != null) {
            try {
                customerService.updateCustomer(selected.getId(), edited);
                loadCustomers();
            } catch (EntityNotFoundException | InvalidOperationException e) {
                showError("Ошибка изменения клиента", e.getMessage());
            } catch (Exception e) {
                showError("Неожиданная ошибка", e.toString());
            }
        }
    }

    @FXML
    private void onDeleteCustomer() {
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Удаление клиента", "Сначала выберите клиента в таблице.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление клиента");
        confirm.setHeaderText(null);
        confirm.setContentText("Удалить клиента \"" + selected.getLastName() + " " + selected.getFirstName() + "\"?");
        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    customerService.deleteCustomer(selected.getId());
                    loadCustomers();
                } catch (EntityNotFoundException e) {
                    showError("Ошибка удаления клиента", e.getMessage());
                } catch (Exception e) {
                    showError("Неожиданная ошибка", e.toString());
                }
            }
        });
    }

    private void loadCustomers() {
        customers.setAll(customerService.getAllCustomers());
    }

    /**
     * Простейшее модальное окно редактирования клиента.
     * Для учебного проекта используем стандартный Dialog без отдельного FXML.
     */
    private Customer showCustomerDialog(Customer existing) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Добавление клиента" : "Редактирование клиента");
        dialog.setHeaderText(null);

        ButtonType okButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        // Поля формы
        TextField lastNameField = new TextField();
        TextField firstNameField = new TextField();
        TextField middleNameField = new TextField();
        TextField passportField = new TextField();
        TextField phoneField = new TextField();
        TextField emailField = new TextField();
        TextField birthDateField = new TextField(); // формат ГГГГ-ММ-ДД
        TextField addressField = new TextField();

        if (existing != null) {
            lastNameField.setText(existing.getLastName());
            firstNameField.setText(existing.getFirstName());
            middleNameField.setText(existing.getMiddleName());
            passportField.setText(existing.getPassportNumber());
            phoneField.setText(existing.getPhone());
            emailField.setText(existing.getEmail());
            if (existing.getDateOfBirth() != null) {
                birthDateField.setText(existing.getDateOfBirth().toString());
            }
            addressField.setText(existing.getAddress());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        int row = 0;
        grid.add(new Label("Фамилия:"), 0, row);
        grid.add(lastNameField, 1, row++);
        grid.add(new Label("Имя:"), 0, row);
        grid.add(firstNameField, 1, row++);
        grid.add(new Label("Отчество:"), 0, row);
        grid.add(middleNameField, 1, row++);
        grid.add(new Label("Паспорт:"), 0, row);
        grid.add(passportField, 1, row++);
        grid.add(new Label("Телефон:"), 0, row);
        grid.add(phoneField, 1, row++);
        grid.add(new Label("E-mail:"), 0, row);
        grid.add(emailField, 1, row++);
        grid.add(new Label("Дата рождения (ГГГГ-ММ-ДД):"), 0, row);
        grid.add(birthDateField, 1, row++);
        grid.add(new Label("Адрес:"), 0, row);
        grid.add(addressField, 1, row);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                Customer c = existing != null ? new Customer() : new Customer();
                if (existing != null && existing.getId() != null) {
                    c.setId(existing.getId());
                }
                c.setLastName(lastNameField.getText());
                c.setFirstName(firstNameField.getText());
                c.setMiddleName(middleNameField.getText());
                c.setPassportNumber(passportField.getText());
                c.setPhone(phoneField.getText());
                c.setEmail(emailField.getText());
                c.setAddress(addressField.getText());
                if (!birthDateField.getText().isBlank()) {
                    try {
                        c.setDateOfBirth(LocalDate.parse(birthDateField.getText().trim()));
                    } catch (Exception e) {
                        // если дата некорректна, просто игнорируем (можно сделать валидацию)
                    }
                }
                return c;
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
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
