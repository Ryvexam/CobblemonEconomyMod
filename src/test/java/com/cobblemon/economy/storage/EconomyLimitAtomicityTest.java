package com.cobblemon.economy.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyLimitAtomicityTest {
    @TempDir
    File tempDir;

    @Test
    void purchaseLimitCanBeRefundedAfterFailedDelivery() {
        EconomyManager manager = new EconomyManager(new File(tempDir, "economy.db"));
        UUID player = UUID.randomUUID();

        assertTrue(manager.consumePurchaseLimit(player, "shop", "item", 2, 3, 0));
        assertFalse(manager.consumePurchaseLimit(player, "shop", "item", 2, 3, 0));
        manager.refundPurchaseLimit(player, "shop", "item", 1);
        assertTrue(manager.consumePurchaseLimit(player, "shop", "item", 2, 3, 0));
    }

    @Test
    void concurrentPurchasesCannotExceedLimit() throws Exception {
        EconomyManager manager = new EconomyManager(new File(tempDir, "concurrent.db"));
        UUID player = UUID.randomUUID();
        AtomicInteger accepted = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(8)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 20; i++) {
                futures.add(executor.submit(() -> {
                    if (manager.consumePurchaseLimit(player, "shop", "item", 1, 5, 0)) {
                        accepted.incrementAndGet();
                    }
                }));
            }
            for (var future : futures) {
                future.get();
            }
        }

        assertEquals(5, accepted.get());
    }
}
