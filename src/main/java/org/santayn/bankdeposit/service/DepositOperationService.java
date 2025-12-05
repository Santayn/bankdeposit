package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.DepositOperationType;
import org.santayn.bankdeposit.repository.DepositOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис операций по вкладам.
 *
 * Отвечает за:
 * - сохранение операций
 * - выдачу списка операций по договору
 */
@Service
@RequiredArgsConstructor
public class DepositOperationService {

    private final DepositOperationRepository depositOperationRepository;

    @Transactional(readOnly = true)
    public List<DepositOperation> getOperationsByContract(Long contractId) {
        if (contractId == null) {
            throw new InvalidOperationException("Не указан идентификатор договора");
        }
        return depositOperationRepository.findByContractIdOrderByOperationDateTime(contractId);
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

        DepositOperation op = new DepositOperation();
        op.setContract(contract);
        op.setType(type);
        op.setAmount(amount);
        op.setDescription(description != null ? description.trim() : null);
        op.setOperationDateTime(dateTime != null ? dateTime : LocalDateTime.now());

        return depositOperationRepository.save(op);
    }

    public void withdraw(Long id, BigDecimal amount) {
    }

    public void deposit(Long id, BigDecimal amount) {
    }

    public void accrueInterest(Long id, BigDecimal amount) {
    }
}
