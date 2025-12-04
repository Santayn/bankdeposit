package org.santayn.bankdeposit.models;

/**
 * Тип операции по депозитному договору.
 */
public enum DepositOperationType {

    /**
     * Открытие вклада (первоначальное размещение средств).
     */
    OPENING,

    /**
     * Пополнение вклада.
     */
    DEPOSIT,

    /**
     * Снятие части суммы вклада.
     */
    WITHDRAWAL,

    /**
     * Начисление процентов.
     */
    INTEREST_ACCRUAL,

    /**
     * Закрытие вклада и возврат средств.
     */
    CLOSING
}
