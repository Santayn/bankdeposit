package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.DepositOperationType;
import org.santayn.bankdeposit.models.DepositProduct;
import org.santayn.bankdeposit.repository.DepositContractRepository;
import org.santayn.bankdeposit.repository.DepositOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис операций по вкладам.
 *
 * Отвечает за:
 * - сохранение операций
 * - выдачу списка операций по договору
 * - пополнение
 * - снятие
 * - начисление процентов
 */
@Service
@RequiredArgsConstructor
public class DepositOperationService {

    private final DepositOperationRepository depositOperationRepository;
    private final DepositContractRepository depositContractRepository;

    @Transactional(readOnly = true)
    public List<DepositOperation> getOperationsByContract(Long contractId) {
        if (contractId == null) {
            throw new InvalidOperationException("Не указан идентификатор договора");
        }

        List<DepositOperation> ops = depositOperationRepository.findByContractIdOrderByOperationDateTime(contractId);

        initializeOperationsForUi(ops);

        return ops;
    }

    @Transactional
    public DepositOperation createOperation(
            DepositContract contract,
            DepositOperationType type,
            BigDecimal amount,
            String description,
            LocalDateTime dateTime
    ) {
        if (contract == null) {
            throw new InvalidOperationException("Не указан договор для операции");
        }
        if (type == null) {
            throw new InvalidOperationException("Не указан тип операции");
        }
        if (amount == null) {
            throw new InvalidOperationException("Не указана сумма операции");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Сумма операции должна быть больше нуля");
        }

        BigDecimal normalizedAmount = normalizeMoney(amount);

        DepositOperation op = new DepositOperation();
        op.setContract(contract);
        op.setType(type);
        op.setAmount(normalizedAmount);
        op.setDescription(description != null ? description.trim() : null);
        op.setOperationDateTime(dateTime != null ? dateTime : LocalDateTime.now());

        DepositOperation saved = depositOperationRepository.save(op);

        initializeOperationForUi(saved);

        return saved;
    }

    /**
     * Внутренний удобный вариант, когда у нас есть только id договора.
     */
    @Transactional
    public DepositOperation createSystemOperation(
            Long contractId,
            DepositOperationType type,
            BigDecimal amount,
            String description
    ) {
        DepositContract contract = getOpenContractOrThrow(contractId, "Системная операция");
        return createOperation(contract, type, amount, description, LocalDateTime.now());
    }

    @Transactional
    public void deposit(Long contractId, BigDecimal amount) {
        DepositContract contract = getOpenContractOrThrow(contractId, "Пополнение");
        validateAmount(amount, "Пополнение");

        DepositProduct product = contract.getProduct();
        if (product != null && Boolean.FALSE.equals(product.getAllowReplenishment())) {
            throw new InvalidOperationException("Пополнение запрещено условиями продукта");
        }

        BigDecimal normalizedAmount = normalizeMoney(amount);

        BigDecimal newBalance = safeBalance(contract).add(normalizedAmount);
        contract.setCurrentBalance(newBalance);

        depositContractRepository.save(contract);

        createOperation(
                contract,
                DepositOperationType.DEPOSIT,
                normalizedAmount,
                "Пополнение вклада",
                LocalDateTime.now()
        );
    }

    @Transactional
    public void withdraw(Long contractId, BigDecimal amount) {
        DepositContract contract = getOpenContractOrThrow(contractId, "Снятие");
        validateAmount(amount, "Снятие");

        DepositProduct product = contract.getProduct();
        if (product != null && Boolean.FALSE.equals(product.getAllowPartialWithdrawal())) {
            throw new InvalidOperationException("Частичное снятие запрещено условиями продукта");
        }

        BigDecimal normalizedAmount = normalizeMoney(amount);

        BigDecimal current = safeBalance(contract);
        if (current.compareTo(normalizedAmount) < 0) {
            throw new InvalidOperationException("Недостаточно средств на вкладе");
        }

        BigDecimal newBalance = current.subtract(normalizedAmount);
        contract.setCurrentBalance(newBalance);

        depositContractRepository.save(contract);

        createOperation(
                contract,
                DepositOperationType.WITHDRAWAL,
                normalizedAmount,
                "Снятие средств",
                LocalDateTime.now()
        );
    }

    /**
     * Начисление процентов вручную.
     */
    @Transactional
    public void accrueInterest(Long contractId, BigDecimal amount) {
        DepositContract contract = getOpenContractOrThrow(contractId, "Начисление процентов");
        validateAmount(amount, "Начисление процентов");

        BigDecimal normalizedAmount = normalizeMoney(amount);

        BigDecimal newBalance = safeBalance(contract).add(normalizedAmount);
        contract.setCurrentBalance(newBalance);

        depositContractRepository.save(contract);

        createOperation(
                contract,
                DepositOperationType.INTEREST_ACCRUAL,
                normalizedAmount,
                "Начисление процентов",
                LocalDateTime.now()
        );
    }

    // ---------------------- Private helpers ----------------------

    private DepositContract getOpenContractOrThrow(Long contractId, String action) {
        if (contractId == null) {
            throw new InvalidOperationException(action + ": не указан id договора");
        }

        DepositContract contract = depositContractRepository.findById(contractId)
                .orElseThrow(() ->
                        new EntityNotFoundException(action + ": договор с id=" + contractId + " не найден")
                );

        if (contract.getStatus() != DepositContractStatus.OPEN) {
            throw new InvalidOperationException(action + ": операция доступна только для активных вкладов");
        }

        // Инициализация связей заранее
        initializeContractForUi(contract);

        return contract;
    }

    private void validateAmount(BigDecimal amount, String action) {
        if (amount == null) {
            throw new InvalidOperationException(action + ": не указана сумма");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException(action + ": сумма должна быть больше нуля");
        }
    }

    private BigDecimal safeBalance(DepositContract contract) {
        BigDecimal balance = contract.getCurrentBalance();
        if (balance == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return balance.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Для JavaFX:
     * при отдаче операций подгружаем contract/customer/product,
     * чтобы ListView/TableView/SelectionModel не падали на lazy proxy.
     */
    private void initializeOperationsForUi(List<DepositOperation> ops) {
        if (ops == null) {
            return;
        }
        for (DepositOperation op : ops) {
            initializeOperationForUi(op);
        }
    }

    private void initializeOperationForUi(DepositOperation op) {
        if (op == null) {
            return;
        }
        DepositContract contract = op.getContract();
        initializeContractForUi(contract);
    }

    private void initializeContractForUi(DepositContract contract) {
        if (contract == null) {
            return;
        }

        contract.getId();
        contract.getStatus();
        contract.getOpenDate();
        contract.getCloseDate();
        contract.getCurrentBalance();

        Customer customer = contract.getCustomer();
        if (customer != null) {
            customer.getId();
            customer.getLastName();
            customer.getFirstName();
            customer.getMiddleName();
        }

        DepositProduct product = contract.getProduct();
        if (product != null) {
            product.getId();
            product.getName();
            product.getAllowReplenishment();
            product.getAllowPartialWithdrawal();
            product.getBaseInterestRate();
        }
    }
}
