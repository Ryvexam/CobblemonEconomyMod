package com.cobblemon.economy.quest;

import com.cobblemon.economy.storage.QuestConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestRotationSelectorTest {
    @Test
    void defaultRotationFillsTwoQuestsPerDifficultyFromFallbacks() {
        Map<String, QuestConfig.QuestDefinition> definitions = new LinkedHashMap<>();
        add(definitions, "preferred_one", "2000");
        add(definitions, "preferred_two", "3000");
        add(definitions, "preferred_three", "6000");
        add(definitions, "fallback_one", "1500");
        add(definitions, "fallback_two", "4000");
        add(definitions, "fallback_three", "7000");
        add(definitions, "fallback_four", "10000");
        add(definitions, "fallback_five", "12000");

        List<String> selected = QuestRotationSelector.selectBalanced(
                List.of("preferred_one", "preferred_two", "preferred_three"),
                List.of("fallback_one", "fallback_two", "fallback_three", "fallback_four", "fallback_five"),
                definitions,
                6,
                List.of()
        );

        assertEquals(6, selected.size());
        assertEquals(2, countTier(selected, definitions, 1));
        assertEquals(2, countTier(selected, definitions, 2));
        assertEquals(2, countTier(selected, definitions, 3));
    }

    private static void add(Map<String, QuestConfig.QuestDefinition> definitions, String id, String reward) {
        QuestConfig.QuestDefinition definition = new QuestConfig.QuestDefinition();
        definition.rewards.pokedollars = new BigDecimal(reward);
        definitions.put(id, definition);
    }

    private static int countTier(List<String> ids, Map<String, QuestConfig.QuestDefinition> definitions, int tier) {
        return (int) ids.stream()
                .filter(id -> QuestDifficulty.tier(definitions.get(id)) == tier)
                .count();
    }
}
