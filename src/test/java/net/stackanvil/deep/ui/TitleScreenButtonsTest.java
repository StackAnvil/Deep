package net.stackanvil.deep.ui;

import net.stackanvil.deep.ui.TitleScreenButtons.Layout;
import net.stackanvil.deep.ui.TitleScreenButtons.Rect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TitleScreenButtonsTest {

    @Test
    void layoutSplitsMultiplayerRowAndGivesRealmsRowToMenuAndAccounts() {
        Layout layout = TitleScreenButtons.layout(new Rect(220, 154, 200, 20), new Rect(220, 178, 200, 20));

        assertEquals(new Rect(220, 154, 98, 20), layout.realms());
        assertEquals(new Rect(322, 154, 98, 20), layout.multiplayer());
        assertEquals(new Rect(220, 178, 98, 20), layout.menu());
        assertEquals(new Rect(322, 178, 98, 20), layout.accounts());
    }

    @Test
    void layoutKeepsTotalWidthForOddWidths() {
        Layout layout = TitleScreenButtons.layout(new Rect(0, 0, 201, 20), new Rect(0, 24, 201, 20));

        assertEquals(201, layout.realms().width() + TitleScreenButtons.GAP + layout.multiplayer().width());
        assertEquals(201, layout.multiplayer().x() + layout.multiplayer().width());
    }
}
