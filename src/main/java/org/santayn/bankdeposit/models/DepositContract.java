package org.santayn.bankdeposit.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
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
import java.time.LocalDate;

/**
 * Депозитный договор (конкретный вклад клиента).
 */
@Entity
@Table(name = "deposit_contracts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositContract {

    /**
     * Уникальный идентификатор договора.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Номер договора (отображается клиенту).
     */
    @Column(name = "contract_number", nullable = false, length = 50, unique = true)
    private String contractNumber;

    /**
     * Клиент, который открыл вклад.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * Депозитный продукт, по которому открыт вклад.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private DepositProduct product;

    /**
     * Дата открытия вклада.
     */
    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    /**
     * Дата закрытия вклада (если закрыт).
     */
    @Column(name = "close_date")
    private LocalDate closeDate;

    /**
     * Сумма, внесённая при открытии вклада.
     */
    @Column(name = "initial_amount", precision = 18, scale = 2)
    private BigDecimal initialAmount;

    /**
     * Текущий баланс по вкладу.
     */
    @Column(name = "current_balance", precision = 18, scale = 2)
    private BigDecimal currentBalance;

    /**
     * Процентная ставка по договору (может отличаться от базовой у продукта).
     */
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    /**
     * Статус договора.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DepositContractStatus status;
}
