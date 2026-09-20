package com.cobblemon.economy.questboard;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public final class QuestBoardSessionRegistry {
    private record Session(String boardId, long expiresAt) {
    }

    private final ConcurrentHashMap<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final LongSupplier clock;

    public QuestBoardSessionRegistry(Duration ttl) {
        this(ttl, System::currentTimeMillis);
    }

    QuestBoardSessionRegistry(Duration ttl, LongSupplier clock) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Quest board session TTL must be positive");
        }
        this.ttlMillis = ttl.toMillis();
        this.clock = clock;
    }

    public void issue(UUID playerId, String boardId) {
        if (playerId == null || boardId == null || boardId.isBlank()) {
            return;
        }
        sessions.put(playerId, new Session(boardId, clock.getAsLong() + ttlMillis));
    }

    public boolean validateAndRefresh(UUID playerId, String boardId) {
        if (playerId == null || boardId == null) {
            return false;
        }
        long now = clock.getAsLong();
        Session updated = sessions.computeIfPresent(playerId, (ignored, session) -> {
            if (session.expiresAt <= now || !session.boardId.equals(boardId)) {
                return null;
            }
            return new Session(boardId, now + ttlMillis);
        });
        return updated != null;
    }
}
