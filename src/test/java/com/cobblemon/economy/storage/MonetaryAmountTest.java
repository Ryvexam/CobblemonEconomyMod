package com.cobblemon.economy.storage;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MonetaryAmountTest {
    @Test
    void parsesExactDecimalAmountsWithoutDoubleConversion() {
        assertEquals(new BigDecimal("0.01"), MonetaryAmount.parse("0.01"));
        assertEquals(new BigDecimal("123456789.99"), MonetaryAmount.parse("123456789.99"));
        assertEquals(BigDecimal.ZERO, MonetaryAmount.parse("0.00"));
    }

    @Test
    void rejectsNegativeExcessiveScaleScientificAndOversizedAmounts() {
        assertThrows(IllegalArgumentException.class, () -> MonetaryAmount.parse("-1"));
        assertThrows(IllegalArgumentException.class, () -> MonetaryAmount.parse("0.001"));
        assertThrows(IllegalArgumentException.class, () -> MonetaryAmount.parse("1e3"));
        assertThrows(IllegalArgumentException.class, () -> MonetaryAmount.parse("1000000000000000001"));
    }
}
