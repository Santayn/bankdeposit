package org.santayn.bankdeposit.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.models.UserRole;
import org.santayn.bankdeposit.service.EntityNotFoundException;
import org.santayn.bankdeposit.service.InvalidOperationException;
import org.santayn.bankdeposit.service.SessionContext;
import org.santayn.bankdeposit.service.UserService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Контроллер вкладки "Пользователи".
 * Доступен только пользователю с ролью ADMIN.
 * Позволяет добавлять, изменять и удалять пользователей.
 */
@Component
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;
    private final SessionContext sessionContext;

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, Long> colId;

    @FXML
    private TableColumn<User, String> colUsername;

    @FXML
    private TableColumn<User, String> colFullName;

    @FXML
    private TableColumn<User, UserRole> colRole;

    @FXML
    private TableColumn<User, Boolean> colActive;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField passwordField;

    @FXML
    private ComboBox<UserRole> roleCombo;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private Button addButton;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    private final ObservableList<User> usersData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configureTable();
        configureRoleCombo();
        usersTable.setItems(usersData);

        usersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        fillForm(newSelection);
                    }
                }
        );

        loadUsers();
        applyRoleRestrictions();
    }

    private void configureTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));
    }

    private void configureRoleCombo() {
        roleCombo.getItems().setAll(Arrays.asList(UserRole.values()));
    }

    private void loadUsers() {
        try {
            List<User> users = userService.getAllUsers();
            usersData.setAll(users);
        } catch (Exception e) {
            showError("Ошибка загрузки пользователей", e.getMessage());
        }
    }

    private void applyRoleRestrictions() {
        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        // На всякий случай: если вдруг не ADMIN — выключим кнопки
        if (currentUser.getRole() != UserRole.ADMIN) {
            addButton.setDisable(true);
            updateButton.setDisable(true);
            deleteButton.setDisable(true);
            usernameField.setDisable(true);
            fullNameField.setDisable(true);
            passwordField.setDisable(true);
            roleCombo.setDisable(true);
            activeCheckBox.setDisable(true);
        }
    }

    @FXML
    private void onAddUser() {
        try {
            User user = buildUserFromForm(null);
            User saved = userService.createUser(user);
            usersData.add(saved);
            usersTable.getSelectionModel().select(saved);
            showInfo("Пользователь создан", "Пользователь успешно добавлен.");
        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка при создании пользователя", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка при создании пользователя", e.toString());
        }
    }

    @FXML
    private void onUpdateUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Не выбран пользователь", "Выберите пользователя в таблице.");
            return;
        }

        try {
            User updatedData = buildUserFromForm(selected.getId());

            // Если пароль в форме пустой, оставляем старый пароль
            if (updatedData.getPassword() == null || updatedData.getPassword().isBlank()) {
                updatedData.setPassword(selected.getPassword());
            }

            User saved = userService.updateUser(selected.getId(), updatedData);

            int index = usersData.indexOf(selected);
            if (index >= 0) {
                usersData.set(index, saved);
            } else {
                loadUsers();
            }

            usersTable.getSelectionModel().select(saved);
            showInfo("Пользователь обновлён", "Изменения успешно сохранены.");
        } catch (InvalidOperationException | EntityNotFoundException e) {
            showError("Ошибка при обновлении пользователя", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка при обновлении пользователя", e.toString());
        }
    }

    @FXML
    private void onDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Не выбран пользователь", "Выберите пользователя для удаления.");
            return;
        }

        User currentUser = sessionContext.getCurrentUser();
        if (currentUser != null && selected.getId().equals(currentUser.getId())) {
            showError("Нельзя удалить текущего пользователя", "Вы не можете удалить учётную запись, под которой вошли.");
            return;
        }

        try {
            userService.deleteUser(selected.getId());
            usersData.remove(selected);
            clearForm();
            showInfo("Пользователь удалён", "Пользователь успешно удалён.");
        } catch (EntityNotFoundException e) {
            showError("Ошибка при удалении пользователя", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка при удалении пользователя", e.toString());
        }
    }

    private User buildUserFromForm(Long id) {
        String username = usernameField.getText();
        String fullName = fullNameField.getText();
        String password = passwordField.getText();
        UserRole role = roleCombo.getValue();
        boolean active = activeCheckBox.isSelected();

        if (username == null || username.isBlank()) {
            throw new InvalidOperationException("Имя пользователя обязательно для заполнения");
        }
        if (role == null) {
            throw new InvalidOperationException("Необходимо выбрать роль пользователя");
        }

        return User.builder()
                .id(id)
                .username(username.trim())
                .fullName(fullName == null || fullName.isBlank() ? null : fullName.trim())
                .password(password == null ? null : password.trim())
                .role(role)
                .active(active)
                .build();
    }

    private void fillForm(User user) {
        usernameField.setText(user.getUsername());
        fullNameField.setText(user.getFullName());
        passwordField.clear();
        roleCombo.setValue(user.getRole());
        activeCheckBox.setSelected(Boolean.TRUE.equals(user.getActive()));
    }

    private void clearForm() {
        usernameField.clear();
        fullNameField.clear();
        passwordField.clear();
        roleCombo.setValue(null);
        activeCheckBox.setSelected(true);
        usersTable.getSelectionModel().clearSelection();
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
