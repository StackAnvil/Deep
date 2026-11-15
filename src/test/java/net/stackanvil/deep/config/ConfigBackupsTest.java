package net.stackanvil.deep.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigBackupsTest {

    private static final Instant START = Instant.parse("2026-11-15T12:00:00Z");

    @TempDir
    private Path tempDir;

    @Test
    void backupZipsExistingFilesWithRelativeNames() throws IOException {
        Files.writeString(this.tempDir.resolve("deep.json"), "{\"modules\":{}}");
        Files.createDirectories(this.tempDir.resolve("deep"));
        Files.writeString(this.tempDir.resolve("deep/waypoints.json"), "[]");
        ConfigBackups backups = this.backups(50);

        Optional<Path> archive = backups.backup(clockAt(START));

        assertTrue(archive.isPresent());
        assertEquals("config-20261115-120000-000.zip", archive.get().getFileName().toString());
        assertEquals(Map.of("deep.json", "{\"modules\":{}}", "deep/waypoints.json", "[]"), readZip(archive.get()));
    }

    @Test
    void backupSkipsWhenNoConfigExists() throws IOException {
        ConfigBackups backups = this.backups(50);

        assertFalse(backups.backup(clockAt(START)).isPresent());
        assertEquals(List.of(), backups.backups());
    }

    @Test
    void backupKeepsOnlyTheNewestArchives() throws IOException {
        Files.writeString(this.tempDir.resolve("deep.json"), "{}");
        ConfigBackups backups = this.backups(3);

        for (int i = 0; i < 5; i++) {
            backups.backup(clockAt(START.plus(Duration.ofMinutes(i))));
        }

        List<String> names = backups.backups().stream().map(path -> path.getFileName().toString()).toList();
        assertEquals(List.of(
                "config-20261115-120400-000.zip",
                "config-20261115-120300-000.zip",
                "config-20261115-120200-000.zip"
        ), names);
    }

    private ConfigBackups backups(final int keep) {
        return new ConfigBackups(this.tempDir, List.of("deep.json", "deep/waypoints.json", "deep/accounts.json"),
                this.tempDir.resolve("deep/backups"), keep);
    }

    private static Clock clockAt(final Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static Map<String, String> readZip(final Path archive) throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (InputStream input = Files.newInputStream(archive);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }
}
