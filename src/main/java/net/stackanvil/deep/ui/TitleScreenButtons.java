package net.stackanvil.deep.ui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.stackanvil.deep.Deep;

import java.util.List;
import java.util.Optional;

/**
 * Puts Realms and Multiplayer side by side on the title screen and adds a full-width Deep menu button in the freed row.
 */
public final class TitleScreenButtons {

    static final String MULTIPLAYER_KEY = "menu.multiplayer";
    static final String REALMS_KEY = "menu.online";
    static final int GAP = 4;

    private TitleScreenButtons() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen) {
                apply(client, screen);
            }
        });
    }

    private static void apply(final Minecraft client, final Screen screen) {
        List<AbstractWidget> widgets = Screens.getWidgets(screen);
        Optional<AbstractWidget> multiplayer = find(widgets, MULTIPLAYER_KEY);
        Optional<AbstractWidget> realms = find(widgets, REALMS_KEY);
        if (multiplayer.isEmpty() || realms.isEmpty()) {
            return;
        }
        Layout layout = layout(new Rect(multiplayer.get().getX(), multiplayer.get().getY(), multiplayer.get().getWidth(), multiplayer.get().getHeight()),
                new Rect(realms.get().getX(), realms.get().getY(), realms.get().getWidth(), realms.get().getHeight()));
        place(realms.get(), layout.realms());
        place(multiplayer.get(), layout.multiplayer());
        Rect menu = layout.menu();
        widgets.add(Button.builder(Component.translatable("menu.deep.client_menu"),
                        button -> client.gui.setScreen(new DeepScreen(Deep.MODULES, Deep.CONFIG)))
                .bounds(menu.x(), menu.y(), menu.width(), menu.height())
                .build());
    }

    /**
     * Splits the multiplayer row between Realms (left) and Multiplayer (right) and gives the Realms row to the menu button.
     */
    static Layout layout(final Rect multiplayer, final Rect realms) {
        int leftWidth = (multiplayer.width() - GAP) / 2;
        int rightWidth = multiplayer.width() - GAP - leftWidth;
        return new Layout(
                new Rect(multiplayer.x(), multiplayer.y(), leftWidth, multiplayer.height()),
                new Rect(multiplayer.x() + leftWidth + GAP, multiplayer.y(), rightWidth, multiplayer.height()),
                new Rect(multiplayer.x(), realms.y(), multiplayer.width(), realms.height())
        );
    }

    private static void place(final AbstractWidget widget, final Rect rect) {
        widget.setX(rect.x());
        widget.setY(rect.y());
        widget.setWidth(rect.width());
    }

    private static Optional<AbstractWidget> find(final List<AbstractWidget> widgets, final String translationKey) {
        return widgets.stream()
                .filter(widget -> widget.getMessage().getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals(translationKey))
                .findFirst();
    }

    record Rect(int x, int y, int width, int height) {
    }

    record Layout(Rect realms, Rect multiplayer, Rect menu) {
    }
}
