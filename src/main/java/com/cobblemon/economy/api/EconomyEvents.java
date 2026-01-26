package com.cobblemon.economy.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.math.BigDecimal;
import java.util.UUID;

public class EconomyEvents {
    
    /**
     * Called before a balance change.
     * Return false to cancel the change.
     */
    public static final Event<BalanceUpdate> BALANCE_UPDATE_PRE = EventFactory.createArrayBacked(BalanceUpdate.class, callbacks -> (uuid, oldAmount, newAmount, isPco) -> {
        for (BalanceUpdate callback : callbacks) {
            if (!callback.handle(uuid, oldAmount, newAmount, isPco)) {
                return false;
            }
        }
        return true;
    });

    /**
     * Called after a successful balance change.
     */
    public static final Event<BalanceUpdatePost> BALANCE_UPDATE_POST = EventFactory.createArrayBacked(BalanceUpdatePost.class, callbacks -> (uuid, oldAmount, newAmount, isPco) -> {
        for (BalanceUpdatePost callback : callbacks) {
            callback.handle(uuid, oldAmount, newAmount, isPco);
        }
    });

    @FunctionalInterface
    public interface BalanceUpdate {
        boolean handle(UUID uuid, BigDecimal oldAmount, BigDecimal newAmount, boolean isPco);
    }
    
    @FunctionalInterface
    public interface BalanceUpdatePost {
        void handle(UUID uuid, BigDecimal oldAmount, BigDecimal newAmount, boolean isPco);
    }
}
