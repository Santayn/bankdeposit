package org.santayn.bankdeposit.initializer;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.models.UserRole;
import org.santayn.bankdeposit.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Инициализация тестовых пользователей.
 */
@Component
@RequiredArgsConstructor
public class UserDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserDataInitializer.class);

    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        createIfNotExists(
                "admin",
                "admin",
                "Администратор системы",
                UserRole.ADMIN,
                true
        );

        createIfNotExists(
                "director",
                "director",
                "Директор",
                UserRole.DIRECTOR,
                true
        );

        createIfNotExists(
                "manager",
                "manager",
                "Менеджер",
                UserRole.MANAGER,
                true
        );

        createIfNotExists(
                "operator",
                "operator",
                "Операционист",
                UserRole.OPERATOR,
                true
        );

        createIfNotExists(
                "auditor",
                "auditor",
                "Аудитор",
                UserRole.AUDITOR,
                true
        );
    }

    private void createIfNotExists(
            String username,
            String password,
            String fullName,
            UserRole role,
            boolean active
    ) {
        userRepository.findByUsername(username).ifPresentOrElse(
                u -> log.info("Пользователь {} уже существует, инициализация пропущена", username),
                () -> {
                    User user = User.builder()
                            .username(username)
                            .password(password)
                            .fullName(fullName)
                            .role(role)
                            .active(active)
                            .build();

                    userRepository.save(user);
                    log.info("Создан тестовый пользователь: {} ({})", username, role);
                }
        );
    }
}
