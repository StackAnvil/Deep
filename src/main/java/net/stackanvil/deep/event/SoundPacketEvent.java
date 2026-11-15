package net.stackanvil.deep.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;

public record SoundPacketEvent(Minecraft client, ClientboundSoundPacket packet) implements DeepEvent {
}
