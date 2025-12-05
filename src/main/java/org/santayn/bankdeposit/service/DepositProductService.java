package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.DepositProduct;
import org.santayn.bankdeposit.repository.DepositProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с депозитными продуктами.
 * Отвечает за валидацию и операции CRUD.
 */
@Service
@RequiredArgsConstructor
public class DepositProductService {

    private final DepositProductRepository depositProductRepository;

    /**
     * Возвращает все депозитные продукты.
     */
    public List<DepositProduct> getAllProducts() {
        return depositProductRepository.findAll();
    }

    /**
     * Возвращает продукт по идентификатору.
     *
     * @param id идентификатор продукта
     * @return найденный продукт
     */
    public DepositProduct getProductById(Long id) {
        if (id == null) {
            throw new InvalidOperationException("Идентификатор продукта не указан");
        }

        return depositProductRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Депозитный продукт с id=" + id + " не найден"
                ));
    }

    /**
     * Создаёт новый депозитный продукт.
     *
     * @param product данные продукта
     * @return созданный продукт
     */
    public DepositProduct createProduct(DepositProduct product) {
        if (product == null) {
            throw new InvalidOperationException("Данные продукта не переданы");
        }

        validateProduct(product, true);

        product.setId(null);
        return depositProductRepository.save(product);
    }

    /**
     * Обновляет существующий депозитный продукт.
     *
     * @param id      идентификатор продукта
     * @param updated обновлённые данные
     * @return обновлённый продукт
     */
    public DepositProduct updateProduct(Long id, DepositProduct updated) {
        if (id == null) {
            throw new InvalidOperationException("Идентификатор продукта не указан");
        }
        if (updated == null) {
            throw new InvalidOperationException("Данные продукта не переданы");
        }

        DepositProduct existing = depositProductRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Депозитный продукт с id=" + id + " не найден"
                ));

        DepositProduct toValidate = new DepositProduct();
        toValidate.setId(existing.getId());
        toValidate.setName(updated.getName());
        toValidate.setDescription(updated.getDescription());
        toValidate.setBaseInterestRate(updated.getBaseInterestRate());
        toValidate.setTermMonths(updated.getTermMonths());
        toValidate.setMinAmount(updated.getMinAmount());
        toValidate.setMaxAmount(updated.getMaxAmount());
        toValidate.setAllowReplenishment(updated.getAllowReplenishment());
        toValidate.setAllowPartialWithdrawal(updated.getAllowPartialWithdrawal());
        toValidate.setCapitalization(updated.getCapitalization());

        validateProduct(toValidate, false);

        existing.setName(toValidate.getName());
        existing.setDescription(toValidate.getDescription());
        existing.setBaseInterestRate(toValidate.getBaseInterestRate());
        existing.setTermMonths(toValidate.getTermMonths());
        existing.setMinAmount(toValidate.getMinAmount());
        existing.setMaxAmount(toValidate.getMaxAmount());
        existing.setAllowReplenishment(toValidate.getAllowReplenishment());
        existing.setAllowPartialWithdrawal(toValidate.getAllowPartialWithdrawal());
        existing.setCapitalization(toValidate.getCapitalization());

        return depositProductRepository.save(existing);
    }

    /**
     * Удаляет продукт по идентификатору.
     */
    public void deleteProduct(Long id) {
        if (id == null) {
            throw new InvalidOperationException("Идентификатор продукта не указан");
        }

        if (!depositProductRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    "Депозитный продукт с id=" + id + " не найден"
            );
        }

        depositProductRepository.deleteById(id);
    }

    /**
     * Поиск продукта по имени.
     *
     * @param name имя продукта
     * @return найденный продукт
     */
    public DepositProduct findByName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidOperationException("Название продукта не может быть пустым");
        }

        Optional<DepositProduct> optional =
                depositProductRepository.findByName(name.trim());

        return optional.orElseThrow(() ->
                new EntityNotFoundException(
                        "Депозитный продукт \"" + name + "\" не найден"
                )
        );
    }

    /**
     * Базовая валидация данных депозитного продукта.
     *
     * @param product  продукт
     * @param isCreate true, если создаём новый (для проверки уникальности имени)
     */
    private void validateProduct(DepositProduct product, boolean isCreate) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new InvalidOperationException("Название депозитного продукта не может быть пустым");
        }

        String normalizedName = product.getName().trim();
        product.setName(normalizedName);

        // Проверяем уникальность имени
        Optional<DepositProduct> existing =
                depositProductRepository.findByName(normalizedName);
        if (existing.isPresent()) {
            if (isCreate || !existing.get().getId().equals(product.getId())) {
                throw new InvalidOperationException(
                        "Депозитный продукт с названием \"" + normalizedName + "\" уже существует"
                );
            }
        }

        BigDecimal rate = product.getBaseInterestRate();
        if (rate == null) {
            throw new InvalidOperationException("Процентная ставка не указана");
        }
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Процентная ставка должна быть положительной");
        }
        // чисто для реализма: ограничим, скажем, 50% годовых
        if (rate.compareTo(new BigDecimal("50")) > 0) {
            throw new InvalidOperationException("Процентная ставка выглядит слишком большой");
        }

        Integer termMonths = product.getTermMonths();
        if (termMonths != null && termMonths < 0) {
            throw new InvalidOperationException("Срок вклада (в месяцах) не может быть отрицательным");
        }

        BigDecimal minAmount = product.getMinAmount();
        BigDecimal maxAmount = product.getMaxAmount();

        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("Минимальная сумма не может быть отрицательной");
        }
        if (maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("Максимальная сумма не может быть отрицательной");
        }
        if (minAmount != null && maxAmount != null
                && maxAmount.compareTo(minAmount) < 0) {
            throw new InvalidOperationException(
                    "Максимальная сумма не может быть меньше минимальной"
            );
        }
    }
}
