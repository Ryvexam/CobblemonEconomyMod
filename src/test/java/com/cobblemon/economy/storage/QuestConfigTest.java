package com.cobblemon.economy.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestConfigTest {
    @TempDir
    File tempDir;

    @Test
    void generatedDefaultQuestsKeepConfiguredPcoRewards() {
        QuestConfig config = QuestConfig.load(new File(tempDir, "quests.json"));

        assertEquals(new BigDecimal("20"), config.quests.get("safari_water_10").rewards.pco);
        assertEquals(new BigDecimal("120"), config.quests.get("safari_shiny_1").rewards.pco);
    }
}
