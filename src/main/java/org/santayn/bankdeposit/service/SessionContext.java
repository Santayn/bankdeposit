package org.santayn.bankdeposit.service;

import lombok.Data;
import org.santayn.bankdeposit.models.User;
import org.springframework.stereotype.Component;

/**
 * Контекст текущей сессии пользователя.
 * Хранит информацию о пользователе, вошедшем в систему.
 */
@Component
@Data
public class SessionContext {

    /**
     * Текущий авторизованный пользователь.
     * Может быть null до момента успешного входа.
     */
    private User currentUser;
}
