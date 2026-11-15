package net.stackanvil.deep.notebot;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.data.MinecraftInstrument;
import net.raphimc.noteblocklib.format.SongFormat;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsSong;
import net.raphimc.noteblocklib.model.Note;
import net.stackanvil.deep.test.MinecraftBootstrapExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class SongFilesTest {

    @TempDir
    private Path tempDir;

    @Test
    void fromSongResamplesToGameTicksAndMapsInstruments() {
        NbsSong song = new NbsSong();
        song.getTempoEvents().set(0, 10F);
        song.getNotes().add(0, note(MinecraftInstrument.HARP, 12));
        song.getNotes().add(1, note(MinecraftInstrument.BASS_DRUM, 0));

        NotebotSong result = SongFiles.fromSong(song);

        assertEquals(Map.of(
                0, List.of(new SongNote(NoteBlockInstrument.HARP, 12)),
                2, List.of(new SongNote(NoteBlockInstrument.BASEDRUM, 0))
        ), result.notesByTick());
        assertTrue(result.gameTickTiming());
    }

    @Test
    void fromSongSkipsCustomInstrumentsAndSilentNotes() {
        NbsSong song = new NbsSong();
        song.getTempoEvents().set(0, 20F);
        song.getNotes().add(0, new Note().setInstrument(new NbsCustomInstrument()).setMcKey(5));
        song.getNotes().add(0, note(MinecraftInstrument.FLUTE, 7).setVolume(0F));
        song.getNotes().add(1, note(MinecraftInstrument.BELL, 3));

        NotebotSong result = SongFiles.fromSong(song);

        assertEquals(Map.of(1, List.of(new SongNote(NoteBlockInstrument.BELL, 3))), result.notesByTick());
    }

    @Test
    void fromSongMergesDuplicateNotesInOneTick() {
        NbsSong song = new NbsSong();
        song.getTempoEvents().set(0, 20F);
        song.getNotes().add(0, note(MinecraftInstrument.HARP, 4));
        song.getNotes().add(0, note(MinecraftInstrument.HARP, 4));

        NotebotSong result = SongFiles.fromSong(song);

        assertEquals(Map.of(new SongNote(NoteBlockInstrument.HARP, 4), 1), result.requiredBlocks());
    }

    @Test
    void loadReadsNbsFilesFromTheSongsFolder() throws Exception {
        NbsSong song = new NbsSong();
        song.getTempoEvents().set(0, 20F);
        song.getNotes().add(0, note(MinecraftInstrument.GUITAR, 9));
        Files.createDirectories(this.tempDir.resolve("pop"));
        NoteBlockLib.writeSong(NoteBlockLib.convertSong(song, SongFormat.NBS), this.tempDir.resolve("pop/test.nbs"));
        SongFiles files = new SongFiles(this.tempDir);

        NotebotSong loaded = files.load("pop/test.nbs");

        assertEquals(List.of("pop/test.nbs"), files.list());
        assertEquals(Map.of(0, List.of(new SongNote(NoteBlockInstrument.GUITAR, 9))), loaded.notesByTick());
    }

    @Test
    void resolveRejectsPathsOutsideTheSongsFolder() {
        SongFiles files = new SongFiles(this.tempDir.resolve("songs"));

        assertThrows(IllegalArgumentException.class, () -> files.resolve("../deep.json"));
    }

    @Test
    void everyMinecraftInstrumentHasANoteBlockInstrument() {
        for (MinecraftInstrument instrument : MinecraftInstrument.values()) {
            assertTrue(SongFiles.instrument(instrument).isTunable(), instrument.name());
        }
    }

    private static Note note(final MinecraftInstrument instrument, final int mcKey) {
        return new Note().setInstrument(instrument).setMcKey(mcKey).setVolume(1F);
    }
}
