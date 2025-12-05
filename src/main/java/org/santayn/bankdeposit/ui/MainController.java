package org.santayn.bankdeposit.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.models.UserRole;
import org.santayn.bankdeposit.service.SessionContext;
import org.springframework.stereotype.Component;

/**
 * Контроллер главного окна приложения.
 * Отвечает за:
 * 1) отображение текущего пользователя;
 * 2) базовые ограничения доступа к вкладкам по ролям;
 * 3) меню "Выход" и "О программе".
 */
@Component
@RequiredArgsConstructor
public class MainController {

    private final SessionContext sessionContext;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Label currentUserLabel;

    @FXML
    public void initialize() {
        updateCurrentUserLabel();
        applyRoleRestrictions();
    }

    private void updateCurrentUserLabel() {
        User u = sessionContext.getCurrentUser();
        if (u == null) {
            currentUserLabel.setText("неизвестен");
            return;
        }
        String fullName = u.getFullName() != null ? u.getFullName() : u.getUsername();
        currentUserLabel.setText(fullName + " (" + u.getRole().name() + ")");
    }

    /**
     * Простые ограничения вкладок:
     *
     * ADMIN:
     *  - доступ ко всему.
     *
     * DIRECTOR / MANAGER:
     *  - всё кроме управления пользователями (можно оставить доступ по желанию).
     *
     * OPERATOR:
     *  - работает с клиентами/договорами/операциями;
     *  - пользователи недоступны.
     *
     * AUDITOR:
     *  - только чтение: операции/отчёты;
     *  - отключаем вкладки, где есть активные CRUD-действия.
     *
     * Это UI-уровень. При желании позже добавим проверки и в сервисы.
     */
    private void applyRoleRestrictions() {
        User u = sessionContext.getCurrentUser();
        if (u == null || u.getRole() == null) {
            return;
        }

        UserRole role = u.getRole();

        Tab clientsTab = findTabByText("Клиенты");
        Tab productsTab = findTabByText("Продукты вкладов");
        Tab contractsTab = findTabByText("Договоры вкладов");
        Tab operationsTab = findTabByText("Операции");
        Tab usersTab = findTabByText("Пользователи");
        Tab reportsTab = findTabByText("Отчёты");

        // На всякий случай: если где-то вкладки переименуешь — приложение не упадёт.
        if (role == UserRole.ADMIN) {
            // Ничего не отключаем
            return;
        }

        if (role == UserRole.DIRECTOR || role == UserRole.MANAGER) {
            // Можно разрешить всё, кроме пользователей
            if (usersTab != null) {
                usersTab.setDisable(true);
            }
            return;
        }

        if (role == UserRole.OPERATOR) {
            if (usersTab != null) {
                usersTab.setDisable(true);
            }
            // Остальное оставляем активным
            return;
        }

        if (role == UserRole.AUDITOR) {
            // Аудитор — только просмотр.
            // Самый простой UI-вариант: отключаем вкладки, где точно есть изменения.
            if (clientsTab != null) {
                clientsTab.setDisable(true);
            }
            if (productsTab != null) {
                productsTab.setDisable(true);
            }
            if (contractsTab != null) {
                contractsTab.setDisable(true);
            }
            if (usersTab != null) {
                usersTab.setDisable(true);
            }

            // Операции и отчёты оставляем.
            // Если хочешь, можно и "Операции" отключить, оставив только отчёты.
            if (reportsTab != null) {
                reportsTab.setDisable(false);
            }
            if (operationsTab != null) {
                operationsTab.setDisable(false);
            }

            // Перекидываем фокус на "Отчёты", если всё остальное выключено
            if (reportsTab != null) {
                mainTabPane.getSelectionModel().select(reportsTab);
            }
        }
    }

    private Tab findTabByText(String text) {
        if (mainTabPane == null || mainTabPane.getTabs() == null) {
            return null;
        }
        for (Tab t : mainTabPane.getTabs()) {
            if (t != null && text.equals(t.getText())) {
                return t;
            }
        }
        return null;
    }

    @FXML
    private void onExit() {
        // Закрытие окна полностью управляется JavaFX-стадией.
        // Здесь просто завершаем приложение.
        System.exit(0);
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText("Депозитный отдел банка");
        alert.setContentText(
                "Учебное приложение на JavaFX + Spring Boot + JPA.\n\n" +
                        "Функции:\n" +
                        "- клиенты\n" +
                        "- депозитные продукты\n" +
                        "- договоры вкладов\n" +
                        "- операции\n" +
                        "- пользователи\n" +
                        "- отчёты"
        );
        alert.showAndWait();
    }
}
