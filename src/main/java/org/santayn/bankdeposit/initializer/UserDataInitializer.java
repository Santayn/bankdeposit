package org.santayn.bankdeposit.initializer;

import lombok.RequiredArgsConstructor;
import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.models.UserRole;

import org.santayn.bankdeposit.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Инициализатор пользователей.
 * Создаёт пользователя admin/admin, если он ещё не создан.
 */
@Component
@RequiredArgsConstructor
public class UserDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(UserDataInitializer.class);

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isPresent()) {
            logger.info("Пользователь admin уже существует, инициализация пропущена");
            return;
        }

        User admin = User.builder()
                .username("admin")
                .password("admin")
                .fullName("Администратор системы")
                .role(UserRole.ADMIN)
                .active(true)
                .build();

        userRepository.save(admin);

        logger.info("Создан пользователь admin с паролем admin и ролью ADMIN");
    }
}
