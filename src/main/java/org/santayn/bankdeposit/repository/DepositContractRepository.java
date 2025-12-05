package org.santayn.bankdeposit.repository;

import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий договоров вкладов.
 */
public interface DepositContractRepository extends JpaRepository<DepositContract, Long> {

    Optional<DepositContract> findByContractNumber(String contractNumber);

    List<DepositContract> findByCustomerId(Long customerId);

    List<DepositContract> findByStatus(DepositContractStatus status);
}
