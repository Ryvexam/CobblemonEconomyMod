package com.cobblemon.economy.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PokedexProgressCompatTest {
    private enum LegacyProgress {
        CAUGHT,
        ENCOUNTERED
    }

    private enum ModernProgress {
        OWNED,
        SEEN,
        UNREGISTERED
    }

    @Test
    void acceptsLegacyCaughtStatus() {
        assertTrue(PokedexProgressCompat.isCaughtOrOwned(LegacyProgress.CAUGHT));
    }

    @Test
    void acceptsModernOwnedStatus() {
        assertTrue(PokedexProgressCompat.isCaughtOrOwned(ModernProgress.OWNED));
    }

    @Test
    void rejectsNonOwnedStatusesAndNull() {
        assertFalse(PokedexProgressCompat.isCaughtOrOwned(LegacyProgress.ENCOUNTERED));
        assertFalse(PokedexProgressCompat.isCaughtOrOwned(ModernProgress.SEEN));
        assertFalse(PokedexProgressCompat.isCaughtOrOwned(ModernProgress.UNREGISTERED));
        assertFalse(PokedexProgressCompat.isCaughtOrOwned(null));
    }
}
