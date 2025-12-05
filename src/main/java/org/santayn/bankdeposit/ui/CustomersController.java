package org.santayn.bankdeposit.ui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.service.CustomerService;
import org.santayn.bankdeposit.service.EntityNotFoundException;
import org.santayn.bankdeposit.service.InvalidOperationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Контроллер вкладки "Клиенты".
 * Позволяет просматривать список клиентов и выполнять операции:
 * - создать нового клиента;
 * - отредактировать выбранного;
 * - удалить клиента;
 * - очистить форму;
 * - найти по паспорту.
 */
@Component
@RequiredArgsConstructor
public class CustomersController {

    private final CustomerService customerService;

    @FXML
    private TableView<Customer> customersTable;

    @FXML
    private TableColumn<Customer, String> lastNameColumn;

    @FXML
    private TableColumn<Customer, String> firstNameColumn;

    @FXML
    private TableColumn<Customer, String> middleNameColumn;

    @FXML
    private TableColumn<Customer, String> dateOfBirthColumn;

    @FXML
    private TableColumn<Customer, String> passportColumn;

    @FXML
    private TableColumn<Customer, String> phoneColumn;

    @FXML
    private TableColumn<Customer, String> emailColumn;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField middleNameField;

    @FXML
    private DatePicker dateOfBirthPicker;

    @FXML
    private TextField passportField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField passportSearchField;

    @FXML
    private Button newButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button findByPassportButton;

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Текущий редактируемый клиент (null, если создаём нового).
     */
    private Customer selectedCustomer;

    @FXML
    public void initialize() {
        setupTable();
        loadCustomers();
        setupSelectionListener();
    }

    private void setupTable() {
        customersTable.setItems(customers);

        lastNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        Optional.ofNullable(cellData.getValue().getLastName()).orElse("")
                ));

        firstNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        Optional.ofNullable(cellData.getValue().getFirstName()).orElse("")
                ));

        middleNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        Optional.ofNullable(cellData.getValue().getMiddleName()).orElse("")
                ));

        dateOfBirthColumn.setCellValueFactory(cellData -> {
            LocalDate dob = cellData.getValue().getDateOfBirth();
            String text = dob != null ? dob.format(dateFormatter) : "";
            return new SimpleStringProperty(text);
        });

        passportColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        Optional.ofNullable(cellData.getValue().getPassportNumber()).orElse("")
                ));

        phoneColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        Optional.ofNullable(cellData.getValue().getPhone()).orElse("")
                ));

        emailColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        Optional.ofNullable(cellData.getValue().getEmail()).orElse("")
                ));
    }

    private void loadCustomers() {
        List<Customer> list = customerService.getAllCustomers();
        customers.setAll(list);
    }

    private void setupSelectionListener() {
        customersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        selectedCustomer = newSelection;
                        fillFormFromCustomer(newSelection);
                    }
                }
        );
    }

    private void fillFormFromCustomer(Customer customer) {
        firstNameField.setText(
                Optional.ofNullable(customer.getFirstName()).orElse("")
        );
        lastNameField.setText(
                Optional.ofNullable(customer.getLastName()).orElse("")
        );
        middleNameField.setText(
                Optional.ofNullable(customer.getMiddleName()).orElse("")
        );
        dateOfBirthPicker.setValue(customer.getDateOfBirth());
        passportField.setText(
                Optional.ofNullable(customer.getPassportNumber()).orElse("")
        );
        addressField.setText(
                Optional.ofNullable(customer.getAddress()).orElse("")
        );
        phoneField.setText(
                Optional.ofNullable(customer.getPhone()).orElse("")
        );
        emailField.setText(
                Optional.ofNullable(customer.getEmail()).orElse("")
        );
    }

    private void clearForm() {
        selectedCustomer = null;
        customersTable.getSelectionModel().clearSelection();

        firstNameField.clear();
        lastNameField.clear();
        middleNameField.clear();
        dateOfBirthPicker.setValue(null);
        passportField.clear();
        addressField.clear();
        phoneField.clear();
        emailField.clear();
    }

    // ------------------------ Обработчики кнопок ------------------------

    /**
     * Нажатие на кнопку "Новый".
     * Очищает форму и снимает выделение в таблице.
     */
    @FXML
    private void onNewCustomer() {
        clearForm();
    }

    /**
     * Нажатие на кнопку "Сохранить".
     * Если selectedCustomer == null — создаём нового,
     * иначе обновляем существующего.
     */
    @FXML
    private void onSaveCustomer() {
        try {
            Customer formCustomer = buildCustomerFromForm();

            if (selectedCustomer == null) {
                // Создание нового
                Customer created = customerService.createCustomer(formCustomer);
                showInfo("Клиент создан",
                        "Клиент успешно добавлен с id=" + created.getId());
            } else {
                // Обновление существующего
                Customer updated = customerService.updateCustomer(
                        selectedCustomer.getId(), formCustomer
                );
                showInfo("Клиент обновлён",
                        "Данные клиента успешно обновлены (id=" + updated.getId() + ")");
            }

            // Перезагружаем список и очищаем форму
            loadCustomers();
            clearForm();

        } catch (InvalidOperationException | EntityNotFoundException ex) {
            showError("Ошибка сохранения", ex.getMessage());
        } catch (Exception ex) {
            showError("Неожиданная ошибка", ex.toString());
        }
    }

    /**
     * Нажатие на кнопку "Удалить".
     * Удаляет выбранного клиента.
     */
    @FXML
    private void onDeleteCustomer() {
        if (selectedCustomer == null) {
            showError("Удаление клиента", "Сначала выберите клиента в таблице");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение удаления");
        confirm.setHeaderText(null);
        confirm.setContentText("Удалить клиента \"" +
                selectedCustomer.getLastName() + " " +
                selectedCustomer.getFirstName() + "\"?");

        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    customerService.deleteCustomer(selectedCustomer.getId());
                    showInfo("Клиент удалён", "Клиент успешно удалён");
                    loadCustomers();
                    clearForm();
                } catch (EntityNotFoundException | InvalidOperationException ex) {
                    showError("Ошибка удаления", ex.getMessage());
                } catch (Exception ex) {
                    showError("Неожиданная ошибка", ex.toString());
                }
            }
        });
    }

    /**
     * Нажатие на кнопку "Очистить".
     * Просто очищает форму.
     */
    @FXML
    private void onClearForm() {
        clearForm();
    }

    /**
     * Поиск клиента по паспорту.
     */
    @FXML
    private void onFindByPassport() {
        String passport = passportSearchField.getText();
        if (passport == null || passport.isBlank()) {
            showError("Поиск по паспорту", "Введите номер паспорта");
            return;
        }

        try {
            Customer found = customerService.findByPassportNumber(passport);
            // Выделяем его в таблице и заполняем форму
            customersTable.getSelectionModel().select(found);
            customersTable.scrollTo(found);
            selectedCustomer = found;
            fillFormFromCustomer(found);
        } catch (EntityNotFoundException | InvalidOperationException ex) {
            showError("Поиск по паспорту", ex.getMessage());
        } catch (Exception ex) {
            showError("Неожиданная ошибка", ex.toString());
        }
    }

    // --------------------- Вспомогательные методы ---------------------

    private Customer buildCustomerFromForm() {
        Customer c = new Customer();
        c.setFirstName(firstNameField.getText());
        c.setLastName(lastNameField.getText());
        c.setMiddleName(middleNameField.getText());
        c.setDateOfBirth(dateOfBirthPicker.getValue());
        c.setPassportNumber(passportField.getText());
        c.setAddress(addressField.getText());
        c.setPhone(phoneField.getText());
        c.setEmail(emailField.getText());
        return c;
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
