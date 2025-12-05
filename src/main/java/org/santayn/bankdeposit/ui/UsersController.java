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
import org.santayn.bankdeposit.service.EntityNotFoundException;
import org.santayn.bankdeposit.service.InvalidOperationException;
import org.santayn.bankdeposit.service.SessionContext;
import org.santayn.bankdeposit.service.UserService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Контроллер вкладки "Пользователи".
 */
@Component
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;
    private final SessionContext sessionContext;

    // Поиск
    @FXML
    private TextField usernameSearchField;

    @FXML
    private Button findButton;

    @FXML
    private Button resetSearchButton;

    @FXML
    private Button refreshButton;

    // Таблица
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

    // Форма
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
    private Label hintLabel;

    @FXML
    private Button newButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button clearButton;

    private final ObservableList<User> users = FXCollections.observableArrayList();

    private User selectedUser;

    @FXML
    public void initialize() {
        setupTable();
        setupRoles();
        loadAllUsers();
        setupSelectionListener();
        resetFormToDefault();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        fullNameColumn.setCellValueFactory(cell -> {
            User u = cell.getValue();
            return new SimpleStringProperty(u != null && u.getFullName() != null ? u.getFullName() : "");
        });

        roleColumn.setCellValueFactory(cell -> {
            User u = cell.getValue();
            return new SimpleStringProperty(u != null && u.getRole() != null ? u.getRole().name() : "");
        });

        activeColumn.setCellValueFactory(cell -> {
            User u = cell.getValue();
            boolean active = u != null && Boolean.TRUE.equals(u.getActive());
            return new SimpleBooleanProperty(active);
        });

        activeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(Boolean.TRUE.equals(item) ? "Да" : "Нет");
                }
            }
        });

        usersTable.setItems(users);
    }

    private void setupRoles() {
        roleComboBox.getItems().setAll(UserRole.values());
        roleComboBox.getSelectionModel().select(UserRole.OPERATOR);
    }

    private void setupSelectionListener() {
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selectedUser = newSel;
            fillFormFromSelection();
            updateButtonsState();
        });

        if (!users.isEmpty()) {
            usersTable.getSelectionModel().selectFirst();
        }
        updateButtonsState();
    }

    private void updateButtonsState() {
        boolean hasSelection = selectedUser != null;

        deleteButton.setDisable(!hasSelection);

        // Нельзя удалять admin
        if (hasSelection && selectedUser.getRole() == UserRole.ADMIN) {
            deleteButton.setDisable(true);
        }
    }

    private void loadAllUsers() {
        List<User> list = userService.getAllUsers();
        users.setAll(list);
    }

    private void fillFormFromSelection() {
        if (selectedUser == null) {
            resetFormToDefault();
            return;
        }

        usernameField.setText(selectedUser.getUsername() != null ? selectedUser.getUsername() : "");
        passwordField.setText(selectedUser.getPassword() != null ? selectedUser.getPassword() : "");
        fullNameField.setText(selectedUser.getFullName() != null ? selectedUser.getFullName() : "");
        roleComboBox.getSelectionModel().select(selectedUser.getRole() != null ? selectedUser.getRole() : UserRole.OPERATOR);
        activeCheckBox.setSelected(Boolean.TRUE.equals(selectedUser.getActive()));

        hintLabel.setText("Редактирование существующего пользователя (ID=" + selectedUser.getId() + ").");
    }

    private void resetFormToDefault() {
        usernameField.clear();
        passwordField.clear();
        fullNameField.clear();
        roleComboBox.getSelectionModel().select(UserRole.OPERATOR);
        activeCheckBox.setSelected(true);

        hintLabel.setText("Новый пользователь будет создан при сохранении без выбранной строки.");
    }

    private User buildUserFromForm() {
        User u = new User();
        u.setUsername(usernameField.getText());
        u.setPassword(passwordField.getText());
        u.setFullName(fullNameField.getText());
        u.setRole(roleComboBox.getValue());
        u.setActive(activeCheckBox.isSelected());
        return u;
    }

    private void reloadAndSelect(Long id) {
        loadAllUsers();

        if (id == null) {
            usersTable.getSelectionModel().clearSelection();
            selectedUser = null;
            updateButtonsState();
            return;
        }

        for (User u : users) {
            if (id.equals(u.getId())) {
                usersTable.getSelectionModel().select(u);
                usersTable.scrollTo(u);
                selectedUser = u;
                break;
            }
        }
        updateButtonsState();
    }

    // -------------------- Actions --------------------

    @FXML
    private void onRefresh() {
        loadAllUsers();
        updateButtonsState();
    }

    @FXML
    private void onFindByUsername() {
        String query = usernameSearchField.getText();
        if (query == null || query.isBlank()) {
            showInfo("Поиск", "Введите логин для поиска.");
            return;
        }

        Optional<User> found = userService.findByUsername(query.trim());

        if (found.isEmpty()) {
            showInfo("Поиск", "Пользователь не найден.");
            return;
        }

        User u = found.get();

        loadAllUsers();
        reloadAndSelect(u.getId());
    }

    @FXML
    private void onResetSearch() {
        usernameSearchField.clear();
        loadAllUsers();
        if (!users.isEmpty()) {
            usersTable.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onNewUser() {
        usersTable.getSelectionModel().clearSelection();
        selectedUser = null;
        resetFormToDefault();
        updateButtonsState();
    }

    @FXML
    private void onClearForm() {
        resetFormToDefault();
    }

    @FXML
    private void onSaveUser() {
        try {
            User formUser = buildUserFromForm();

            User saved;

            if (selectedUser == null) {
                saved = userService.createUserValidated(formUser);
                showInfo("Пользователи", "Пользователь создан: " + saved.getUsername());
            } else {
                saved = userService.updateUserValidated(selectedUser.getId(), formUser, sessionContext.getCurrentUser());
                showInfo("Пользователи", "Пользователь обновлён: " + saved.getUsername());
            }

            reloadAndSelect(saved.getId());

        } catch (InvalidOperationException | EntityNotFoundException ex) {
            showError("Сохранение пользователя", ex.getMessage());
        } catch (Exception ex) {
            showError("Неожиданная ошибка", ex.toString());
        }
    }

    @FXML
    private void onDeleteUser() {
        if (selectedUser == null) {
            showInfo("Удаление", "Сначала выберите пользователя.");
            return;
        }

        if (selectedUser.getRole() == UserRole.ADMIN) {
            showError("Удаление", "Пользователя с ролью ADMIN удалять нельзя.");
            return;
        }

        User current = sessionContext.getCurrentUser();
        if (current != null && current.getId() != null && current.getId().equals(selectedUser.getId())) {
            showError("Удаление", "Нельзя удалить самого себя.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление пользователя");
        confirm.setHeaderText(null);
        confirm.setContentText("Удалить пользователя " + selectedUser.getUsername() + "?");

        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    userService.deleteUserValidated(selectedUser.getId(), sessionContext.getCurrentUser());
                    reloadAndSelect(null);
                    resetFormToDefault();
                } catch (InvalidOperationException | EntityNotFoundException ex) {
                    showError("Удаление пользователя", ex.getMessage());
                } catch (Exception ex) {
                    showError("Неожиданная ошибка", ex.toString());
                }
            }
        });
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
