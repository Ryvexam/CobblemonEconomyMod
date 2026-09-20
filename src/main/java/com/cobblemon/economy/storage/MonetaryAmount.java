package com.cobblemon.economy.storage;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class MonetaryAmount {
    public static final int MAX_SCALE = 2;
    public static final BigDecimal MAX_VALUE = new BigDecimal("1000000000000000000");
    private static final Pattern DECIMAL = Pattern.compile("[0-9]+(?:\\.[0-9]{1,2})?");

    private MonetaryAmount() {
    }

    public static BigDecimal parse(String value) {
        if (value == null || value.length() > 32 || !DECIMAL.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid monetary amount");
        }
        return requireValid(new BigDecimal(value));
    }

    public static BigDecimal requireValid(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX_VALUE) > 0) {
            throw new IllegalArgumentException("Monetary amount is outside the supported range");
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (Math.max(0, normalized.scale()) > MAX_SCALE) {
            throw new IllegalArgumentException("Monetary amount supports at most two decimal places");
        }
        return normalized.signum() == 0 ? BigDecimal.ZERO : normalized;
    }
}
