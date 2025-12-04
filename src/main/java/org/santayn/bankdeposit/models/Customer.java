package org.santayn.bankdeposit.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Клиент депозитного отдела банка.
 * Отражается в таблице customers.
 */
@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    /**
     * Уникальный идентификатор клиента.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Фамилия.
     */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Имя.
     */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * Отчество (может быть null).
     */
    @Column(name = "middle_name", length = 100)
    private String middleName;

    /**
     * Дата рождения.
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Паспорт или иной документ.
     */
    @Column(name = "passport_number", length = 50, unique = true)
    private String passportNumber;

    /**
     * Телефон.
     */
    @Column(name = "phone", length = 30)
    private String phone;

    /**
     * E-mail.
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * Почтовый адрес.
     */
    @Column(name = "address", length = 255)
    private String address;
}
