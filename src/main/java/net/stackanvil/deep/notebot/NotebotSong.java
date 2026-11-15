package net.stackanvil.deep.notebot;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A song ready for the Notebot.
 *
 * @param notesByTick      notes grouped by song tick
 * @param lastTick         the last tick that holds notes
 * @param requiredBlocks   how many note blocks each note needs, based on the busiest tick
 * @param gameTickTiming   {@code true} when one song tick equals one game tick, so the play delay setting is ignored
 */
public record NotebotSong(Map<Integer, List<SongNote>> notesByTick, int lastTick,
                          Map<SongNote, Integer> requiredBlocks, boolean gameTickTiming) {

    public static final NotebotSong EMPTY = new NotebotSong(Map.of(), 0, Map.of(), false);

    public static NotebotSong of(final Map<Integer, List<SongNote>> notesByTick, final boolean gameTickTiming) {
        Map<Integer, List<SongNote>> copy = new LinkedHashMap<>();
        notesByTick.forEach((tick, notes) -> {
            if (!notes.isEmpty()) {
                copy.put(tick, List.copyOf(notes));
            }
        });
        if (copy.isEmpty()) {
            return EMPTY;
        }
        int lastTick = copy.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        return new NotebotSong(Map.copyOf(copy), lastTick, requirements(copy), gameTickTiming);
    }

    public boolean empty() {
        return this.notesByTick.isEmpty();
    }

    public int noteCount() {
        return this.notesByTick.values().stream().mapToInt(List::size).sum();
    }

    public int requiredBlockCount() {
        return this.requiredBlocks.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static Map<SongNote, Integer> requirements(final Map<Integer, List<SongNote>> notesByTick) {
        Map<SongNote, Integer> requirements = new HashMap<>();
        for (List<SongNote> notes : notesByTick.values()) {
            Map<SongNote, Integer> counts = new HashMap<>();
            for (SongNote note : notes) {
                counts.merge(note, 1, Integer::sum);
            }
            counts.forEach((note, count) -> requirements.merge(note, count, Math::max));
        }
        return Map.copyOf(requirements);
    }
}
