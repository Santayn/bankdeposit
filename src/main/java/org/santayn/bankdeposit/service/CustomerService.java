package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.Customer;
import org.santayn.bankdeposit.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с клиентами банка.
 * Выполняет валидацию и обращается к репозиторию.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * Возвращает всех клиентов.
     */
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    /**
     * Возвращает клиента по идентификатору.
     *
     * @param id идентификатор клиента
     * @return найденный клиент
     * @throws EntityNotFoundException если клиент не найден
     */
    public Customer getCustomerById(Long id) {
        if (id == null) {
            throw new InvalidOperationException("Идентификатор клиента не указан");
        }

        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Клиент с id=" + id + " не найден"
                ));
    }

    /**
     * Создаёт нового клиента.
     *
     * @param customer данные клиента
     * @return созданный клиент
     */
    public Customer createCustomer(Customer customer) {
        if (customer == null) {
            throw new InvalidOperationException("Данные клиента не переданы");
        }

        validateCustomer(customer, true);

        // На всякий случай обнуляем id, чтобы точно была вставка
        customer.setId(null);
        return customerRepository.save(customer);
    }

    /**
     * Обновляет существующего клиента.
     *
     * @param id       идентификатор редактируемого клиента
     * @param updated  новые данные
     * @return обновлённый клиент
     */
    public Customer updateCustomer(Long id, Customer updated) {
        if (id == null) {
            throw new InvalidOperationException("Идентификатор клиента не указан");
        }
        if (updated == null) {
            throw new InvalidOperationException("Данные клиента не переданы");
        }

        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Клиент с id=" + id + " не найден"
                ));

        // Помещаем целевые значения во временный объект для валидации
        Customer toValidate = new Customer();
        toValidate.setId(existing.getId());
        toValidate.setFirstName(updated.getFirstName());
        toValidate.setLastName(updated.getLastName());
        toValidate.setMiddleName(updated.getMiddleName());
        toValidate.setDateOfBirth(updated.getDateOfBirth());
        toValidate.setPassportNumber(updated.getPassportNumber());
        toValidate.setAddress(updated.getAddress());
        toValidate.setPhone(updated.getPhone());
        toValidate.setEmail(updated.getEmail());

        validateCustomer(toValidate, false);

        existing.setFirstName(toValidate.getFirstName());
        existing.setLastName(toValidate.getLastName());
        existing.setMiddleName(toValidate.getMiddleName());
        existing.setDateOfBirth(toValidate.getDateOfBirth());
        existing.setPassportNumber(toValidate.getPassportNumber());
        existing.setAddress(toValidate.getAddress());
        existing.setPhone(toValidate.getPhone());
        existing.setEmail(toValidate.getEmail());

        return customerRepository.save(existing);
    }

    /**
     * Удаляет клиента по идентификатору.
     *
     * @param id идентификатор клиента
     */
    public void deleteCustomer(Long id) {
        if (id == null) {
            throw new InvalidOperationException("Идентификатор клиента не указан");
        }

        if (!customerRepository.existsById(id)) {
            throw new EntityNotFoundException("Клиент с id=" + id + " не найден");
        }

        customerRepository.deleteById(id);
    }

    /**
     * Поиск клиента по номеру паспорта.
     *
     * @param passportNumber номер паспорта
     * @return клиент, если найден
     */
    public Customer findByPassportNumber(String passportNumber) {
        if (passportNumber == null || passportNumber.isBlank()) {
            throw new InvalidOperationException("Номер паспорта не может быть пустым");
        }

        Optional<Customer> optionalCustomer =
                customerRepository.findByPassportNumber(passportNumber.trim());

        return optionalCustomer.orElseThrow(() ->
                new EntityNotFoundException(
                        "Клиент с паспортом \"" + passportNumber + "\" не найден"
                )
        );
    }

    /**
     * Базовая валидация данных клиента.
     *
     * @param customer  клиент
     * @param isCreate  true, если создаём нового (для проверки уникальности паспорта)
     */
    private void validateCustomer(Customer customer, boolean isCreate) {
        if (customer.getFirstName() == null || customer.getFirstName().isBlank()) {
            throw new InvalidOperationException("Имя клиента не может быть пустым");
        }
        if (customer.getLastName() == null || customer.getLastName().isBlank()) {
            throw new InvalidOperationException("Фамилия клиента не может быть пустой");
        }

        LocalDate dob = customer.getDateOfBirth();
        if (dob != null && dob.isAfter(LocalDate.now())) {
            throw new InvalidOperationException("Дата рождения не может быть в будущем");
        }

        String passport = customer.getPassportNumber();
        if (passport != null && !passport.isBlank()) {
            String normalized = passport.trim();

            // Проверяем уникальность паспорта
            Optional<Customer> existing =
                    customerRepository.findByPassportNumber(normalized);

            if (existing.isPresent()) {
                if (isCreate || !existing.get().getId().equals(customer.getId())) {
                    throw new InvalidOperationException(
                            "Клиент с паспортом \"" + normalized + "\" уже существует"
                    );
                }
            }

            customer.setPassportNumber(normalized);
        }

        // Можно добавить проверки формата телефона и email,
        // но для учебного проекта часто достаточно базовой проверки непустоты.
    }
}
