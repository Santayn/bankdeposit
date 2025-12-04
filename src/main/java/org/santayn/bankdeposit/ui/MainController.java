package org.santayn.bankdeposit.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.service.SessionContext;
import org.springframework.stereotype.Component;

/**
 * Контроллер главного окна приложения.
 */
@Component
@RequiredArgsConstructor
public class MainController {

    private final SessionContext sessionContext;

    @FXML
    private Label currentUserLabel;

    @FXML
    private TabPane mainTabPane;

    @FXML
    public void initialize() {
        User user = sessionContext.getCurrentUser();
        if (user != null) {
            currentUserLabel.setText(
                    user.getFullName() + " (" + user.getRole().name() + ")"
            );
        } else {
            currentUserLabel.setText("неизвестен");
        }
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText("Депозитный отдел банка");
        alert.setContentText("Учебное настольное приложение на JavaFX + Spring Boot.");
        alert.showAndWait();
    }
}
