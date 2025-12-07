package org.santayn.bankdeposit.repository;

import org.santayn.bankdeposit.models.DepositContract;
import org.santayn.bankdeposit.models.DepositContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepositContractRepository extends JpaRepository<DepositContract, Long> {

    @Query("""
            select dc
            from DepositContract dc
            join fetch dc.customer
            join fetch dc.product
            """)
    List<DepositContract> findAllWithCustomerAndProduct();

    @Query("""
            select dc
            from DepositContract dc
            join fetch dc.customer
            join fetch dc.product
            where dc.id = :id
            """)
    Optional<DepositContract> findByIdWithCustomerAndProduct(Long id);

    List<DepositContract> findByStatus(DepositContractStatus depositContractStatus);

    List<DepositContract> findByCustomerId(Long customerId);
}
