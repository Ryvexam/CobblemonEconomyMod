package com.cobblemon.economy.storage;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** Safe filesystem primitives shared by the JSON configuration loaders. */
public final class ConfigFileStore {
    private ConfigFileStore() {
    }

    public static void writeAtomically(File target, Gson gson, Object value) throws IOException {
        Path targetPath = target.toPath().toAbsolutePath().normalize();
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        if (Files.isRegularFile(targetPath)) {
            Files.copy(targetPath, targetPath.resolveSibling(targetPath.getFileName() + ".bak"),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        Path temporary = Files.createTempFile(parent, targetPath.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                gson.toJson(value, writer);
            }
            try {
                Files.move(temporary, targetPath, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static File quarantine(File file, String reason) throws IOException {
        Path source = file.toPath().toAbsolutePath().normalize();
        String suffix = "." + reason + "-" + Instant.now().toEpochMilli();
        Path destination = source.resolveSibling(source.getFileName() + suffix);
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.toFile();
    }
}
