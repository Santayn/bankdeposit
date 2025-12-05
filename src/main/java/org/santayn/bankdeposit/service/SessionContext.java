package org.santayn.bankdeposit.service;

import org.santayn.bankdeposit.models.User;
import org.springframework.stereotype.Component;

/**
 * Контекст текущей сессии приложения (JavaFX).
 * Хранит вошедшего пользователя.
 */
@Component
public class SessionContext {

    private User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public void clear() {
        this.currentUser = null;
    }
}
