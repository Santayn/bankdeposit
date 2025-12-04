package org.santayn.bankdeposit.service;

/**
 * Исключение, выбрасывается при нарушении бизнес-правил,
 * например, попытке снять денег больше, чем есть на вкладе.
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
