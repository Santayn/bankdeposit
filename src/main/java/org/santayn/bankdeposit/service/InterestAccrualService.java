package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.DepositOperationType;
import org.santayn.bankdeposit.models.DepositProduct;
import org.santayn.bankdeposit.repository.DepositContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Сервис автоматического начисления процентов.
 *
 * Логика:
 * - берём все OPEN-договоры
 * - определяем дату последнего начисления:
 *      * последняя операция INTEREST_ACCRUAL
 *      * иначе дата открытия
 * - считаем дни
 * - проценты = balance * rate/100 * days/365
 * - создаём операцию INTEREST_ACCRUAL
 * - увеличиваем баланс (упрощённая модель для учебного проекта)
 *
 * Если у продукта capitalization=false, в реальном банке проценты могли бы
 * начисляться на отдельный счёт, но для учебного проекта мы добавляем к балансу.
 */
@Service
@RequiredArgsConstructor
public class InterestAccrualService {

    private final DepositContractRepository depositContractRepository;
    private final DepositOperationService depositOperationService;

    @Transactional
    public int accrueInterestForAll(LocalDate asOfDate) {
        LocalDate расчетнаяДата = asOfDate != null ? asOfDate : LocalDate.now();

        List<DepositContract> active = depositContractRepository.findByStatus(DepositContractStatus.OPEN);

        int accruedCount = 0;

        for (DepositContract contract : active) {
            boolean accrued = accrueInterestForContractInternal(contract, расчетнаяДата);
            if (accrued) {
                accruedCount++;
            }
        }

        return accruedCount;
    }

    @Transactional
    public boolean accrueInterestForContract(Long contractId, LocalDate asOfDate) {
        if (contractId == null) {
            throw new InvalidOperationException("Не указан договор");
        }

        DepositContract contract = depositContractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Договор с id=" + contractId + " не найден"));

        if (contract.getStatus() != DepositContractStatus.OPEN) {
            return false;
        }

        LocalDate расчетнаяДата = asOfDate != null ? asOfDate : LocalDate.now();

        return accrueInterestForContractInternal(contract, расчетнаяДата);
    }

    private boolean accrueInterestForContractInternal(DepositContract contract, LocalDate расчетнаяДата) {
        BigDecimal balance = normalizeMoney(contract.getCurrentBalance());
        BigDecimal rate = contract.getInterestRate() != null
                ? contract.getInterestRate()
                : BigDecimal.ZERO;

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        LocalDate lastBaseDate = resolveLastAccrualBaseDate(contract);

        long days = ChronoUnit.DAYS.between(lastBaseDate, расчетнаяДата);

        if (days <= 0) {
            return false;
        }

        BigDecimal interest = calculateDailyInterest(balance, rate, days);

        if (interest.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        DepositProduct product = contract.getProduct();

        // В учебной модели добавим к балансу в любом случае
        contract.setCurrentBalance(balance.add(interest));

        DepositContract saved = depositContractRepository.save(contract);

        String desc = "Автоначисление процентов за " + days + " дн.";

        depositOperationService.createOperation(
                saved,
                DepositOperationType.INTEREST_ACCRUAL,
                interest,
                product != null && Boolean.TRUE.equals(product.getCapitalization())
                        ? desc + " (капитализация)"
                        : desc,
                LocalDateTime.of(расчетнаяДата, java.time.LocalTime.of(12, 0))
        );

        return true;
    }

    private LocalDate resolveLastAccrualBaseDate(DepositContract contract) {
        List<DepositOperation> ops = depositOperationService.getOperationsByContract(contract.getId());

        LocalDateTime lastAccrual = null;

        for (int i = ops.size() - 1; i >= 0; i--) {
            DepositOperation op = ops.get(i);
            if (op.getType() == DepositOperationType.INTEREST_ACCRUAL) {
                lastAccrual = op.getOperationDateTime();
                break;
            }
        }

        if (lastAccrual != null) {
            return lastAccrual.toLocalDate();
        }

        if (contract.getOpenDate() != null) {
            return contract.getOpenDate();
        }

        return LocalDate.now();
    }

    private BigDecimal calculateDailyInterest(BigDecimal balance, BigDecimal rate, long days) {
        // interest = balance * rate/100 * days/365
        BigDecimal годоваяДоля = rate
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        BigDecimal dayFraction = BigDecimal.valueOf(days)
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);

        BigDecimal interest = balance.multiply(годоваяДоля).multiply(dayFraction);

        return normalizeMoney(interest);
    }

    private BigDecimal normalizeMoney(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
