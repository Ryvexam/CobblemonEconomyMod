package com.cobblemon.economy.quest;

import com.cobblemon.economy.storage.QuestConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestRotationSelector {
    private static final int QUESTS_PER_TIER = 2;

    private QuestRotationSelector() {
    }

    public static List<String> selectBalanced(List<String> preferredIds,
                                              List<String> fallbackIds,
                                              Map<String, QuestConfig.QuestDefinition> definitions,
                                              int limit,
                                              Collection<String> reservedIds) {
        if (limit <= 0 || definitions == null || definitions.isEmpty()) {
            return List.of();
        }

        List<String> selected = new ArrayList<>();
        Set<String> selectedSet = new HashSet<>();
        if (reservedIds != null) {
            for (String id : reservedIds) {
                if (id != null && selectedSet.add(id)) {
                    selected.add(id);
                }
            }
        }
        int reservedCount = selected.size();
        if (reservedCount >= limit) {
            return List.of();
        }
        List<String> preferred = uniqueKnownIds(preferredIds, definitions, selectedSet);
        List<String> fallback = uniqueKnownIds(fallbackIds, definitions, selectedSet);

        for (int tier = 1; tier <= 3 && selected.size() < limit; tier++) {
            int needed = QUESTS_PER_TIER - countTier(selected, definitions, tier);
            addTier(selected, selectedSet, preferred, definitions, tier, needed, limit);
            needed = QUESTS_PER_TIER - countTier(selected, definitions, tier);
            addTier(selected, selectedSet, fallback, definitions, tier, needed, limit);
        }

        addRemaining(selected, selectedSet, preferred, limit);
        addRemaining(selected, selectedSet, fallback, limit);
        if (selected.size() <= reservedCount) {
            return List.of();
        }
        return new ArrayList<>(selected.subList(reservedCount, Math.min(limit, selected.size())));
    }

    private static List<String> uniqueKnownIds(List<String> ids,
                                               Map<String, QuestConfig.QuestDefinition> definitions,
                                               Set<String> selectedSet) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (ids == null) {
            return new ArrayList<>();
        }
        for (String id : ids) {
            if (id != null && definitions.containsKey(id) && !selectedSet.contains(id)) {
                unique.add(id);
            }
        }
        return new ArrayList<>(unique);
    }

    private static void addTier(List<String> selected,
                                Set<String> selectedSet,
                                List<String> candidates,
                                Map<String, QuestConfig.QuestDefinition> definitions,
                                int tier,
                                int count,
                                int limit) {
        if (count <= 0) {
            return;
        }
        for (String id : candidates) {
            if (selected.size() >= limit || count <= 0) {
                return;
            }
            if (selectedSet.contains(id) || QuestDifficulty.tier(definitions.get(id)) != tier) {
                continue;
            }
            selected.add(id);
            selectedSet.add(id);
            count--;
        }
    }

    private static void addRemaining(List<String> selected,
                                     Set<String> selectedSet,
                                     List<String> candidates,
                                     int limit) {
        for (String id : candidates) {
            if (selected.size() >= limit) {
                return;
            }
            if (selectedSet.add(id)) {
                selected.add(id);
            }
        }
    }

    private static int countTier(List<String> selected,
                                 Map<String, QuestConfig.QuestDefinition> definitions,
                                 int tier) {
        int count = 0;
        for (String id : selected) {
            if (QuestDifficulty.tier(definitions.get(id)) == tier) {
                count++;
            }
        }
        return count;
    }
}
