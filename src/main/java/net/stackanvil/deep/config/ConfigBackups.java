package net.stackanvil.deep.config;

import net.fabricmc.loader.api.FabricLoader;
import net.stackanvil.deep.Deep;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zips the client's config files into timestamped archives and keeps only the newest ones.
 */
public final class ConfigBackups {

    public static final int DEFAULT_KEEP = 50;
    static final List<String> DEFAULT_FILES = List.of(
            Deep.MOD_ID + ".json",
            Deep.MOD_ID + "-friends.txt",
            Deep.MOD_ID + "-hud.json",
            Deep.MOD_ID + "-server-profiles.json",
            Deep.MOD_ID + "/profiles.json",
            Deep.MOD_ID + "/waypoints.json",
            Deep.MOD_ID + "/seeds.json",
            Deep.MOD_ID + "/accounts.json"
    );
    private static final String PREFIX = "config-";
    private static final String SUFFIX = ".zip";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.ROOT);

    private final Path configDir;
    private final List<String> files;
    private final Path backupDir;
    private final int keep;

    public ConfigBackups(final Path configDir, final List<String> files, final Path backupDir, final int keep) {
        if (keep < 1) {
            throw new IllegalArgumentException("keep must be at least 1");
        }
        this.configDir = configDir;
        this.files = List.copyOf(files);
        this.backupDir = backupDir;
        this.keep = keep;
    }

    public static ConfigBackups defaults() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        return new ConfigBackups(configDir, DEFAULT_FILES, configDir.resolve(Deep.MOD_ID).resolve("backups"), DEFAULT_KEEP);
    }

    /**
     * Writes a new backup archive and prunes old ones.
     *
     * @return the new archive, or empty when none of the config files exist yet
     */
    public Optional<Path> backup(final Clock clock) throws IOException {
        List<String> existing = this.files.stream()
                .filter(file -> Files.isRegularFile(this.configDir.resolve(file)))
                .toList();
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        Files.createDirectories(this.backupDir);
        Path archive = this.backupDir.resolve(PREFIX + LocalDateTime.now(clock).format(TIMESTAMP) + SUFFIX);
        Path partial = archive.resolveSibling(archive.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(partial);
             ZipOutputStream zip = new ZipOutputStream(output)) {
            for (String file : existing) {
                zip.putNextEntry(new ZipEntry(file));
                Files.copy(this.configDir.resolve(file), zip);
                zip.closeEntry();
            }
        } catch (IOException exception) {
            Files.deleteIfExists(partial);
            throw exception;
        }
        Files.move(partial, archive);
        this.prune();
        return Optional.of(archive);
    }

    /**
     * Lists backup archives, newest first.
     */
    public List<Path> backups() throws IOException {
        if (!Files.isDirectory(this.backupDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(this.backupDir)) {
            List<Path> archives = new ArrayList<>(stream
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith(PREFIX) && name.endsWith(SUFFIX);
                    })
                    .toList());
            archives.sort(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed());
            return List.copyOf(archives);
        }
    }

    private void prune() throws IOException {
        List<Path> archives = this.backups();
        for (int i = this.keep; i < archives.size(); i++) {
            Files.deleteIfExists(archives.get(i));
        }
    }
}
