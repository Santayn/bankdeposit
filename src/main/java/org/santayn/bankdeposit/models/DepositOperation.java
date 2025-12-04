package org.santayn.bankdeposit.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Операция по депозитному договору.
 */
@Entity
@Table(name = "deposit_operations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositOperation {

    /**
     * Уникальный идентификатор операции.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Договор, к которому относится операция.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private DepositContract contract;

    /**
     * Дата и время операции.
     */
    @Column(name = "operation_datetime", nullable = false)
    private LocalDateTime operationDateTime;

    /**
     * Тип операции.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", length = 30, nullable = false)
    private DepositOperationType type;

    /**
     * Сумма операции.
     * Для снятия можно хранить положительную сумму и интерпретировать знак по типу операции.
     */
    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    /**
     * Дополнительное описание (примечание).
     */
    @Column(name = "description", length = 255)
    private String description;
}
