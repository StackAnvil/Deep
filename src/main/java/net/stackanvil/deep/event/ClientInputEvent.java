package net.stackanvil.deep.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;

public record ClientInputEvent(Minecraft client, ClientInput input) implements DeepEvent {
}
