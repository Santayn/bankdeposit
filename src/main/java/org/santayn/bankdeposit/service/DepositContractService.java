package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.DepositOperationType;
import org.santayn.bankdeposit.models.DepositProduct;

import org.santayn.bankdeposit.repository.CustomerRepository;
import org.santayn.bankdeposit.repository.DepositContractRepository;
import org.santayn.bankdeposit.repository.DepositOperationRepository;
import org.santayn.bankdeposit.repository.DepositProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для работы с депозитными договорами и операциями.
 */
@Service
@RequiredArgsConstructor
public class DepositContractService {

    private final CustomerRepository customerRepository;
    private final DepositProductRepository depositProductRepository;
    private final DepositContractRepository depositContractRepository;
    private DepositOperationRepository depositOperationRepository;

    /**
     * Возвращает все договоры.
     */
    public List<DepositContract> getAllContracts() {
        return depositContractRepository.findAll();
    }

    /**
     * Возвращает договор по идентификатору или выбрасывает исключение.
     */
    public DepositContract getContractById(Long id) {
        return depositContractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Договор вклада с id=" + id + " не найден"));
    }

    /**
     * Возвращает список договоров клиента.
     */
    public List<DepositContract> getContractsByCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент с id=" + customerId + " не найден"));
        return depositContractRepository.findByCustomer(customer);
    }

    /**
     * Открывает новый вклад (создаёт договор и операцию OPENING).
     *
     * @param customerId    идентификатор клиента
     * @param productId     идентификатор депозитного продукта
     * @param initialAmount сумма первоначального взноса
     * @param openDate      дата открытия вклада (если null, берётся текущая дата)
     * @return созданный договор
     */
    @Transactional
    public DepositContract openContract(Long customerId,
                                        Long productId,
                                        BigDecimal initialAmount,
                                        LocalDate openDate) {

        if (initialAmount == null || initialAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Сумма первоначального взноса должна быть положительной");
        }

        if (openDate == null) {
            openDate = LocalDate.now();
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент с id=" + customerId + " не найден"));

        DepositProduct product = depositProductRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Депозитный продукт с id=" + productId + " не найден"));

        if (product.getMinAmount() != null && initialAmount.compareTo(product.getMinAmount()) < 0) {
            throw new InvalidOperationException("Сумма меньше минимальной для данного продукта");
        }
        if (product.getMaxAmount() != null && initialAmount.compareTo(product.getMaxAmount()) > 0) {
            throw new InvalidOperationException("Сумма больше максимальной для данного продукта");
        }

        String contractNumber = generateContractNumber();

        DepositContract contract = DepositContract.builder()
                .contractNumber(contractNumber)
                .customer(customer)
                .product(product)
                .openDate(openDate)
                .closeDate(null)
                .initialAmount(initialAmount)
                .currentBalance(initialAmount)
                .interestRate(product.getBaseInterestRate())
                .status(DepositContractStatus.OPEN)
                .build();

        contract = depositContractRepository.save(contract);

        DepositOperation openingOperation = DepositOperation.builder()
                .contract(contract)
                .operationDateTime(LocalDateTime.now())
                .type(DepositOperationType.OPENING)
                .amount(initialAmount)
                .description("Открытие вклада")
                .build();

        depositOperationRepository.save(openingOperation);

        return contract;
    }

    /**
     * Пополнение вклада.
     */
    @Transactional
    public DepositContract deposit(Long contractId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Сумма пополнения должна быть положительной");
        }

        DepositContract contract = getContractById(contractId);

        if (contract.getStatus() != DepositContractStatus.OPEN) {
            throw new InvalidOperationException("Пополнение возможно только для открытых вкладов");
        }

        DepositProduct product = contract.getProduct();
        if (Boolean.FALSE.equals(product.getAllowReplenishment())) {
            throw new InvalidOperationException("Для данного продукта пополнение не разрешено");
        }

        BigDecimal newBalance = contract.getCurrentBalance().add(amount);
        contract.setCurrentBalance(newBalance);
        contract = depositContractRepository.save(contract);

        DepositOperation operation = DepositOperation.builder()
                .contract(contract)
                .operationDateTime(LocalDateTime.now())
                .type(DepositOperationType.DEPOSIT)
                .amount(amount)
                .description("Пополнение вклада")
                .build();

        depositOperationRepository.save(operation);

        return contract;
    }

    /**
     * Снятие средств с вклада.
     */
    @Transactional
    public DepositContract withdraw(Long contractId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Сумма снятия должна быть положительной");
        }

        DepositContract contract = getContractById(contractId);

        if (contract.getStatus() != DepositContractStatus.OPEN) {
            throw new InvalidOperationException("Снятие возможно только для открытых вкладов");
        }

        DepositProduct product = contract.getProduct();
        if (Boolean.FALSE.equals(product.getAllowPartialWithdrawal())) {
            throw new InvalidOperationException("Для данного продукта частичное снятие не разрешено");
        }

        if (contract.getCurrentBalance().compareTo(amount) < 0) {
            throw new InvalidOperationException("Недостаточно средств на вкладе");
        }

        BigDecimal newBalance = contract.getCurrentBalance().subtract(amount);
        contract.setCurrentBalance(newBalance);
        contract = depositContractRepository.save(contract);

        DepositOperation operation = DepositOperation.builder()
                .contract(contract)
                .operationDateTime(LocalDateTime.now())
                .type(DepositOperationType.WITHDRAWAL)
                .amount(amount)
                .description("Снятие средств с вклада")
                .build();

        depositOperationRepository.save(operation);

        return contract;
    }

    /**
     * Начисление процентов по вкладу.
     * Здесь пока упрощённо: просто увеличиваем текущий баланс на указанную сумму.
     */
    @Transactional
    public DepositContract accrueInterest(Long contractId, BigDecimal interestAmount) {
        if (interestAmount == null || interestAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Сумма начисленных процентов должна быть положительной");
        }

        DepositContract contract = getContractById(contractId);

        if (contract.getStatus() != DepositContractStatus.OPEN) {
            throw new InvalidOperationException("Начисление процентов возможно только для открытых вкладов");
        }

        BigDecimal newBalance = contract.getCurrentBalance().add(interestAmount);
        contract.setCurrentBalance(newBalance);
        contract = depositContractRepository.save(contract);

        DepositOperation operation = DepositOperation.builder()
                .contract(contract)
                .operationDateTime(LocalDateTime.now())
                .type(DepositOperationType.INTEREST_ACCRUAL)
                .amount(interestAmount)
                .description("Начисление процентов")
                .build();

        depositOperationRepository.save(operation);

        return contract;
    }

    /**
     * Закрытие вклада.
     * Фиксируем операцию CLOSING, обнуляем баланс и проставляем дату закрытия.
     */
    @Transactional
    public DepositContract closeContract(Long contractId) {
        DepositContract contract = getContractById(contractId);

        if (contract.getStatus() != DepositContractStatus.OPEN) {
            throw new InvalidOperationException("Закрыть можно только открытый вклад");
        }

        BigDecimal payoutAmount = contract.getCurrentBalance();

        contract.setStatus(DepositContractStatus.CLOSED);
        contract.setCloseDate(LocalDate.now());
        contract.setCurrentBalance(BigDecimal.ZERO);
        contract = depositContractRepository.save(contract);

        DepositOperation closingOperation = DepositOperation.builder()
                .contract(contract)
                .operationDateTime(LocalDateTime.now())
                .type(DepositOperationType.CLOSING)
                .amount(payoutAmount)
                .description("Закрытие вклада и выплата средств клиенту")
                .build();

        depositOperationRepository.save(closingOperation);

        return contract;
    }

    /**
     * Список операций по договору.
     */
    public List<DepositOperation> getOperationsForContract(Long contractId) {
        DepositContract contract = getContractById(contractId);
        return depositOperationRepository.findByContractOrderByOperationDateTimeAsc(contract);
    }

    /**
     * Простая генерация номера договора.
     * Для учебного проекта достаточно такого варианта.
     */
    private String generateContractNumber() {
        long count = depositContractRepository.count() + 1;
        return String.format("D-%06d", count);
    }
}
