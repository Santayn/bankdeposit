package org.santayn.bankdeposit.models;

/**
 * Статус депозитного договора.
 */
public enum DepositContractStatus {

    /**
     * Вклад открыт, операции разрешены.
     */
    OPEN,

    /**
     * Вклад закрыт, операции запрещены.
     */
    CLOSED,

    /**
     * Вклад заморожен (по решению банка и т.п.).
     */
    FROZEN
}
