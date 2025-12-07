package org.santayn.bankdeposit.service;

import org.santayn.bankdeposit.models.User;
import org.springframework.stereotype.Component;

/**
 * Хранит информацию о текущем пользователе для UI-уровня.
 * Должен заполняться в LoginController после успешной авторизации.
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

    public void clear() {
        this.currentUser = null;
    }

    public boolean isAuthenticated() {
        return currentUser != null && Boolean.TRUE.equals(currentUser.getActive());
    }
}
