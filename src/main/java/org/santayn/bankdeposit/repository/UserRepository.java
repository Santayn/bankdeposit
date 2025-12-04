package org.santayn.bankdeposit.repository;

import org.santayn.bankdeposit.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для работы с пользователями системы.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Поиск пользователя по имени (логину).
     *
     * @param username имя пользователя
     * @return Optional с пользователем, если найден
     */
    Optional<User> findByUsername(String username);
}
