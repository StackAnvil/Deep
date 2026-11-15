package net.stackanvil.deep.notebot;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.data.MinecraftDefinitions;
import net.raphimc.noteblocklib.data.MinecraftInstrument;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocklib.util.SongResampler;
import net.stackanvil.deep.Deep;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Lists and loads note block songs from the songs folder.
 */
public final class SongFiles {

    public static final Set<String> EXTENSIONS = Set.of("nbs", "mid", "midi", "txt", "mcsp", "mcsp2");
    private static final float GAME_TICKS_PER_SECOND = 20F;

    private final Path directory;

    public SongFiles(final Path directory) {
        this.directory = directory;
    }

    public static SongFiles defaults() {
        return new SongFiles(FabricLoader.getInstance().getConfigDir().resolve(Deep.MOD_ID).resolve("songs"));
    }

    public Path directory() {
        return this.directory;
    }

    /**
     * Lists song file names relative to the songs folder, sorted case-insensitively.
     */
    public List<String> list() throws IOException {
        Files.createDirectories(this.directory);
        try (Stream<Path> stream = Files.walk(this.directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> EXTENSIONS.contains(extension(path)))
                    .map(path -> this.directory.relativize(path).toString().replace('\\', '/'))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
    }

    /**
     * Resolves a song name inside the songs folder and rejects paths that leave it.
     */
    public Path resolve(final String name) {
        Path resolved = this.directory.resolve(name).normalize();
        if (!resolved.startsWith(this.directory.normalize())) {
            throw new IllegalArgumentException("Song path leaves the songs folder: " + name);
        }
        return resolved;
    }

    public NotebotSong load(final String name) throws Exception {
        return fromSong(NoteBlockLib.readSong(this.resolve(name)));
    }

    /**
     * Converts a NoteBlockLib song to Notebot notes. The song is resampled to 20 ticks per second so one song tick
     * plays per game tick. Notes outside the note block range move to a higher or lower instrument first and are
     * clamped after that. Custom instruments are skipped because no note block can play them.
     */
    public static NotebotSong fromSong(final Song song) {
        SongResampler.changeTickSpeed(song, GAME_TICKS_PER_SECOND);
        song.getNotes().removeSilentNotes();
        song.getNotes().forEach(MinecraftDefinitions::instrumentShiftNote);
        song.getNotes().forEach(MinecraftDefinitions::clampNoteKey);
        song.getNotes().removeDoubleNotes();
        Map<Integer, List<SongNote>> notesByTick = new TreeMap<>();
        for (int tick : song.getNotes().getTicks()) {
            for (Note note : song.getNotes().get(tick)) {
                if (note.getInstrument() instanceof MinecraftInstrument instrument) {
                    notesByTick.computeIfAbsent(tick, ignored -> new ArrayList<>())
                            .add(new SongNote(instrument(instrument), note.getMcKey()));
                }
            }
        }
        return NotebotSong.of(notesByTick, true);
    }

    static NoteBlockInstrument instrument(final MinecraftInstrument instrument) {
        return switch (instrument) {
            case HARP -> NoteBlockInstrument.HARP;
            case BASS -> NoteBlockInstrument.BASS;
            case BASS_DRUM -> NoteBlockInstrument.BASEDRUM;
            case SNARE -> NoteBlockInstrument.SNARE;
            case HAT -> NoteBlockInstrument.HAT;
            case GUITAR -> NoteBlockInstrument.GUITAR;
            case FLUTE -> NoteBlockInstrument.FLUTE;
            case BELL -> NoteBlockInstrument.BELL;
            case CHIME -> NoteBlockInstrument.CHIME;
            case XYLOPHONE -> NoteBlockInstrument.XYLOPHONE;
            case IRON_XYLOPHONE -> NoteBlockInstrument.IRON_XYLOPHONE;
            case COW_BELL -> NoteBlockInstrument.COW_BELL;
            case DIDGERIDOO -> NoteBlockInstrument.DIDGERIDOO;
            case BIT -> NoteBlockInstrument.BIT;
            case BANJO -> NoteBlockInstrument.BANJO;
            case PLING -> NoteBlockInstrument.PLING;
        };
    }

    private static String extension(final Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
