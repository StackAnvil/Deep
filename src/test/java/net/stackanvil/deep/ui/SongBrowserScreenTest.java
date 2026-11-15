package net.stackanvil.deep.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SongBrowserScreenTest {

    @Test
    void filterMatchesSongNamesCaseInsensitively() {
        List<String> songs = List.of("pop/Megalovania.nbs", "Tetris.nbs", "classic/ode.mid");

        assertEquals(List.of("pop/Megalovania.nbs"), SongBrowserScreen.filter(songs, " mega "));
        assertEquals(songs, SongBrowserScreen.filter(songs, ""));
    }

    @Test
    void pagingSplitsSongsIntoRowsAndKeepsOnePageWhenEmpty() {
        List<String> songs = IntStream.range(0, SongBrowserScreen.ROWS + 3).mapToObj(i -> "song" + i).toList();

        assertEquals(2, SongBrowserScreen.pageCount(songs.size()));
        assertEquals(1, SongBrowserScreen.pageCount(0));
        assertEquals(List.of("song8", "song9", "song10"), SongBrowserScreen.pageItems(songs, 1));
        assertEquals(List.of(), SongBrowserScreen.pageItems(List.of(), 0));
    }
}
