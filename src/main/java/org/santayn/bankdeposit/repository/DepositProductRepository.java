package org.santayn.bankdeposit.repository;

import org.santayn.bankdeposit.models.DepositProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositProductRepository extends JpaRepository<DepositProduct, Long> {
}
