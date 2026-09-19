package com.cobblemon.economy.events;

/**
 * Normalizes the Pokédex ownership status across Cobblemon API generations.
 *
 * <p>Cobblemon 1.7 calls the status {@code CAUGHT}; Cobblemon 1.8 calls the
 * equivalent status {@code OWNED}. Comparing enum names avoids linking the
 * listener to a constant that does not exist in the other version.</p>
 */
public final class PokedexProgressCompat {
    private PokedexProgressCompat() {
    }

    public static boolean isCaughtOrOwned(Enum<?> progress) {
        if (progress == null) {
            return false;
        }
        String name = progress.name();
        return "CAUGHT".equals(name) || "OWNED".equals(name);
    }
}
