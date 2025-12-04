package org.santayn.bankdeposit.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.service.CustomerService;
import org.santayn.bankdeposit.service.EntityNotFoundException;
import org.santayn.bankdeposit.service.InvalidOperationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * JavaFX-контроллер экрана "Клиенты".
 * Поднимается как Spring-бин, сервисы внедряются через конструктор.
 */
@Component
@RequiredArgsConstructor
public class CustomersController {

    private final CustomerService customerService;

    // Таблица
    @FXML
    private TableView<Customer> customersTable;

    @FXML
    private TableColumn<Customer, Long> colId;

    @FXML
    private TableColumn<Customer, String> colLastName;

    @FXML
    private TableColumn<Customer, String> colFirstName;

    @FXML
    private TableColumn<Customer, String> colPassport;

    @FXML
    private TableColumn<Customer, String> colPhone;

    // Поле поиска
    @FXML
    private TextField searchLastNameField;

    // Поля формы
    @FXML
    private TextField lastNameField;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField middleNameField;

    @FXML
    private DatePicker dateOfBirthPicker;

    @FXML
    private TextField passportField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField addressField;

    // Кнопки
    @FXML
    private Button refreshButton;

    @FXML
    private Button addButton;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    private final ObservableList<Customer> customersData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Настройка колонок таблицы
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colPassport.setCellValueFactory(new PropertyValueFactory<>("passportNumber"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        customersTable.setItems(customersData);

        customersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        fillForm(newSelection);
                    }
                }
        );

        loadAllCustomers();
    }

    private void loadAllCustomers() {
        try {
            List<Customer> customers = customerService.getAllCustomers();
            customersData.setAll(customers);
        } catch (Exception e) {
            showError("Ошибка загрузки клиентов", e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        clearSearch();
        clearForm();
        loadAllCustomers();
    }

    @FXML
    private void onSearch() {
        String query = searchLastNameField.getText();
        try {
            List<Customer> customers = customerService.searchByLastName(query);
            customersData.setAll(customers);
        } catch (Exception e) {
            showError("Ошибка поиска", e.getMessage());
        }
    }

    @FXML
    private void onAdd() {
        try {
            Customer customer = buildCustomerFromForm(null);
            Customer saved = customerService.createCustomer(customer);
            customersData.add(saved);
            customersTable.getSelectionModel().select(saved);
            showInfo("Клиент добавлен", "Клиент успешно сохранён.");
        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка при сохранении клиента", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка при сохранении", e.toString());
        }
    }

    @FXML
    private void onUpdate() {
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Не выбран клиент", "Выберите клиента в таблице для редактирования.");
            return;
        }

        try {
            Customer updated = buildCustomerFromForm(selected.getId());
            Customer saved = customerService.updateCustomer(selected.getId(), updated);

            int index = customersData.indexOf(selected);
            if (index >= 0) {
                customersData.set(index, saved);
            }

            customersTable.getSelectionModel().select(saved);
            showInfo("Клиент обновлён", "Изменения успешно сохранены.");
        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка при обновлении клиента", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка при обновлении", e.toString());
        }
    }

    @FXML
    private void onDelete() {
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Не выбран клиент", "Выберите клиента в таблице для удаления.");
            return;
        }

        try {
            customerService.deleteCustomer(selected.getId());
            customersData.remove(selected);
            clearForm();
            showInfo("Клиент удалён", "Клиент успешно удалён.");
        } catch (EntityNotFoundException e) {
            showError("Ошибка при удалении клиента", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка при удалении", e.toString());
        }
    }

    private Customer buildCustomerFromForm(Long id) {
        String lastName = lastNameField.getText();
        String firstName = firstNameField.getText();
        String middleName = middleNameField.getText();
        LocalDate dateOfBirth = dateOfBirthPicker.getValue();
        String passport = passportField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();
        String address = addressField.getText();

        if (lastName == null || lastName.isBlank()) {
            throw new InvalidOperationException("Фамилия обязательна для заполнения");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new InvalidOperationException("Имя обязательно для заполнения");
        }
        if (passport == null || passport.isBlank()) {
            throw new InvalidOperationException("Паспортные данные обязательны для заполнения");
        }

        return Customer.builder()
                .id(id)
                .lastName(lastName.trim())
                .firstName(firstName.trim())
                .middleName(middleName == null || middleName.isBlank() ? null : middleName.trim())
                .dateOfBirth(dateOfBirth)
                .passportNumber(passport.trim())
                .phone(phone == null || phone.isBlank() ? null : phone.trim())
                .email(email == null || email.isBlank() ? null : email.trim())
                .address(address == null || address.isBlank() ? null : address.trim())
                .build();
    }

    private void fillForm(Customer customer) {
        lastNameField.setText(customer.getLastName());
        firstNameField.setText(customer.getFirstName());
        middleNameField.setText(customer.getMiddleName());
        dateOfBirthPicker.setValue(customer.getDateOfBirth());
        passportField.setText(customer.getPassportNumber());
        phoneField.setText(customer.getPhone());
        emailField.setText(customer.getEmail());
        addressField.setText(customer.getAddress());
    }

    private void clearForm() {
        lastNameField.clear();
        firstNameField.clear();
        middleNameField.clear();
        dateOfBirthPicker.setValue(null);
        passportField.clear();
        phoneField.clear();
        emailField.clear();
        addressField.clear();
        customersTable.getSelectionModel().clearSelection();
    }

    private void clearSearch() {
        searchLastNameField.clear();
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
