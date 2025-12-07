package org.santayn.bankdeposit.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.models.UserRole;
import org.santayn.bankdeposit.service.SessionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Контроллер главного окна приложения.
 *
 * Управляет:
 * - показом текущего пользователя
 * - доступностью вкладок по ролям
 * - меню "Выход" и "О программе"
 */
@Component
@RequiredArgsConstructor
public class MainController {

    private final SessionContext sessionContext;
    private final ApplicationContext applicationContext;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Tab customersTab;

    @FXML
    private Tab productsTab;

    @FXML
    private Tab contractsTab;

    @FXML
    private Tab operationsTab;

    @FXML
    private Tab usersTab;

    @FXML
    private Tab reportsTab;

    @FXML
    private Label currentUserLabel;

    @FXML
    public void initialize() {
        applySessionInfo();
        applyRolePermissions();
    }

    @FXML
    private void onExit() {
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText(null);
        alert.setContentText("Учебная информационная система депозитного отдела банка.");
        alert.showAndWait();
    }

    @FXML
    private void onLogout() {
        sessionContext.clear();
        openLoginWindow();
        closeCurrentStage();
    }

    private void applySessionInfo() {
        User u = sessionContext.getCurrentUser();
        if (u == null) {
            currentUserLabel.setText("неизвестен");
            return;
        }

        String role = u.getRole() != null ? u.getRole().name() : "UNKNOWN";
        String full = safe(u.getFullName());
        String login = safe(u.getUsername());

        String label = login;
        if (!full.isBlank()) {
            label += " (" + full + ")";
        }
        label += " / " + role;

        currentUserLabel.setText(label);
    }

    private void applyRolePermissions() {
        User u = sessionContext.getCurrentUser();
        UserRole role = u != null ? u.getRole() : null;

        disableAllTabs();

        Set<Tab> allowed = new HashSet<>();

        if (role == null) {
            // минимальный безопасный доступ
            allowed.add(contractsTab);
            allowed.add(operationsTab);
            allowed.add(reportsTab);
        } else {
            switch (role) {
                case ADMIN -> allowed.addAll(Arrays.asList(
                        customersTab, productsTab, contractsTab,
                        operationsTab, usersTab, reportsTab
                ));
                case DIRECTOR -> allowed.addAll(Arrays.asList(
                        contractsTab, operationsTab, reportsTab
                ));
                case MANAGER -> allowed.addAll(Arrays.asList(
                        customersTab, productsTab, contractsTab
                ));
                case OPERATOR -> allowed.addAll(Arrays.asList(
                        contractsTab, operationsTab
                ));
                case AUDITOR -> allowed.addAll(Arrays.asList(
                        contractsTab, operationsTab, reportsTab
                ));
            }
        }

        enableTabs(allowed);
        selectFirstEnabledTab();
    }

    private void disableAllTabs() {
        if (customersTab != null) {
            customersTab.setDisable(true);
        }
        if (productsTab != null) {
            productsTab.setDisable(true);
        }
        if (contractsTab != null) {
            contractsTab.setDisable(true);
        }
        if (operationsTab != null) {
            operationsTab.setDisable(true);
        }
        if (usersTab != null) {
            usersTab.setDisable(true);
        }
        if (reportsTab != null) {
            reportsTab.setDisable(true);
        }
    }

    private void enableTabs(Set<Tab> tabs) {
        for (Tab t : tabs) {
            if (t != null) {
                t.setDisable(false);
            }
        }
    }

    private void selectFirstEnabledTab() {
        if (customersTab != null && !customersTab.isDisable()) {
            mainTabPane.getSelectionModel().select(customersTab);
            return;
        }
        if (productsTab != null && !productsTab.isDisable()) {
            mainTabPane.getSelectionModel().select(productsTab);
            return;
        }
        if (contractsTab != null && !contractsTab.isDisable()) {
            mainTabPane.getSelectionModel().select(contractsTab);
            return;
        }
        if (operationsTab != null && !operationsTab.isDisable()) {
            mainTabPane.getSelectionModel().select(operationsTab);
            return;
        }
        if (usersTab != null && !usersTab.isDisable()) {
            mainTabPane.getSelectionModel().select(usersTab);
            return;
        }
        if (reportsTab != null && !reportsTab.isDisable()) {
            mainTabPane.getSelectionModel().select(reportsTab);
        }
    }

    private void openLoginWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Авторизация");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText("Не удалось открыть окно авторизации: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void closeCurrentStage() {
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        stage.close();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
