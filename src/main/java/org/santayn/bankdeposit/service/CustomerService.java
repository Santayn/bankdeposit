package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.repository.CustomerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll(
                Sort.by(
                        Sort.Order.asc("lastName"),
                        Sort.Order.asc("firstName"),
                        Sort.Order.asc("middleName")
                )
        );
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Клиент с id=" + id + " не найден")
                );
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customer == null) {
            throw new InvalidOperationException("Нельзя создать пустого клиента");
        }
        customer.setId(null);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer updated) {
        if (id == null) {
            throw new InvalidOperationException("id клиента обязателен");
        }
        Customer existing = getCustomerById(id);

        existing.setLastName(updated.getLastName());
        existing.setFirstName(updated.getFirstName());
        existing.setMiddleName(updated.getMiddleName());
        existing.setDateOfBirth(updated.getDateOfBirth());
        existing.setPassportNumber(updated.getPassportNumber());
        existing.setPhone(updated.getPhone());
        existing.setEmail(updated.getEmail());
        existing.setAddress(updated.getAddress());

        return customerRepository.save(existing);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer existing = getCustomerById(id);
        customerRepository.delete(existing);
    }

    public Customer findByPassportNumber(String passport) {
        return null;
    }
}
