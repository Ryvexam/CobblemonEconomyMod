package com.cobblemon.economy.util;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.storage.EconomyConfig;


public final class PerformanceProfiler {
    private PerformanceProfiler() {
    }

    public static long start() {
        if (!isEnabled()) {
            return 0L;
        }
        return System.nanoTime();
    }

    public static void end(String name, long startNanos, String details) {
        if (startNanos == 0L || !isEnabled()) {
            return;
        }
        long elapsedNanos = System.nanoTime() - startNanos;
        long elapsedMs = elapsedNanos / 1_000_000L;
        int thresholdMs = getThresholdMs();
        if (elapsedMs < thresholdMs) {
            return;
        }
        String suffix = details == null || details.isBlank() ? "" : " | " + details;
        CobblemonEconomy.LOGGER.info("[Perf] {} took {} ms{}", name, elapsedMs, suffix);
    }

    public static String format(String key, Object value) {
        if (value == null) {
            return key + "=null";
        }
        if (value instanceof String str) {
            return key + "=" + str;
        }
        return key + "=" + String.valueOf(value);
    }

    private static boolean isEnabled() {
        EconomyConfig config = CobblemonEconomy.getConfig();
        return config != null && config.enableProfiling;
    }

    private static int getThresholdMs() {
        EconomyConfig config = CobblemonEconomy.getConfig();
        if (config == null || config.profilingThresholdMs <= 0) {
            return 1;
        }
        return config.profilingThresholdMs;
    }
}
