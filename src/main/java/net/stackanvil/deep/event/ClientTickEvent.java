package net.stackanvil.deep.event;

import net.minecraft.client.Minecraft;

public record ClientTickEvent(Minecraft client) implements DeepEvent {
}
