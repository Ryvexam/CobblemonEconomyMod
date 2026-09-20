package com.cobblemon.economy.quest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestManagerAtomicityTest {
    @TempDir
    File tempDir;

    @Test
    void objectiveIncrementsAreAtomicAndCapped() throws Exception {
        QuestManager manager = new QuestManager(new File(tempDir, "quests.db"));
        UUID player = UUID.randomUUID();
        assertTrue(manager.acceptQuest(player, "board", "quest", 1));

        try (var executor = Executors.newFixedThreadPool(4)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 40; i++) {
                futures.add(executor.submit(() -> manager.incrementObjectiveProgress(player, "board", "quest", 0, 1, 25)));
            }
            for (var future : futures) {
                future.get();
            }
        }

        assertEquals(25, manager.getObjectiveProgress(player, "board", "quest", 0));
    }

    @Test
    void completedQuestCanBeClaimedOnlyOnce() {
        QuestManager manager = new QuestManager(new File(tempDir, "claims.db"));
        UUID player = UUID.randomUUID();
        assertTrue(manager.acceptQuest(player, "board", "quest", 1));
        manager.markQuestCompleted(player, "board", "quest");

        assertTrue(manager.claimCompletedQuest(player, "board", "quest", 123L));
        assertFalse(manager.claimCompletedQuest(player, "board", "quest", 123L));
        assertEquals("CLAIMED", manager.getQuestState(player, "board", "quest").status);
    }
}
