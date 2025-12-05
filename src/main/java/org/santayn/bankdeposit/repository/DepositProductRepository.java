package org.santayn.bankdeposit.repository;

import org.santayn.bankdeposit.models.DepositProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий депозитных продуктов.
 */
public interface DepositProductRepository extends JpaRepository<DepositProduct, Long> {

    List<DepositProduct> findByNameContainingIgnoreCase(String name);

    Optional<DepositProduct> findByName(String normalizedName);
}
