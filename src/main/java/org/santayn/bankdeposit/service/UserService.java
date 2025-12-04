package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для работы с пользователями.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /**
     * Репозиторий внедряется через конструктор (Lombok @RequiredArgsConstructor).
     * ВАЖНО: поле должно быть final, иначе останется null.
     */
    private final UserRepository userRepository;

    /**
     * Аутентификация пользователя по имени и паролю.
     *
     * @param username имя пользователя
     * @param password пароль
     * @return найденный пользователь
     * @throws InvalidOperationException если логин/пароль неверные или пользователь заблокирован
     */
    public User authenticate(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new InvalidOperationException("Имя пользователя не может быть пустым");
        }
        if (password == null || password.isBlank()) {
            throw new InvalidOperationException("Пароль не может быть пустым");
        }

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new InvalidOperationException("Неверное имя пользователя или пароль"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidOperationException("Пользователь заблокирован и не может входить в систему");
        }

        if (!user.getPassword().equals(password)) {
            throw new InvalidOperationException("Неверное имя пользователя или пароль");
        }

        return user;
    }

    /**
     * Возвращает всех пользователей.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Создаёт нового пользователя.
     */
    public User createUser(User user) {
        user.setId(null);
        return userRepository.save(user);
    }

    /**
     * Обновляет существующего пользователя.
     */
    public User updateUser(Long id, User updated) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id=" + id + " не найден"));

        existing.setUsername(updated.getUsername());
        existing.setPassword(updated.getPassword());
        existing.setFullName(updated.getFullName());
        existing.setRole(updated.getRole());
        existing.setActive(updated.getActive());

        return userRepository.save(existing);
    }

    /**
     * Удаляет пользователя по идентификатору.
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("Пользователь с id=" + id + " не найден");
        }
        userRepository.deleteById(id);
    }
}
