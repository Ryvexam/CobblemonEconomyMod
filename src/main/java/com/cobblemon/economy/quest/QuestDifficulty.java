package com.cobblemon.economy.quest;

import com.cobblemon.economy.storage.QuestConfig;

import java.math.BigDecimal;

public final class QuestDifficulty {
    private static final BigDecimal TIER_2_MIN_REWARD = new BigDecimal("5000");
    private static final BigDecimal TIER_3_MIN_REWARD = new BigDecimal("10000");

    private QuestDifficulty() {
    }

    public static int tier(QuestConfig.QuestDefinition definition) {
        return tier(definition == null || definition.rewards == null
                ? null
                : definition.rewards.pokedollars);
    }

    public static int tier(BigDecimal pokedollars) {
        if (pokedollars != null && pokedollars.compareTo(TIER_3_MIN_REWARD) >= 0) {
            return 3;
        }
        if (pokedollars != null && pokedollars.compareTo(TIER_2_MIN_REWARD) >= 0) {
            return 2;
        }
        return 1;
    }
}
