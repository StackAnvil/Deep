package net.stackanvil.deep.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public record HudRenderEvent(Minecraft client, GuiGraphicsExtractor graphics) implements DeepEvent {
}
