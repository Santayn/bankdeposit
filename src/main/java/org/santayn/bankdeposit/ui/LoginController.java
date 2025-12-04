package org.santayn.bankdeposit.ui;

import javafx.event.ActionEvent;
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
import org.santayn.bankdeposit.service.InvalidOperationException;
import org.santayn.bankdeposit.service.SessionContext;
import org.santayn.bankdeposit.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Контроллер окна авторизации.
 * После успешного входа открывает главное окно приложения.
 */
@Component
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;
    private final ApplicationContext applicationContext;
    private final SessionContext sessionContext;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button exitButton;

    /**
     * Основной Stage приложения, в который после авторизации
     * подставляется главное окно.
     */
    private Stage primaryStage;

    /**
     * Устанавливает основной Stage приложения, чтобы при успешном входе
     * заменить сцену на главное окно.
     *
     * Вызывается из BankDepositJavaFxApplication после загрузки LoginView.
     *
     * @param primaryStage основной Stage
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    public void initialize() {
        // Для удобства тестирования сразу подставим admin
        if (usernameField != null) {
            usernameField.setText("admin");
        }
    }

    @FXML
    private void onLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        try {
            User user = userService.authenticate(username, password);
            // Сохраняем пользователя в контексте сессии
            sessionContext.setCurrentUser(user);
            // Открываем главное окно
            openMainWindow(user);
        } catch (InvalidOperationException e) {
            showError("Ошибка входа", e.getMessage());
        } catch (Exception e) {
            showError("Неожиданная ошибка", e.toString());
        }
    }

    @FXML
    private void onExit(ActionEvent event) {
        if (primaryStage != null) {
            primaryStage.close();
        } else {
            System.exit(0);
        }
    }

    private void openMainWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MainView.fxml")
            );
            loader.setControllerFactory(applicationContext::getBean);

            Parent root = loader.load();

            Scene scene = new Scene(root, 1000, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Депозитный отдел банка — пользователь: "
                    + user.getFullName() + " (" + user.getRole().name() + ")");
            primaryStage.show();
        } catch (Exception e) {
            showError("Ошибка открытия главного окна", e.toString());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
