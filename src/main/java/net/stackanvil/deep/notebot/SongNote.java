package net.stackanvil.deep.notebot;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.jetbrains.annotations.Nullable;

/**
 * One note to play: a note block key from 0 to 24 and the instrument it needs.
 *
 * @param instrument the required instrument, or {@code null} when any tunable note block works
 * @param key        the note block key, clamped to 0..24
 */
public record SongNote(@Nullable NoteBlockInstrument instrument, int key) {

    public SongNote {
        key = Math.max(0, Math.min(24, key));
    }

    public static SongNote any(final int key) {
        return new SongNote(null, key);
    }

    public boolean accepts(final NoteBlockInstrument blockInstrument) {
        return this.instrument == null || this.instrument == blockInstrument;
    }
}
