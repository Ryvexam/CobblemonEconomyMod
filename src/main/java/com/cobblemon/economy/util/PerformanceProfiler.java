package com.cobblemon.economy.util;

import com.cobblemon.economy.storage.EconomyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class PerformanceProfiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceProfiler.class);
    private static volatile EconomyConfig config;

    private PerformanceProfiler() {
    }

    public static void configure(EconomyConfig economyConfig) {
        config = economyConfig;
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
        LOGGER.info("[Perf] {} took {} ms{}", name, elapsedMs, suffix);
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
        EconomyConfig current = config;
        return current != null && current.enableProfiling;
    }

    private static int getThresholdMs() {
        EconomyConfig current = config;
        if (current == null || current.profilingThresholdMs <= 0) {
            return 1;
        }
        return current.profilingThresholdMs;
    }
}
