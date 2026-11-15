package net.stackanvil.deep.module.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.stackanvil.deep.module.impl.NotebotModule.NoteBlockPlan;
import net.stackanvil.deep.module.impl.NotebotModule.ScannedNoteBlock;
import net.stackanvil.deep.notebot.NotebotSong;
import net.stackanvil.deep.notebot.SongNote;
import net.stackanvil.deep.test.MinecraftBootstrapExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class NotebotPlanTest {

    private static final BlockPos HARP_BLOCK = new BlockPos(0, 0, 0);
    private static final BlockPos BASS_BLOCK = new BlockPos(1, 0, 0);

    @Test
    void planMatchesNotesToBlocksWithTheRightInstrument() {
        NotebotSong song = NotebotSong.of(Map.of(0, List.of(
                new SongNote(NoteBlockInstrument.BASS, 3),
                new SongNote(NoteBlockInstrument.HARP, 7)
        )), true);
        List<ScannedNoteBlock> blocks = List.of(
                new ScannedNoteBlock(HARP_BLOCK, NoteBlockInstrument.HARP, 0, 1.0),
                new ScannedNoteBlock(BASS_BLOCK, NoteBlockInstrument.BASS, 3, 2.0)
        );

        NoteBlockPlan plan = NoteBlockPlan.create(song, blocks);

        assertTrue(plan.possible());
        assertEquals(List.of(HARP_BLOCK), plan.blocksByNote().get(new SongNote(NoteBlockInstrument.HARP, 7)));
        assertEquals(List.of(BASS_BLOCK), plan.blocksByNote().get(new SongNote(NoteBlockInstrument.BASS, 3)));
        assertEquals(Map.of(HARP_BLOCK, 7), plan.tuneHits());
    }

    @Test
    void specificInstrumentsClaimBlocksBeforeAnyInstrumentNotes() {
        NotebotSong song = NotebotSong.of(Map.of(0, List.of(
                SongNote.any(5),
                new SongNote(NoteBlockInstrument.BASS, 5)
        )), false);
        List<ScannedNoteBlock> blocks = List.of(
                new ScannedNoteBlock(BASS_BLOCK, NoteBlockInstrument.BASS, 5, 1.0),
                new ScannedNoteBlock(HARP_BLOCK, NoteBlockInstrument.HARP, 0, 2.0)
        );

        NoteBlockPlan plan = NoteBlockPlan.create(song, blocks);

        assertTrue(plan.possible());
        assertEquals(List.of(BASS_BLOCK), plan.blocksByNote().get(new SongNote(NoteBlockInstrument.BASS, 5)));
        assertEquals(List.of(HARP_BLOCK), plan.blocksByNote().get(SongNote.any(5)));
    }

    @Test
    void planReportsMissingBlocksPerInstrument() {
        NotebotSong song = NotebotSong.of(Map.of(0, List.of(
                new SongNote(NoteBlockInstrument.FLUTE, 1),
                new SongNote(NoteBlockInstrument.FLUTE, 2),
                new SongNote(NoteBlockInstrument.BELL, 1)
        )), true);

        NoteBlockPlan plan = NoteBlockPlan.create(song, List.of(
                new ScannedNoteBlock(HARP_BLOCK, NoteBlockInstrument.HARP, 0, 1.0)));

        assertFalse(plan.possible());
        assertEquals(Map.of("flute", 2, "bell", 1), plan.shortfall());
        assertEquals("1 bell, 2 flute", NotebotModule.describeShortfall(plan.shortfall()));
    }
}
