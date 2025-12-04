package org.santayn.bankdeposit.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Депозитный продукт (вид вклада):
 * название, базовая ставка, срок, условия.
 */
@Entity
@Table(name = "deposit_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositProduct {

    /**
     * Уникальный идентификатор продукта.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название продукта (например, "Накопительный+", "Классический").
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Описание условий вклада.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Минимальная сумма вклада.
     */
    @Column(name = "min_amount", precision = 18, scale = 2)
    private BigDecimal minAmount;

    /**
     * Максимальная сумма вклада (может быть null).
     */
    @Column(name = "max_amount", precision = 18, scale = 2)
    private BigDecimal maxAmount;

    /**
     * Срок вклада в месяцах (например, 6, 12, 24).
     * Для вкладов "до востребования" можно использовать 0.
     */
    @Column(name = "term_months")
    private Integer termMonths;

    /**
     * Базовая процентная ставка годовых.
     */
    @Column(name = "base_interest_rate", precision = 5, scale = 2)
    private BigDecimal baseInterestRate;

    /**
     * Разрешено ли пополнять вклад.
     */
    @Column(name = "allow_replenishment")
    private Boolean allowReplenishment;

    /**
     * Разрешено ли частичное снятие средств.
     */
    @Column(name = "allow_partial_withdrawal")
    private Boolean allowPartialWithdrawal;

    /**
     * Начисляются ли проценты с капитализацией (проценты на проценты).
     */
    @Column(name = "capitalization")
    private Boolean capitalization;
}
