package net.stackanvil.deep.event;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public record BlockBreakingProgressEvent(Minecraft client, int breakerId, BlockPos pos, int progress) implements DeepEvent {
}
