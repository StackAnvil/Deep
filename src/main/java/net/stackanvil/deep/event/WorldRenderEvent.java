package net.stackanvil.deep.event;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

public record WorldRenderEvent(LevelRenderContext context) implements DeepEvent {
}
