package org.santayn.bankdeposit.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Пользователь информационной системы депозитного отдела банка.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * Уникальный идентификатор пользователя.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальное имя пользователя (логин).
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Пароль пользователя.
     * В учебном проекте может храниться в открытом виде.
     * В реальных системах необходимо использовать хэширование.
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    /**
     * Полное имя пользователя.
     */
    @Column(name = "full_name", length = 100)
    private String fullName;

    /**
     * Роль пользователя.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private UserRole role;

    /**
     * Признак активности пользователя.
     * Неактивный пользователь не может входить в систему.
     */
    @Column(name = "active", nullable = false)
    private Boolean active;
}
