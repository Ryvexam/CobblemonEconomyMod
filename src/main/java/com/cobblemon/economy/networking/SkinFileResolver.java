package com.cobblemon.economy.networking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class SkinFileResolver {
    public static final long MAX_SKIN_BYTES = 1024L * 1024L;
    private static final Pattern SKIN_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,63}(?:\\.png)?");

    private SkinFileResolver() {
    }

    public static Optional<Path> resolve(Path skinDirectory, String requestedName) throws IOException {
        if (skinDirectory == null || requestedName == null || !SKIN_NAME.matcher(requestedName).matches()) {
            return Optional.empty();
        }

        String fileName = requestedName.toLowerCase(Locale.ROOT).endsWith(".png")
                ? requestedName
                : requestedName + ".png";
        Path normalizedDirectory = skinDirectory.toAbsolutePath().normalize();
        Path candidate = normalizedDirectory.resolve(fileName).normalize();
        if (!candidate.startsWith(normalizedDirectory) || !Files.isRegularFile(candidate)) {
            return Optional.empty();
        }

        Path realDirectory = normalizedDirectory.toRealPath();
        Path realCandidate = candidate.toRealPath();
        if (!realCandidate.startsWith(realDirectory) || Files.size(realCandidate) > MAX_SKIN_BYTES) {
            return Optional.empty();
        }
        return Optional.of(realCandidate);
    }
}
