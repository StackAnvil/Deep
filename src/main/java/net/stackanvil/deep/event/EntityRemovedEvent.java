package net.stackanvil.deep.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;

public record EntityRemovedEvent(Minecraft client, Entity entity, RemovalReason reason) implements DeepEvent {
}
