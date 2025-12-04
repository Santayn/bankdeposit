package org.santayn.bankdeposit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.santayn.bankdeposit.ui.LoginController;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX-приложение, использующее Spring-контекст.
 * При старте открывает окно авторизации (LoginView.fxml).
 * После успешного входа окно логина заменяется на главное окно.
 */
public class BankDepositJavaFxApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(String[]::new);
        this.context = BankDepositApplication.createSpringContext(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/LoginView.fxml")
        );
        loader.setControllerFactory(context::getBean);

        Parent root = loader.load();

        LoginController loginController = loader.getController();
        loginController.setPrimaryStage(primaryStage);

        Scene scene = new Scene(root, 450, 250);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Депозитный отдел банка — вход в систему");
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
        Platform.exit();
    }
}
