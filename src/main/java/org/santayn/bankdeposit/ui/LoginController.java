package org.santayn.bankdeposit.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.service.SessionContext;
import org.santayn.bankdeposit.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Контроллер окна авторизации.
 *
 * Работает с LoginView.fxml.
 *
 * Логика:
 * - ищем пользователя по username среди всех пользователей
 * - проверяем пароль
 * - проверяем active
 * - сохраняем пользователя в SessionContext
 * - подменяем сцену primaryStage на MainView.fxml
 */
@Component
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;
    private final SessionContext sessionContext;
    private final ApplicationContext applicationContext;

    private Stage primaryStage;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button exitButton;

    /**
     * Устанавливается из BankDepositJavaFxApplication после загрузки FXML.
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    public void initialize() {
        // Ничего критичного, но можно добавить удобство:
        if (usernameField != null) {
            usernameField.requestFocus();
        }
    }

    @FXML
    private void onLogin() {
        try {
            String username = safe(usernameField.getText());
            String password = safe(passwordField.getText());

            if (username.isBlank()) {
                showError("Авторизация", "Введите логин.");
                return;
            }
            if (password.isBlank()) {
                showError("Авторизация", "Введите пароль.");
                return;
            }

            List<User> all = userService.getAllUsers();

            User found = all.stream()
                    .filter(u -> u.getUsername() != null && u.getUsername().equals(username))
                    .findFirst()
                    .orElse(null);

            if (found == null) {
                showError("Авторизация", "Пользователь не найден.");
                return;
            }

            if (found.getActive() == null || !found.getActive()) {
                showError("Авторизация", "Пользователь неактивен.");
                return;
            }

            String storedPassword = found.getPassword() != null ? found.getPassword() : "";
            if (!storedPassword.equals(password)) {
                showError("Авторизация", "Неверный пароль.");
                return;
            }

            // Успешный вход
            sessionContext.setCurrentUser(found);

            openMainView();

        } catch (Exception ex) {
            showError("Неожиданная ошибка", ex.toString());
        }
    }

    @FXML
    private void onExit() {
        if (primaryStage != null) {
            primaryStage.close();
        }
    }

    // ---------------------- Navigation ----------------------

    private void openMainView() {
        if (primaryStage == null) {
            showError("Авторизация", "Не удалось определить главное окно приложения.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            loader.setControllerFactory(applicationContext::getBean);

            Parent root = loader.load();

            Scene scene = new Scene(root, 1100, 700);
            primaryStage.setTitle("Депозитный отдел банка");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);
            primaryStage.centerOnScreen();

        } catch (IOException e) {
            showError("Ошибка", "Не удалось открыть главное окно: " + e.getMessage());
        }
    }

    // ---------------------- Helpers ----------------------

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
}
