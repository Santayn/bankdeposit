package org.santayn.bankdeposit.repository;

import org.santayn.bankdeposit.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByLastNameContainingIgnoreCase(String lastNamePart);
    Optional<Customer> findByPassportNumber(String passportNumber);

}
