package org.santayn.bankdeposit.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Утилиты форматирования и парсинга денежных значений и ставок для UI.
 */
public final class MoneyUtil {

    private MoneyUtil() {
    }

    /**
     * Парсит положительную сумму (строка может содержать запятую).
     * Возвращает null при ошибке или если сумма <= 0.
     */
    public static BigDecimal parsePositiveMoney(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            BigDecimal v = new BigDecimal(s.replace(',', '.'));
            if (v.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Форматирование денег для таблиц.
     */
    public static String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        try {
            return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (Exception e) {
            return value.toPlainString();
        }
    }

    /**
     * Форматирование процентной ставки.
     */
    public static String formatRate(BigDecimal value) {
        if (value == null) {
            return "";
        }
        try {
            return value.stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            return value.toPlainString();
        }
    }
}
