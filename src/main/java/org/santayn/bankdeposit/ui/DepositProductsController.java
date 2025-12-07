package org.santayn.bankdeposit.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.DepositProduct;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.service.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Контроллер вкладки "Депозитные продукты".
 *
 * Работает с DepositProductsView.fxml:
 * - поиск по названию
 * - таблица продуктов
 * - форма создания/редактирования
 * - удаление
 *
 * UI-доступ:
 * - ADMIN, MANAGER могут создавать/редактировать/удалять
 * - остальные — только просмотр и поиск
 */
@Component
@RequiredArgsConstructor
public class DepositProductsController {

    private final DepositProductService depositProductService;
    private final SessionContext sessionContext;
    private final UiAccessManager uiAccessManager;

    // ---------------------- Search/top controls ----------------------

    @FXML
    private TextField nameSearchField;

    @FXML
    private Button findByNameButton;

    // ---------------------- Table ----------------------

    @FXML
    private TableView<DepositProduct> productsTable;

    @FXML
    private TableColumn<DepositProduct, String> nameColumn;

    @FXML
    private TableColumn<DepositProduct, Integer> termColumn;

    @FXML
    private TableColumn<DepositProduct, BigDecimal> rateColumn;

    @FXML
    private TableColumn<DepositProduct, BigDecimal> minAmountColumn;

    @FXML
    private TableColumn<DepositProduct, BigDecimal> maxAmountColumn;

    @FXML
    private TableColumn<DepositProduct, Boolean> replColumn;

    @FXML
    private TableColumn<DepositProduct, Boolean> partialWithdrawColumn;

    @FXML
    private TableColumn<DepositProduct, Boolean> capitalizationColumn;

    // ---------------------- Form ----------------------

    @FXML
    private TextField nameField;

    @FXML
    private TextField rateField;

    @FXML
    private TextField termMonthsField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextField minAmountField;

    @FXML
    private TextField maxAmountField;

    @FXML
    private CheckBox allowReplenishmentCheckBox;

    @FXML
    private CheckBox allowPartialWithdrawalCheckBox;

    @FXML
    private CheckBox capitalizationCheckBox;

    @FXML
    private Button newButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button clearButton;

    // ---------------------- Data ----------------------

    private final ObservableList<DepositProduct> products = FXCollections.observableArrayList();

    private DepositProduct selectedProduct;

    // ---------------------- Init ----------------------

    @FXML
    public void initialize() {
        setupTable();
        setupSelectionListener();
        bindItems();

        loadAllProducts();
        clearForm();

        applyRoleUiAccess();
        updateButtonsState();
    }

    private void bindItems() {
        productsTable.setItems(products);
    }

    // ---------------------- Role UI access ----------------------

    private void applyRoleUiAccess() {
        User current = sessionContext.getCurrentUser();
        boolean canManage = uiAccessManager.canManageProducts(current);

        newButton.setDisable(!canManage);
        saveButton.setDisable(!canManage);
        deleteButton.setDisable(!canManage);
        clearButton.setDisable(!canManage);

        nameField.setDisable(!canManage);
        rateField.setDisable(!canManage);
        termMonthsField.setDisable(!canManage);
        minAmountField.setDisable(!canManage);
        maxAmountField.setDisable(!canManage);

        if (descriptionArea != null) {
            descriptionArea.setDisable(!canManage);
        }

        if (allowReplenishmentCheckBox != null) {
            allowReplenishmentCheckBox.setDisable(!canManage);
        }
        if (allowPartialWithdrawalCheckBox != null) {
            allowPartialWithdrawalCheckBox.setDisable(!canManage);
        }
        if (capitalizationCheckBox != null) {
            capitalizationCheckBox.setDisable(!canManage);
        }
    }

    // ---------------------- Table setup ----------------------

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        termColumn.setCellValueFactory(new PropertyValueFactory<>("termMonths"));
        rateColumn.setCellValueFactory(new PropertyValueFactory<>("baseInterestRate"));
        minAmountColumn.setCellValueFactory(new PropertyValueFactory<>("minAmount"));
        maxAmountColumn.setCellValueFactory(new PropertyValueFactory<>("maxAmount"));

        replColumn.setCellValueFactory(new PropertyValueFactory<>("allowReplenishment"));
        partialWithdrawColumn.setCellValueFactory(new PropertyValueFactory<>("allowPartialWithdrawal"));
        capitalizationColumn.setCellValueFactory(new PropertyValueFactory<>("capitalization"));

        replColumn.setCellFactory(this::yesNoCell);
        partialWithdrawColumn.setCellFactory(this::yesNoCell);
        capitalizationColumn.setCellFactory(this::yesNoCell);

        rateColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.toPlainString());
            }
        });

        minAmountColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.toPlainString());
            }
        });

        maxAmountColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    return;
                }
                setText(item.toPlainString());
            }
        });
    }

    private TableCell<DepositProduct, Boolean> yesNoCell(TableColumn<DepositProduct, Boolean> col) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(Boolean.TRUE.equals(item) ? "Да" : "Нет");
            }
        };
    }

    private void setupSelectionListener() {
        productsTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldV, newV) -> {
                    selectedProduct = newV;
                    fillFormFromSelection();
                    updateButtonsState();
                });
    }

    // ---------------------- Load ----------------------

    private void loadAllProducts() {
        List<DepositProduct> list = depositProductService.getAllProducts();
        products.setAll(list);

        if (!products.isEmpty()) {
            productsTable.getSelectionModel().selectFirst();
            selectedProduct = productsTable.getSelectionModel().getSelectedItem();
            fillFormFromSelection();
        } else {
            selectedProduct = null;
        }

        updateButtonsState();
    }

    // ---------------------- Search ----------------------

    @FXML
    private void onFindByName() {
        try {
            String query = safe(nameSearchField.getText());

            List<DepositProduct> all = depositProductService.getAllProducts();

            List<DepositProduct> filtered;
            if (query.isBlank()) {
                filtered = all;
            } else {
                String q = query.toLowerCase();
                filtered = all.stream()
                        .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(q))
                        .collect(Collectors.toList());
            }

            products.setAll(filtered);

            if (!products.isEmpty()) {
                productsTable.getSelectionModel().selectFirst();
                selectedProduct = productsTable.getSelectionModel().getSelectedItem();
                fillFormFromSelection();
            } else {
                selectedProduct = null;
                clearForm();
            }

            updateButtonsState();

        } catch (Exception ex) {
            showError("Поиск", ex.toString());
        }
    }

    // ---------------------- Form actions ----------------------

    @FXML
    private void onNewProduct() {
        if (!uiAccessManager.canManageProducts(sessionContext.getCurrentUser())) {
            showInfo("Доступ", "Недостаточно прав для создания продукта.");
            return;
        }

        productsTable.getSelectionModel().clearSelection();
        selectedProduct = null;
        clearForm();
        updateButtonsState();
        nameField.requestFocus();
    }

    @FXML
    private void onSaveProduct() {
        if (!uiAccessManager.canManageProducts(sessionContext.getCurrentUser())) {
            showInfo("Доступ", "Недостаточно прав для сохранения продукта.");
            return;
        }

        try {
            String name = safe(nameField.getText());
            String description = descriptionArea != null ? safe(descriptionArea.getText()) : "";

            if (name.isBlank()) {
                showError("Продукт", "Название не может быть пустым.");
                return;
            }

            BigDecimal rate = parseDecimal(rateField.getText(), "% годовых", true);
            if (rate == null) {
                return;
            }

            Integer termMonths = parseInt(termMonthsField.getText(), "Срок (мес.)", true);
            if (termMonths == null) {
                return;
            }
            if (termMonths < 0) {
                showError("Срок (мес.)", "Срок не может быть отрицательным.");
                return;
            }

            BigDecimal minAmount = parseDecimal(minAmountField.getText(), "Мин. сумма", true);
            if (minAmount == null) {
                return;
            }

            BigDecimal maxAmount = parseDecimal(maxAmountField.getText(), "Макс. сумма", false);
            if (maxAmount != null && maxAmount.compareTo(minAmount) < 0) {
                showError("Макс. сумма", "Макс. сумма не может быть меньше минимальной.");
                return;
            }

            boolean allowRepl = allowReplenishmentCheckBox != null && allowReplenishmentCheckBox.isSelected();
            boolean allowPartial = allowPartialWithdrawalCheckBox != null && allowPartialWithdrawalCheckBox.isSelected();
            boolean cap = capitalizationCheckBox != null && capitalizationCheckBox.isSelected();

            if (selectedProduct == null) {
                DepositProduct p = new DepositProduct();
                p.setName(name);
                p.setDescription(description);
                p.setBaseInterestRate(rate);
                p.setTermMonths(termMonths);
                p.setMinAmount(minAmount);
                p.setMaxAmount(maxAmount);
                p.setAllowReplenishment(allowRepl);
                p.setAllowPartialWithdrawal(allowPartial);
                p.setCapitalization(cap);

                DepositProduct created = depositProductService.createProduct(p);
                reloadAndSelect(created.getId());

                showInfo("Продукт", "Продукт создан: " + safe(created.getName()));
            } else {
                DepositProduct updated = new DepositProduct();
                updated.setName(name);
                updated.setDescription(description);
                updated.setBaseInterestRate(rate);
                updated.setTermMonths(termMonths);
                updated.setMinAmount(minAmount);
                updated.setMaxAmount(maxAmount);
                updated.setAllowReplenishment(allowRepl);
                updated.setAllowPartialWithdrawal(allowPartial);
                updated.setCapitalization(cap);

                DepositProduct result =
                        depositProductService.updateProduct(selectedProduct.getId(), updated);
                reloadAndSelect(result.getId());

                showInfo("Продукт", "Продукт обновлён: " + safe(result.getName()));
            }

        } catch (InvalidOperationException | EntityNotFoundException ex) {
            showError("Продукт", ex.getMessage());
        } catch (Exception ex) {
            showError("Неожиданная ошибка", ex.toString());
        }
    }

    @FXML
    private void onDeleteProduct() {
        if (!uiAccessManager.canManageProducts(sessionContext.getCurrentUser())) {
            showInfo("Доступ", "Недостаточно прав для удаления продукта.");
            return;
        }

        if (selectedProduct == null || selectedProduct.getId() == null) {
            showInfo("Удаление", "Сначала выберите продукт.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление продукта");
        confirm.setHeaderText(null);
        confirm.setContentText("Удалить продукт \"" + safe(selectedProduct.getName()) + "\"?");

        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    depositProductService.deleteProduct(selectedProduct.getId());
                    loadAllProducts();
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
        List<DepositProduct> list = depositProductService.getAllProducts();
        products.setAll(list);

        selectedProduct = null;

        if (id != null) {
            for (DepositProduct p : products) {
                if (Objects.equals(p.getId(), id)) {
                    productsTable.getSelectionModel().select(p);
                    productsTable.scrollTo(p);
                    selectedProduct = p;
                    break;
                }
            }
        }

        if (selectedProduct == null && !products.isEmpty()) {
            productsTable.getSelectionModel().selectFirst();
            selectedProduct = productsTable.getSelectionModel().getSelectedItem();
        }

        fillFormFromSelection();
        updateButtonsState();
    }

    private void fillFormFromSelection() {
        if (selectedProduct == null) {
            clearForm();
            return;
        }

        nameField.setText(safe(selectedProduct.getName()));
        rateField.setText(selectedProduct.getBaseInterestRate() != null
                ? selectedProduct.getBaseInterestRate().toPlainString()
                : "");

        termMonthsField.setText(selectedProduct.getTermMonths() != null
                ? String.valueOf(selectedProduct.getTermMonths())
                : "");

        if (descriptionArea != null) {
            descriptionArea.setText(safe(selectedProduct.getDescription()));
        }

        minAmountField.setText(selectedProduct.getMinAmount() != null
                ? selectedProduct.getMinAmount().toPlainString()
                : "");

        maxAmountField.setText(selectedProduct.getMaxAmount() != null
                ? selectedProduct.getMaxAmount().toPlainString()
                : "");

        if (allowReplenishmentCheckBox != null) {
            allowReplenishmentCheckBox.setSelected(Boolean.TRUE.equals(selectedProduct.getAllowReplenishment()));
        }
        if (allowPartialWithdrawalCheckBox != null) {
            allowPartialWithdrawalCheckBox.setSelected(Boolean.TRUE.equals(selectedProduct.getAllowPartialWithdrawal()));
        }
        if (capitalizationCheckBox != null) {
            capitalizationCheckBox.setSelected(Boolean.TRUE.equals(selectedProduct.getCapitalization()));
        }
    }

    private void clearForm() {
        nameField.clear();
        rateField.clear();
        termMonthsField.clear();
        minAmountField.clear();
        maxAmountField.clear();

        if (descriptionArea != null) {
            descriptionArea.clear();
        }

        if (allowReplenishmentCheckBox != null) {
            allowReplenishmentCheckBox.setSelected(false);
        }
        if (allowPartialWithdrawalCheckBox != null) {
            allowPartialWithdrawalCheckBox.setSelected(false);
        }
        if (capitalizationCheckBox != null) {
            capitalizationCheckBox.setSelected(false);
        }
    }

    private void updateButtonsState() {
        User current = sessionContext.getCurrentUser();
        boolean canManage = uiAccessManager.canManageProducts(current);

        boolean hasSelection = selectedProduct != null && selectedProduct.getId() != null;

        deleteButton.setDisable(!canManage || !hasSelection);
        saveButton.setDisable(!canManage);
        newButton.setDisable(!canManage);
        clearButton.setDisable(!canManage);
    }

    private BigDecimal parseDecimal(String text, String fieldName, boolean required) {
        if (text == null || text.isBlank()) {
            if (required) {
                showError(fieldName, "Значение обязательно для заполнения.");
                return null;
            }
            return null;
        }

        try {
            BigDecimal v = new BigDecimal(text.trim().replace(',', '.'));
            if (v.compareTo(BigDecimal.ZERO) < 0) {
                showError(fieldName, "Значение не может быть отрицательным.");
                return null;
            }
            return v;
        } catch (Exception e) {
            showError(fieldName, "Неверный формат числа.");
            return null;
        }
    }

    private Integer parseInt(String text, String fieldName, boolean required) {
        if (text == null || text.isBlank()) {
            if (required) {
                showError(fieldName, "Значение обязательно для заполнения.");
                return null;
            }
            return null;
        }

        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            showError(fieldName, "Неверный формат числа.");
            return null;
        }
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
