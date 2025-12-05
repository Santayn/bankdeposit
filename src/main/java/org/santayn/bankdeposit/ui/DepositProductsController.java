package org.santayn.bankdeposit.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.DepositProduct;
import org.santayn.bankdeposit.service.DepositProductService;
import org.santayn.bankdeposit.service.EntityNotFoundException;
import org.santayn.bankdeposit.service.InvalidOperationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DepositProductsController {

    private final DepositProductService depositProductService;

    // Поиск
    @FXML
    private TextField nameSearchField;

    @FXML
    private Button findByNameButton;

    // Таблица
    @FXML
    private TableView<DepositProduct> productsTable;

    @FXML
    private TableColumn<DepositProduct, String> nameColumn;

    @FXML
    private TableColumn<DepositProduct, String> termColumn;

    @FXML
    private TableColumn<DepositProduct, String> rateColumn;

    @FXML
    private TableColumn<DepositProduct, String> minAmountColumn;

    @FXML
    private TableColumn<DepositProduct, String> maxAmountColumn;

    @FXML
    private TableColumn<DepositProduct, String> replColumn;

    @FXML
    private TableColumn<DepositProduct, String> partialWithdrawColumn;

    @FXML
    private TableColumn<DepositProduct, String> capitalizationColumn;

    // Форма
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

    // Кнопки формы
    @FXML
    private Button newButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button clearButton;

    private final ObservableList<DepositProduct> products = FXCollections.observableArrayList();

    private DepositProduct selectedProduct;

    @FXML
    public void initialize() {
        setupTable();
        setupSelection();
        loadAllProducts();
        clearForm();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        termColumn.setCellValueFactory(cell -> {
            Integer term = cell.getValue().getTermMonths();
            if (term == null) {
                return new SimpleStringProperty("");
            }
            if (term == 0) {
                return new SimpleStringProperty("Бессрочный");
            }
            return new SimpleStringProperty(term + " мес.");
        });

        rateColumn.setCellValueFactory(cell -> {
            BigDecimal rate = cell.getValue().getBaseInterestRate();
            return new SimpleStringProperty(rate != null ? rate.toPlainString() : "");
        });

        minAmountColumn.setCellValueFactory(cell -> {
            BigDecimal min = cell.getValue().getMinAmount();
            return new SimpleStringProperty(min != null ? min.toPlainString() : "");
        });

        maxAmountColumn.setCellValueFactory(cell -> {
            BigDecimal max = cell.getValue().getMaxAmount();
            return new SimpleStringProperty(max != null ? max.toPlainString() : "");
        });

        replColumn.setCellValueFactory(cell -> {
            Boolean v = cell.getValue().getAllowReplenishment();
            return new SimpleStringProperty(Boolean.TRUE.equals(v) ? "Да" : "Нет");
        });

        partialWithdrawColumn.setCellValueFactory(cell -> {
            Boolean v = cell.getValue().getAllowPartialWithdrawal();
            return new SimpleStringProperty(Boolean.TRUE.equals(v) ? "Да" : "Нет");
        });

        capitalizationColumn.setCellValueFactory(cell -> {
            Boolean v = cell.getValue().getCapitalization();
            return new SimpleStringProperty(Boolean.TRUE.equals(v) ? "Да" : "Нет");
        });

        productsTable.setItems(products);
    }

    private void setupSelection() {
        productsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedProduct = newV;
            fillFormFromSelected();
            updateButtonsState();
        });
        updateButtonsState();
    }

    private void updateButtonsState() {
        boolean has = selectedProduct != null;
        deleteButton.setDisable(!has);
    }

    private void loadAllProducts() {
        List<DepositProduct> list = depositProductService.getAllProducts();
        products.setAll(list);
        if (!products.isEmpty()) {
            productsTable.getSelectionModel().selectFirst();
        }
    }

    private void fillFormFromSelected() {
        if (selectedProduct == null) {
            return;
        }

        nameField.setText(selectedProduct.getName());
        rateField.setText(selectedProduct.getBaseInterestRate() != null
                ? selectedProduct.getBaseInterestRate().toPlainString()
                : "");
        termMonthsField.setText(selectedProduct.getTermMonths() != null
                ? selectedProduct.getTermMonths().toString()
                : "");
        descriptionArea.setText(selectedProduct.getDescription() != null
                ? selectedProduct.getDescription()
                : "");
        minAmountField.setText(selectedProduct.getMinAmount() != null
                ? selectedProduct.getMinAmount().toPlainString()
                : "");
        maxAmountField.setText(selectedProduct.getMaxAmount() != null
                ? selectedProduct.getMaxAmount().toPlainString()
                : "");

        allowReplenishmentCheckBox.setSelected(Boolean.TRUE.equals(selectedProduct.getAllowReplenishment()));
        allowPartialWithdrawalCheckBox.setSelected(Boolean.TRUE.equals(selectedProduct.getAllowPartialWithdrawal()));
        capitalizationCheckBox.setSelected(Boolean.TRUE.equals(selectedProduct.getCapitalization()));
    }

    // ------------------- Actions -------------------

    @FXML
    private void onFindByName() {
        String query = nameSearchField.getText();
        if (query == null || query.isBlank()) {
            loadAllProducts();
            return;
        }

        List<DepositProduct> found = Collections.singletonList(depositProductService.findByName(query.trim()));
        products.setAll(found);

        if (!products.isEmpty()) {
            productsTable.getSelectionModel().selectFirst();
        } else {
            selectedProduct = null;
            clearForm();
        }

        updateButtonsState();
    }

    @FXML
    private void onNewProduct() {
        selectedProduct = null;
        clearForm();
        productsTable.getSelectionModel().clearSelection();
        updateButtonsState();
    }

    @FXML
    private void onSaveProduct() {
        try {
            DepositProduct prepared = readProductFromForm();

            DepositProduct saved;
            if (selectedProduct == null || selectedProduct.getId() == null) {
                saved = depositProductService.createProduct(prepared);
            } else {
                saved = depositProductService.updateProduct(selectedProduct.getId(), prepared);
            }

            reloadAndSelect(saved.getId());
            showInfo("Сохранение продукта", "Данные успешно сохранены.");

        } catch (InvalidOperationException | EntityNotFoundException ex) {
            showError("Сохранение продукта", ex.getMessage());
        } catch (Exception ex) {
            showError("Неожиданная ошибка", ex.toString());
        }
    }

    @FXML
    private void onDeleteProduct() {
        if (selectedProduct == null || selectedProduct.getId() == null) {
            showError("Удаление продукта", "Сначала выберите продукт.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление продукта");
        confirm.setHeaderText(null);
        confirm.setContentText("Удалить выбранный депозитный продукт?");

        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    depositProductService.deleteProduct(selectedProduct.getId());
                    selectedProduct = null;
                    loadAllProducts();
                    clearForm();
                    updateButtonsState();
                } catch (InvalidOperationException | EntityNotFoundException ex) {
                    showError("Удаление продукта", ex.getMessage());
                } catch (Exception ex) {
                    showError("Неожиданная ошибка", ex.toString());
                }
            }
        });
    }

    @FXML
    private void onClearForm() {
        selectedProduct = null;
        clearForm();
        productsTable.getSelectionModel().clearSelection();
        updateButtonsState();
    }

    // ------------------- Helpers -------------------

    private DepositProduct readProductFromForm() {
        String name = safe(nameField.getText());
        String rateText = safe(rateField.getText());
        String termText = safe(termMonthsField.getText());
        String description = descriptionArea.getText();

        String minText = safe(minAmountField.getText());
        String maxText = safe(maxAmountField.getText());

        if (name.isBlank()) {
            throw new InvalidOperationException("Название продукта не может быть пустым");
        }
        if (rateText.isBlank()) {
            throw new InvalidOperationException("Укажите процентную ставку");
        }

        BigDecimal rate;
        try {
            rate = new BigDecimal(rateText.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new InvalidOperationException("Неверный формат ставки");
        }

        Integer termMonths = null;
        if (!termText.isBlank()) {
            try {
                termMonths = Integer.parseInt(termText);
                if (termMonths < 0) {
                    throw new InvalidOperationException("Срок не может быть отрицательным");
                }
            } catch (NumberFormatException ex) {
                throw new InvalidOperationException("Неверный формат срока");
            }
        }

        BigDecimal minAmount = null;
        if (!minText.isBlank()) {
            try {
                minAmount = new BigDecimal(minText.replace(',', '.'));
            } catch (NumberFormatException ex) {
                throw new InvalidOperationException("Неверный формат минимальной суммы");
            }
        }

        BigDecimal maxAmount = null;
        if (!maxText.isBlank()) {
            try {
                maxAmount = new BigDecimal(maxText.replace(',', '.'));
            } catch (NumberFormatException ex) {
                throw new InvalidOperationException("Неверный формат максимальной суммы");
            }
        }

        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("Минимальная сумма не может быть отрицательной");
        }
        if (maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("Максимальная сумма не может быть отрицательной");
        }
        if (minAmount != null && maxAmount != null && maxAmount.compareTo(minAmount) < 0) {
            throw new InvalidOperationException("Максимальная сумма меньше минимальной");
        }

        DepositProduct p = new DepositProduct();
        p.setId(null);
        p.setName(name);
        p.setBaseInterestRate(rate);
        p.setTermMonths(termMonths != null ? termMonths : 0);
        p.setDescription(description);

        p.setMinAmount(minAmount);
        p.setMaxAmount(maxAmount);

        p.setAllowReplenishment(allowReplenishmentCheckBox.isSelected());
        p.setAllowPartialWithdrawal(allowPartialWithdrawalCheckBox.isSelected());
        p.setCapitalization(capitalizationCheckBox.isSelected());

        return p;
    }

    private void reloadAndSelect(Long id) {
        List<DepositProduct> list = depositProductService.getAllProducts();
        products.setAll(list);

        if (id != null) {
            for (DepositProduct p : products) {
                if (id.equals(p.getId())) {
                    productsTable.getSelectionModel().select(p);
                    productsTable.scrollTo(p);
                    selectedProduct = p;
                    fillFormFromSelected();
                    break;
                }
            }
        }

        updateButtonsState();
    }

    private void clearForm() {
        nameField.clear();
        rateField.clear();
        termMonthsField.clear();
        descriptionArea.clear();
        minAmountField.clear();
        maxAmountField.clear();

        allowReplenishmentCheckBox.setSelected(false);
        allowPartialWithdrawalCheckBox.setSelected(false);
        capitalizationCheckBox.setSelected(false);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
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
