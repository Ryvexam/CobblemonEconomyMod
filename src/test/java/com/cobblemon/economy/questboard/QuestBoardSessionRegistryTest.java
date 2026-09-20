package com.cobblemon.economy.questboard;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestBoardSessionRegistryTest {
    @Test
    void acceptsOnlyTheIssuedBoardUntilExpiry() {
        AtomicLong now = new AtomicLong(1_000L);
        QuestBoardSessionRegistry sessions = new QuestBoardSessionRegistry(Duration.ofSeconds(5), now::get);
        UUID player = UUID.randomUUID();

        sessions.issue(player, "safari_board");
        assertFalse(sessions.validateAndRefresh(player, "other_board"));

        sessions.issue(player, "safari_board");
        assertTrue(sessions.validateAndRefresh(player, "safari_board"));
        now.set(7_000L);
        assertFalse(sessions.validateAndRefresh(player, "safari_board"));
    }
}
