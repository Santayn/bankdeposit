package org.santayn.bankdeposit.repository;

import org.santayn.bankdeposit.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByLastNameContainingIgnoreCase(String lastNamePart);
}
