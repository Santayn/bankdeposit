package org.santayn.bankdeposit;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Точка входа Spring Boot-приложения.
 * Не запускает UI напрямую, а только создаёт контекст,
 * который потом использует JavaFX-приложение.
 */
@SpringBootApplication
public class BankDepositApplication {

    public static void main(String[] args) {
        // Запуск JavaFX-приложения, внутри которого поднимется Spring
        javafx.application.Application.launch(BankDepositJavaFxApplication.class, args);
    }

    /**
     * Создаёт и возвращает контекст Spring.
     * Вызывается из JavaFX-приложения.
     */
    static ConfigurableApplicationContext createSpringContext(String[] args) {
        return new SpringApplicationBuilder(BankDepositApplication.class)
                .run(args);
    }
}
