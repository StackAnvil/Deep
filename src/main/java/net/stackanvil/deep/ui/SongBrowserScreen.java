package net.stackanvil.deep.ui;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.stackanvil.deep.module.impl.NotebotModule;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Lists the songs in the songs folder and starts the Notebot on the one you pick.
 */
public final class SongBrowserScreen extends Screen {

    static final int ROWS = 8;
    private static final int WIDTH = 300;
    private static final int ROW_HEIGHT = 22;

    private final @Nullable Screen parent;
    private final NotebotModule notebot;
    private List<String> songs = List.of();
    private @Nullable String loadError;
    private String filter = "";
    private int page;
    private @Nullable EditBox search;

    public SongBrowserScreen(final @Nullable Screen parent, final NotebotModule notebot) {
        super(Component.translatable("screen.deep.song_browser"));
        this.parent = parent;
        this.notebot = notebot;
    }

    @Override
    protected void init() {
        try {
            this.songs = this.notebot.songFiles().list();
            this.loadError = null;
        } catch (IOException exception) {
            this.songs = List.of();
            this.loadError = exception.getMessage();
        }
        int left = this.width / 2 - WIDTH / 2;
        int top = 32;
        this.addRenderableWidget(new StringWidget(left, 12, WIDTH, 12, this.title, this.font));

        this.search = new EditBox(this.font, left, top, WIDTH, 20, this.search,
                Component.translatable("screen.deep.song_browser.search"));
        this.search.setHint(Component.translatable("screen.deep.song_browser.search"));
        this.search.setValue(this.filter);
        this.search.setResponder(value -> {
            if (!value.equals(this.filter)) {
                this.filter = value;
                this.page = 0;
                this.rebuildWidgets();
            }
        });
        this.addRenderableWidget(this.search);
        this.setInitialFocus(this.search);

        List<String> visible = filter(this.songs, this.filter);
        int pages = pageCount(visible.size());
        this.page = Math.max(0, Math.min(this.page, pages - 1));
        int rowTop = top + 26;
        if (visible.isEmpty()) {
            Component message = this.loadError != null
                    ? Component.translatable("screen.deep.song_browser.error", this.loadError)
                    : Component.translatable("screen.deep.song_browser.empty");
            this.addRenderableWidget(new StringWidget(left, rowTop + 6, WIDTH, 12, message, this.font));
        }
        List<String> pageSongs = pageItems(visible, this.page);
        for (int index = 0; index < pageSongs.size(); index++) {
            String song = pageSongs.get(index);
            String label = song.equals(this.notebot.selectedSong()) ? "> " + song : song;
            this.addRenderableWidget(Button.builder(Component.literal(label), button -> this.play(song))
                    .bounds(left, rowTop + index * ROW_HEIGHT, WIDTH, 20)
                    .build());
        }

        int footer = rowTop + ROWS * ROW_HEIGHT + 4;
        Button previous = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            this.page--;
            this.rebuildWidgets();
        }).bounds(left, footer, 20, 20).build());
        previous.active = this.page > 0;
        this.addRenderableWidget(new StringWidget(left + 24, footer + 6, 60, 12,
                Component.literal((this.page + 1) + "/" + pages), this.font));
        Button next = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            this.page++;
            this.rebuildWidgets();
        }).bounds(left + 88, footer, 20, 20).build());
        next.active = this.page < pages - 1;
        this.addRenderableWidget(Button.builder(Component.translatable("screen.deep.song_browser.open_folder"),
                        button -> Blaze3D.openPath(this.notebot.songFiles().directory()))
                .bounds(left + 112, footer, 92, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(left + 208, footer, 92, 20)
                .build());
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    private void play(final String song) {
        this.notebot.selectSong(song);
        if (!this.notebot.enabled()) {
            this.notebot.enabled(true);
        }
        this.onClose();
    }

    static List<String> filter(final List<String> songs, final String query) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return songs;
        }
        return songs.stream().filter(song -> song.toLowerCase(Locale.ROOT).contains(needle)).toList();
    }

    static int pageCount(final int size) {
        return Math.max(1, (size + ROWS - 1) / ROWS);
    }

    static List<String> pageItems(final List<String> songs, final int page) {
        int from = Math.min(songs.size(), page * ROWS);
        return songs.subList(from, Math.min(songs.size(), from + ROWS));
    }
}
