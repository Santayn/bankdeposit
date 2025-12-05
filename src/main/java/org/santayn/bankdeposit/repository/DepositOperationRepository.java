package org.santayn.bankdeposit.repository;

import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.DepositOperationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий операций по вкладам.
 */
public interface DepositOperationRepository extends JpaRepository<DepositOperation, Long> {

    List<DepositOperation> findByContractIdOrderByOperationDateTime(Long contractId);

    List<DepositOperation> findByOperationDateTimeBetween(LocalDateTime from, LocalDateTime to);

    List<DepositOperation> findByTypeAndOperationDateTimeBetween(
            DepositOperationType type,
            LocalDateTime from,
            LocalDateTime to
    );
}
