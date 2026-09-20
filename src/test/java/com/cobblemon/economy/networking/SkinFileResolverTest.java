package com.cobblemon.economy.networking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinFileResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesOnlyPngFilesInsideSkinDirectory() throws Exception {
        Path skins = Files.createDirectory(tempDir.resolve("skins"));
        Path skin = Files.write(skins.resolve("trainer_blue.png"), new byte[]{1, 2, 3});

        assertEquals(skin.toRealPath(), SkinFileResolver.resolve(skins, "trainer_blue").orElseThrow());
        assertEquals(skin.toRealPath(), SkinFileResolver.resolve(skins, "trainer_blue.png").orElseThrow());
    }

    @Test
    void rejectsTraversalAbsolutePathsAndUnsupportedNames() throws Exception {
        Path skins = Files.createDirectory(tempDir.resolve("skins"));
        Files.write(tempDir.resolve("secret.png"), new byte[]{1});

        assertTrue(SkinFileResolver.resolve(skins, "../secret").isEmpty());
        assertTrue(SkinFileResolver.resolve(skins, tempDir.resolve("secret.png").toString()).isEmpty());
        assertTrue(SkinFileResolver.resolve(skins, "nested/skin").isEmpty());
        assertTrue(SkinFileResolver.resolve(skins, "skin.jpg").isEmpty());
    }

    @Test
    void rejectsOversizedFiles() throws Exception {
        Path skins = Files.createDirectory(tempDir.resolve("skins"));
        Path oversized = skins.resolve("oversized.png");
        try (var output = Files.newOutputStream(oversized)) {
            output.write(new byte[(int) SkinFileResolver.MAX_SKIN_BYTES + 1]);
        }

        assertTrue(SkinFileResolver.resolve(skins, "oversized").isEmpty());
    }

    @Test
    void rejectsSymlinksThatEscapeSkinDirectory() throws Exception {
        Path skins = Files.createDirectory(tempDir.resolve("skins"));
        Path outside = Files.write(tempDir.resolve("outside.png"), new byte[]{1});
        Files.createSymbolicLink(skins.resolve("linked.png"), outside);

        assertTrue(SkinFileResolver.resolve(skins, "linked").isEmpty());
    }
}
