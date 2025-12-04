package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;

import org.santayn.bankdeposit.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для работы с клиентами.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private CustomerRepository customerRepository;

    /**
     * Возвращает список всех клиентов.
     */
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    /**
     * Возвращает клиента по идентификатору или выбрасывает EntityNotFoundException.
     */
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент с id=" + id + " не найден"));
    }

    /**
     * Поиск клиентов по части фамилии (без учёта регистра).
     */
    public List<Customer> searchByLastName(String lastNamePart) {
        if (lastNamePart == null || lastNamePart.isBlank()) {
            return getAllCustomers();
        }
        return customerRepository.findByLastNameContainingIgnoreCase(lastNamePart.trim());
    }

    /**
     * Создание нового клиента.
     */
    public Customer createCustomer(Customer customer) {
        customer.setId(null);
        return customerRepository.save(customer);
    }

    /**
     * Обновление существующего клиента.
     */
    public Customer updateCustomer(Long id, Customer updated) {
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

    /**
     * Удаление клиента по идентификатору.
     */
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new EntityNotFoundException("Клиент с id=" + id + " не найден");
        }
        customerRepository.deleteById(id);
    }
}
