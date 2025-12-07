package org.santayn.bankdeposit.service;

import org.santayn.bankdeposit.models.User;
import org.santayn.bankdeposit.models.UserRole;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Единое место проверки UI-доступа по ролям.
 *
 * ВАЖНО:
 * Это UI-уровень. Он не заменяет серверные проверки в сервисах,
 * но делает интерфейс корректным и безопасным для учебного проекта.
 */
@Component
public class UiAccessManager {

    private static final Set<UserRole> MANAGE_CUSTOMERS =
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER);

    private static final Set<UserRole> MANAGE_PRODUCTS =
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER);

    private static final Set<UserRole> MANAGE_CONTRACTS =
            EnumSet.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.OPERATOR);

    private static final Set<UserRole> OPERATE_DEPOSITS =
            EnumSet.of(UserRole.ADMIN, UserRole.OPERATOR);

    private static final Set<UserRole> MANAGE_USERS =
            EnumSet.of(UserRole.ADMIN);

    private static final Set<UserRole> VIEW_REPORTS =
            EnumSet.of(UserRole.ADMIN, UserRole.DIRECTOR, UserRole.AUDITOR);

    public boolean canManageCustomers(User user) {
        return hasRole(user, MANAGE_CUSTOMERS);
    }

    public boolean canManageProducts(User user) {
        return hasRole(user, MANAGE_PRODUCTS);
    }

    public boolean canManageContracts(User user) {
        return hasRole(user, MANAGE_CONTRACTS);
    }

    public boolean canOperateDeposits(User user) {
        return hasRole(user, OPERATE_DEPOSITS);
    }

    public boolean canManageUsers(User user) {
        return hasRole(user, MANAGE_USERS);
    }

    public boolean canViewReports(User user) {
        return hasRole(user, VIEW_REPORTS);
    }

    private boolean hasRole(User user, Set<UserRole> allowed) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return allowed.contains(user.getRole());
    }
}
