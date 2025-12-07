package org.santayn.bankdeposit.ui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.models.UserRole;
import org.santayn.bankdeposit.service.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Контроллер вкладки "Пользователи".
 *
 * Функции:
 * - просмотр списка
 * - поиск по логину
 * - создание нового пользователя
 * - редактирование
 * - удаление
 *
 * UI-доступ:
 * - только ADMIN имеет право на управление пользователями
 * - остальные роли: только просмотр (вкладка обычно скрыта в MainController)
 */
@Component
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;
    private final SessionContext sessionContext;
    private final UiAccessManager uiAccessManager;

    // ---------------------- Search/top controls ----------------------

    @FXML
    private TextField usernameSearchField;

    @FXML
    private Button findButton;

    @FXML
    private Button resetSearchButton;

    @FXML
    private Button refreshButton;

    // ---------------------- Table ----------------------

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, Long> idColumn;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, String> fullNameColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, Boolean> activeColumn;

    // ---------------------- Form ----------------------

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField fullNameField;

    @FXML
    private ComboBox<UserRole> roleComboBox;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private Button newButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button clearButton;

    // ---------------------- Data ----------------------

    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<UserRole> roles = FXCollections.observableArrayList();

    private User selectedUser;

    // ---------------------- Init ----------------------

    @FXML
    public void initialize() {
        setupRoles();
        setupTable();
        setupSelectionListener();
        bindItems();

        loadAllUsers();
        clearForm();

        applyRoleUiAccess();
        updateButtonsState();
    }

    private void bindItems() {
        usersTable.setItems(users);
        roleComboBox.setItems(roles);
    }

    private void setupRoles() {
        roles.setAll(UserRole.values());
        if (!roles.isEmpty()) {
            roleComboBox.getSelectionModel().selectFirst();
        }
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        roleColumn.setCellValueFactory(cell -> {
            UserRole r = cell.getValue().getRole();
            return new SimpleStringProperty(r != null ? r.name() : "");
        });

        activeColumn.setCellValueFactory(cell ->
                new SimpleBooleanProperty(Boolean.TRUE.equals(cell.getValue().getActive()))
        );

        activeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                setText(Boolean.TRUE.equals(item) ? "Да" : "Нет");
            }
        });
    }

    private void setupSelectionListener() {
        usersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> {
                    selectedUser = newSel;
                    fillFormFromSelection();
                    updateButtonsState();
                }
        );
    }

    // ---------------------- Role UI access ----------------------

    private void applyRoleUiAccess() {
        User current = sessionContext.getCurrentUser();
        boolean canManage = uiAccessManager.canManageUsers(current);

        usernameField.setDisable(!canManage);
        passwordField.setDisable(!canManage);
        fullNameField.setDisable(!canManage);
        roleComboBox.setDisable(!canManage);
        activeCheckBox.setDisable(!canManage);

        newButton.setDisable(!canManage);
        saveButton.setDisable(!canManage);
        deleteButton.setDisable(!canManage);
        clearButton.setDisable(!canManage);
    }

    // ---------------------- Load ----------------------

    private void loadAllUsers() {
        List<User> list = userService.getAllUsers();
        users.setAll(list);

        if (!users.isEmpty()) {
            usersTable.getSelectionModel().selectFirst();
            selectedUser = usersTable.getSelectionModel().getSelectedItem();
        } else {
            selectedUser = null;
        }

        fillFormFromSelection();
        updateButtonsState();
    }

    // ---------------------- Search ----------------------

    @FXML
    private void onFindByUsername() {
        String query = usernameSearchField.getText();
        List<User> list = userService.getAllUsers();

        if (query != null && !query.isBlank()) {
            String q = query.trim().toLowerCase();
            list = list.stream()
                    .filter(u -> u.getUsername() != null && u.getUsername().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        users.setAll(list);

        if (!users.isEmpty()) {
            usersTable.getSelectionModel().selectFirst();
            selectedUser = usersTable.getSelectionModel().getSelectedItem();
        } else {
            selectedUser = null;
        }

        fillFormFromSelection();
        updateButtonsState();
    }

    @FXML
    private void onResetSearch() {
        usernameSearchField.clear();
        loadAllUsers();
    }

    @FXML
    private void onRefresh() {
        loadAllUsers();
        showInfo("Пользователи", "Список обновлён.");
    }

    // ---------------------- Form actions ----------------------

    @FXML
    private void onNewUser() {
        User current = sessionContext.getCurrentUser();
        if (!uiAccessManager.canManageUsers(current)) {
            showInfo("Пользователи", "Недостаточно прав.");
            return;
        }

        usersTable.getSelectionModel().clearSelection();
        selectedUser = null;
        clearForm();
        updateButtonsState();
        usernameField.requestFocus();
    }

    @FXML
    private void onSaveUser() {
        User current = sessionContext.getCurrentUser();
        if (!uiAccessManager.canManageUsers(current)) {
            showInfo("Пользователи", "Недостаточно прав.");
            return;
        }

        try {
            String username = safe(usernameField.getText());
            String password = safe(passwordField.getText());
            String fullName = safe(fullNameField.getText());
            UserRole role = roleComboBox.getValue();
            boolean active = activeCheckBox.isSelected();

            if (username.isBlank()) {
                showError("Пользователь", "Логин не может быть пустым.");
                return;
            }
            if (fullName.isBlank()) {
                showError("Пользователь", "ФИО не может быть пустым.");
                return;
            }
            if (role == null) {
                showError("Пользователь", "Выберите роль.");
                return;
            }

            if (selectedUser == null) {
                if (password.isBlank()) {
                    showError("Пользователь", "Для нового пользователя пароль обязателен.");
                    return;
                }

                User u = new User();
                u.setUsername(username);
                u.setPassword(password);
                u.setFullName(fullName);
                u.setRole(role);
                u.setActive(active);

                User created = userService.createUser(u);
                reloadAndSelect(created.getId());

                showInfo("Пользователь", "Пользователь создан: " + safe(created.getUsername()));
            } else {
                User updated = new User();
                updated.setUsername(username);

                if (password.isBlank()) {
                    updated.setPassword(selectedUser.getPassword());
                } else {
                    updated.setPassword(password);
                }

                updated.setFullName(fullName);
                updated.setRole(role);
                updated.setActive(active);

                User result = userService.updateUser(selectedUser.getId(), updated);
                reloadAndSelect(result.getId());

                showInfo("Пользователь", "Пользователь обновлён: " + safe(result.getUsername()));
            }

        } catch (InvalidOperationException | EntityNotFoundException ex) {
            showError("Пользователь", ex.getMessage());
        } catch (Exception ex) {
            showError("Неожиданная ошибка", ex.toString());
        }
    }

    @FXML
    private void onDeleteUser() {
        User current = sessionContext.getCurrentUser();
        if (!uiAccessManager.canManageUsers(current)) {
            showInfo("Удаление", "Недостаточно прав.");
            return;
        }

        if (selectedUser == null || selectedUser.getId() == null) {
            showInfo("Удаление", "Сначала выберите пользователя.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление пользователя");
        confirm.setHeaderText(null);
        confirm.setContentText("Удалить пользователя " + safe(selectedUser.getUsername()) + "?");

        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    userService.deleteUser(selectedUser.getId());
                    loadAllUsers();
                    clearForm();
                } catch (InvalidOperationException | EntityNotFoundException ex) {
                    showError("Удаление", ex.getMessage());
                } catch (Exception ex) {
                    showError("Неожиданная ошибка", ex.toString());
                }
            }
        });
    }

    @FXML
    private void onClearForm() {
        clearForm();
        updateButtonsState();
    }

    // ---------------------- Helpers ----------------------

    private void reloadAndSelect(Long id) {
        List<User> list = userService.getAllUsers();
        users.setAll(list);

        selectedUser = null;

        if (id != null) {
            for (User u : users) {
                if (Objects.equals(u.getId(), id)) {
                    usersTable.getSelectionModel().select(u);
                    usersTable.scrollTo(u);
                    selectedUser = u;
                    break;
                }
            }
        }

        if (selectedUser == null && !users.isEmpty()) {
            usersTable.getSelectionModel().selectFirst();
            selectedUser = usersTable.getSelectionModel().getSelectedItem();
        }

        fillFormFromSelection();
        updateButtonsState();
    }

    private void fillFormFromSelection() {
        if (selectedUser == null) {
            clearForm();
            return;
        }

        usernameField.setText(safe(selectedUser.getUsername()));
        passwordField.clear();
        fullNameField.setText(safe(selectedUser.getFullName()));

        if (selectedUser.getRole() != null) {
            roleComboBox.getSelectionModel().select(selectedUser.getRole());
        } else if (!roles.isEmpty()) {
            roleComboBox.getSelectionModel().selectFirst();
        }

        activeCheckBox.setSelected(Boolean.TRUE.equals(selectedUser.getActive()));
    }

    private void clearForm() {
        usernameField.clear();
        passwordField.clear();
        fullNameField.clear();

        if (!roles.isEmpty()) {
            roleComboBox.getSelectionModel().selectFirst();
        } else {
            roleComboBox.getSelectionModel().clearSelection();
        }

        activeCheckBox.setSelected(true);
    }

    private void updateButtonsState() {
        User current = sessionContext.getCurrentUser();
        boolean canManage = uiAccessManager.canManageUsers(current);

        boolean hasSelection = selectedUser != null && selectedUser.getId() != null;

        deleteButton.setDisable(!canManage || !hasSelection);
        saveButton.setDisable(!canManage);
        newButton.setDisable(!canManage);
        clearButton.setDisable(!canManage);
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
