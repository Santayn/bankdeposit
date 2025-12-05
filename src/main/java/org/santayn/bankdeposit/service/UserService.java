package org.santayn.bankdeposit.service;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.models.UserRole;
import org.santayn.bankdeposit.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с пользователями.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Аутентификация пользователя по имени и паролю.
     *
     * @param username имя пользователя
     * @param password пароль
     * @return найденный пользователь
     * @throws InvalidOperationException если логин/пароль неверные или пользователь заблокирован
     */
    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username.trim());
    }

    /**
     * Создаёт нового пользователя с базовыми проверками.
     */
    @Transactional
    public User createUserValidated(User user) {
        if (user == null) {
            throw new InvalidOperationException("Пользователь не задан");
        }

        String username = normalize(user.getUsername());
        String password = normalize(user.getPassword());

        if (username.isBlank()) {
            throw new InvalidOperationException("Логин не может быть пустым");
        }
        if (password.isBlank()) {
            throw new InvalidOperationException("Пароль не может быть пустым");
        }

        if (userRepository.existsByUsername(username)) {
            throw new InvalidOperationException("Пользователь с логином '" + username + "' уже существует");
        }

        User toSave = new User();
        toSave.setId(null);
        toSave.setUsername(username);
        toSave.setPassword(password);
        toSave.setFullName(normalizeNullable(user.getFullName()));
        toSave.setRole(user.getRole() != null ? user.getRole() : UserRole.OPERATOR);
        toSave.setActive(user.getActive() != null ? user.getActive() : Boolean.TRUE);

        return userRepository.save(toSave);
    }

    /**
     * Обновляет существующего пользователя с проверками.
     *
     * @param id идентификатор редактируемого пользователя
     * @param updated данные из формы
     * @param currentUser текущий пользователь сессии (может быть null)
     */
    @Transactional
    public User updateUserValidated(Long id, User updated, User currentUser) {
        if (id == null) {
            throw new InvalidOperationException("Не указан id пользователя");
        }
        if (updated == null) {
            throw new InvalidOperationException("Данные пользователя не заданы");
        }

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id=" + id + " не найден"));

        String newUsername = normalize(updated.getUsername());
        String newPassword = normalize(updated.getPassword());

        if (newUsername.isBlank()) {
            throw new InvalidOperationException("Логин не может быть пустым");
        }
        if (newPassword.isBlank()) {
            throw new InvalidOperationException("Пароль не может быть пустым");
        }

        // Уникальность username при смене
        if (!newUsername.equalsIgnoreCase(existing.getUsername())
                && userRepository.existsByUsername(newUsername)) {
            throw new InvalidOperationException("Пользователь с логином '" + newUsername + "' уже существует");
        }

        // Нельзя понизить/сломать единственного admin? (упрощённо)
        if (existing.getRole() == UserRole.ADMIN) {
            // admin можно редактировать, но нельзя сделать неактивным (в учебном варианте)
            if (Boolean.FALSE.equals(updated.getActive())) {
                throw new InvalidOperationException("Пользователя ADMIN нельзя блокировать");
            }
        }

        // Нельзя заблокировать самого себя
        if (currentUser != null && currentUser.getId() != null && currentUser.getId().equals(existing.getId())) {
            if (Boolean.FALSE.equals(updated.getActive())) {
                throw new InvalidOperationException("Нельзя заблокировать самого себя");
            }
        }

        existing.setUsername(newUsername);
        existing.setPassword(newPassword);
        existing.setFullName(normalizeNullable(updated.getFullName()));
        existing.setRole(updated.getRole() != null ? updated.getRole() : existing.getRole());
        existing.setActive(updated.getActive() != null ? updated.getActive() : Boolean.TRUE);

        return userRepository.save(existing);
    }

    /**
     * Удаляет пользователя по идентификатору с проверками.
     */
    @Transactional
    public void deleteUserValidated(Long id, User currentUser) {
        if (id == null) {
            throw new InvalidOperationException("Не указан id пользователя");
        }

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id=" + id + " не найден"));

        if (existing.getRole() == UserRole.ADMIN) {
            throw new InvalidOperationException("Пользователя с ролью ADMIN удалять нельзя");
        }

        if (currentUser != null && currentUser.getId() != null && currentUser.getId().equals(id)) {
            throw new InvalidOperationException("Нельзя удалить самого себя");
        }

        userRepository.deleteById(id);
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    private String normalizeNullable(String s) {
        String v = normalize(s);
        return v.isBlank() ? null : v;
    }
}
