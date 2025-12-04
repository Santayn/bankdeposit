package org.santayn.bankdeposit.repository;

import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositOperation;
import org.santayn.bankdeposit.models.DepositOperationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для операций по депозитным договорам.
 */
public interface DepositOperationRepository extends JpaRepository<DepositOperation, Long> {

    /**
     * Все операции по договору, отсортированные по дате и времени.
     */
    List<DepositOperation> findByContractOrderByOperationDateTimeAsc(DepositContract contract);

    /**
     * Операции по договору за период.
     */
    List<DepositOperation> findByContractAndOperationDateTimeBetween(
            DepositContract contract,
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * Операции определённого типа по всем договорам.
     */
    List<DepositOperation> findByType(DepositOperationType type);

    /**
     * Все операции за период по всем договорам.
     */
    List<DepositOperation> findByOperationDateTimeBetween(
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * Операции определённого типа за период по всем договорам.
     */
    List<DepositOperation> findByTypeAndOperationDateTimeBetween(
            DepositOperationType type,
            LocalDateTime from,
            LocalDateTime to
    );
}
