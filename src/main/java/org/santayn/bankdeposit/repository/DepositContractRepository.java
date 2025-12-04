package org.santayn.bankdeposit.repository;

import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositContractRepository extends JpaRepository<DepositContract, Long> {

    List<DepositContract> findByCustomer(Customer customer);

    List<DepositContract> findByStatus(DepositContractStatus status);
}
