package org.santayn.bankdeposit.initializer;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Создаёт тестовые данные при первом запуске приложения.
 */
@Component
@RequiredArgsConstructor
public class DatabaseDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseDataInitializer.class);

    private final CustomerRepository customerRepository;
    private final DepositProductRepository depositProductRepository;
    private final DepositContractRepository depositContractRepository;
    private final DepositOperationRepository depositOperationRepository;

    @Override
    public void run(String... args) {
        logger.info("Запуск инициализации тестовых данных...");

        Customer customer = initCustomer();
        DepositProduct product = initProduct();
        initContractAndOperation(customer, product);

        logger.info("Инициализация тестовых данных завершена");
    }

    private Customer initCustomer() {
        long count = customerRepository.count();
        if (count > 0) {
            logger.info("Клиенты уже существуют, пропускаем создание");
            return customerRepository.findAll().get(0);
        }

        Customer customer = Customer.builder()
                .lastName("Иванов")
                .firstName("Иван")
                .middleName("Иванович")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .passportNumber("1234 567890")
                .phone("+7-900-000-00-00")
                .email("ivanov@example.com")
                .address("г. Москва, ул. Примерная, д. 1")
                .build();

        customer = customerRepository.save(customer);
        logger.info("Создан тестовый клиент с id={}", customer.getId());
        return customer;
    }

    private DepositProduct initProduct() {
        long count = depositProductRepository.count();
        if (count > 0) {
            logger.info("Депозитные продукты уже существуют, пропускаем создание");
            return depositProductRepository.findAll().get(0);
        }

        DepositProduct product = DepositProduct.builder()
                .name("Накопительный+")
                .description("Классический рублёвый вклад с возможностью пополнения и капитализацией процентов.")
                .minAmount(new BigDecimal("10000.00"))
                .maxAmount(null)
                .termMonths(12)
                .baseInterestRate(new BigDecimal("8.50"))
                .allowReplenishment(true)
                .allowPartialWithdrawal(false)
                .capitalization(true)
                .build();

        product = depositProductRepository.save(product);
        logger.info("Создан тестовый депозитный продукт с id={}", product.getId());
        return product;
    }

    private void initContractAndOperation(Customer customer, DepositProduct product) {
        long count = depositContractRepository.count();
        if (count > 0) {
            logger.info("Депозитные договоры уже существуют, пропускаем создание");
            return;
        }

        BigDecimal initialAmount = new BigDecimal("50000.00");
        DepositContract contract = DepositContract.builder()
                .contractNumber("D-000001")
                .customer(customer)
                .product(product)
                .openDate(LocalDate.now())
                .closeDate(null)
                .initialAmount(initialAmount)
                .currentBalance(initialAmount)
                .interestRate(product.getBaseInterestRate())
                .status(DepositContractStatus.OPEN)
                .build();

        contract = depositContractRepository.save(contract);
        logger.info("Создан тестовый договор вклада с id={}", contract.getId());

        DepositOperation openingOperation = DepositOperation.builder()
                .contract(contract)
                .operationDateTime(LocalDateTime.now())
                .type(DepositOperationType.OPENING)
                .amount(initialAmount)
                .description("Открытие вклада и зачисление первоначальной суммы")
                .build();

        openingOperation = depositOperationRepository.save(openingOperation);
        logger.info("Создана тестовая операция открытия вклада с id={}", openingOperation.getId());
    }
}
