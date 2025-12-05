package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.santayn.bankdeposit.models.DepositOperationType;
import org.santayn.bankdeposit.models.DepositProduct;
import org.santayn.bankdeposit.repository.CustomerRepository;
import org.santayn.bankdeposit.repository.DepositContractRepository;
import org.santayn.bankdeposit.repository.DepositProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Сервис управления договорами вкладов.
 */
@Service
@RequiredArgsConstructor
public class DepositContractService {

    private final DepositContractRepository depositContractRepository;
    private final DepositProductRepository depositProductRepository;
    private final CustomerRepository customerRepository;
    private final DepositOperationService depositOperationService;

    @Transactional(readOnly = true)
    public List<DepositContract> getAllContracts() {
        return depositContractRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DepositContract> getContractsByCustomer(Long customerId) {
        if (customerId == null) {
            throw new InvalidOperationException("Не указан клиент");
        }
        return depositContractRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<DepositContract> getActiveContracts() {
        return depositContractRepository.findByStatus(DepositContractStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public DepositContract getContractById(Long id) {
        if (id == null) {
            throw new InvalidOperationException("Не указан идентификатор договора");
        }
        return depositContractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Договор с id=" + id + " не найден"));
    }

    /**
     * Открытие нового вклада.
     */
    @Transactional
    public DepositContract openContract(
            Long customerId,
            Long productId,
            BigDecimal initialAmount,
            LocalDate openDate
    ) {
        if (customerId == null) {
            throw new InvalidOperationException("Не указан клиент");
        }
        if (productId == null) {
            throw new InvalidOperationException("Не указан депозитный продукт");
        }
        if (initialAmount == null || initialAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Начальная сумма должна быть больше нуля");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент с id=" + customerId + " не найден"));

        DepositProduct product = depositProductRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Продукт с id=" + productId + " не найден"));

        validateAmountAgainstProduct(initialAmount, product);

        DepositContract contract = new DepositContract();
        contract.setCustomer(customer);
        contract.setProduct(product);

        contract.setOpenDate(openDate != null ? openDate : LocalDate.now());
        contract.setStatus(DepositContractStatus.OPEN);

        contract.setInitialAmount(normalizeMoney(initialAmount));
        contract.setCurrentBalance(normalizeMoney(initialAmount));

        BigDecimal rate = product.getBaseInterestRate() != null
                ? product.getBaseInterestRate()
                : BigDecimal.ZERO;
        contract.setInterestRate(rate);

        contract.setContractNumber(generateContractNumber());

        DepositContract saved = depositContractRepository.save(contract);

        depositOperationService.createOperation(
                saved,
                DepositOperationType.OPENING,
                normalizeMoney(initialAmount),
                "Открытие вклада",
                LocalDateTime.now()
        );

        return saved;
    }

    /**
     * Пополнение вклада.
     */
    @Transactional
    public DepositContract deposit(
            Long contractId,
            BigDecimal amount,
            String description
    ) {
        DepositContract contract = getContractById(contractId);

        ensureOpen(contract);

        DepositProduct product = contract.getProduct();
        if (product != null && Boolean.FALSE.equals(product.getAllowReplenishment())) {
            throw new InvalidOperationException("Данный продукт не допускает пополнение");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Сумма пополнения должна быть больше нуля");
        }

        BigDecimal newBalance = normalizeMoney(contract.getCurrentBalance()).add(normalizeMoney(amount));

        if (product != null && product.getMaxAmount() != null) {
            if (newBalance.compareTo(product.getMaxAmount()) > 0) {
                throw new InvalidOperationException("Превышен максимальный лимит суммы по продукту");
            }
        }

        contract.setCurrentBalance(newBalance);

        DepositContract saved = depositContractRepository.save(contract);

        depositOperationService.createOperation(
                saved,
                DepositOperationType.DEPOSIT,
                normalizeMoney(amount),
                description != null && !description.isBlank() ? description.trim() : "Пополнение вклада",
                LocalDateTime.now()
        );

        return saved;
    }

    /**
     * Снятие средств.
     */
    @Transactional
    public DepositContract withdraw(
            Long contractId,
            BigDecimal amount,
            String description
    ) {
        DepositContract contract = getContractById(contractId);

        ensureOpen(contract);

        DepositProduct product = contract.getProduct();
        if (product != null && Boolean.FALSE.equals(product.getAllowPartialWithdrawal())) {
            throw new InvalidOperationException("Данный продукт не допускает частичное снятие");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Сумма снятия должна быть больше нуля");
        }

        BigDecimal current = normalizeMoney(contract.getCurrentBalance());
        BigDecimal normalizedAmount = normalizeMoney(amount);

        if (current.compareTo(normalizedAmount) < 0) {
            throw new InvalidOperationException("Недостаточно средств для снятия");
        }

        BigDecimal newBalance = current.subtract(normalizedAmount);

        if (product != null && product.getMinAmount() != null) {
            if (newBalance.compareTo(product.getMinAmount()) < 0) {
                throw new InvalidOperationException("После снятия остаток будет меньше минимальной суммы по продукту");
            }
        }

        contract.setCurrentBalance(newBalance);

        DepositContract saved = depositContractRepository.save(contract);

        depositOperationService.createOperation(
                saved,
                DepositOperationType.WITHDRAWAL,
                normalizedAmount,
                description != null && !description.isBlank() ? description.trim() : "Снятие средств",
                LocalDateTime.now()
        );

        return saved;
    }

    /**
     * Ручное начисление процентов (если пользователь вводит сумму).
     */
    @Transactional
    public DepositContract accrueInterestManual(
            Long contractId,
            BigDecimal amount,
            String description,
            LocalDateTime dateTime
    ) {
        DepositContract contract = getContractById(contractId);

        ensureOpen(contract);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Сумма процентов должна быть больше нуля");
        }

        BigDecimal normalized = normalizeMoney(amount);

        contract.setCurrentBalance(normalizeMoney(contract.getCurrentBalance()).add(normalized));
        DepositContract saved = depositContractRepository.save(contract);

        depositOperationService.createOperation(
                saved,
                DepositOperationType.INTEREST_ACCRUAL,
                normalized,
                description != null && !description.isBlank() ? description.trim() : "Начисление процентов",
                dateTime != null ? dateTime : LocalDateTime.now()
        );

        return saved;
    }

    /**
     * Закрытие вклада.
     */
    @Transactional
    public DepositContract closeContract(Long contractId, LocalDate closeDate) {
        DepositContract contract = getContractById(contractId);

        ensureOpen(contract);

        contract.setStatus(DepositContractStatus.CLOSED);
        contract.setCloseDate(closeDate != null ? closeDate : LocalDate.now());

        DepositContract saved = depositContractRepository.save(contract);

        BigDecimal остаток = normalizeMoney(saved.getCurrentBalance());

        if (остаток.compareTo(BigDecimal.ZERO) > 0) {
            // фиксируем закрытие как отдельную операцию "CLOSING" на сумму остатка
            depositOperationService.createOperation(
                    saved,
                    DepositOperationType.CLOSING,
                    остаток,
                    "Закрытие вклада",
                    LocalDateTime.now()
            );
        } else {
            depositOperationService.createOperation(
                    saved,
                    DepositOperationType.CLOSING,
                    BigDecimal.ONE,
                    "Закрытие вклада (остаток 0)",
                    LocalDateTime.now()
            );
        }

        return saved;
    }

    // -------------------- Валидации и утилиты --------------------

    private void ensureOpen(DepositContract contract) {
        if (contract.getStatus() != DepositContractStatus.OPEN) {
            throw new InvalidOperationException("Операция недоступна. Договор не открыт");
        }
    }

    private void validateAmountAgainstProduct(BigDecimal amount, DepositProduct product) {
        BigDecimal normalized = normalizeMoney(amount);

        if (product.getMinAmount() != null && normalized.compareTo(product.getMinAmount()) < 0) {
            throw new InvalidOperationException("Сумма меньше минимальной по продукту");
        }

        if (product.getMaxAmount() != null && normalized.compareTo(product.getMaxAmount()) > 0) {
            throw new InvalidOperationException("Сумма больше максимальной по продукту");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private String generateContractNumber() {
        // Простой генератор номера договора для учебного проекта
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "DC-" + uid;
    }
}
