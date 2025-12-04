package org.santayn.bankdeposit.service;

/**
 * Исключение, выбрасывается когда сущность не найдена в базе данных.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
