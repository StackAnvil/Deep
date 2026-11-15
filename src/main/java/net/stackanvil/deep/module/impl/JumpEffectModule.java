package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.render.CuboidMarker;
import net.stackanvil.deep.render.MarkerManager;
import net.stackanvil.deep.render.MarkerStyle;
import net.minecraft.client.Minecraft;

public final class JumpEffectModule extends Module {

    private boolean wasOnGround;

    public JumpEffectModule() {
        super("jump_effect", "Jump Effect", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null) {
            this.wasOnGround = false;
            return;
        }
        boolean onGround = client.player.onGround();
        if (this.wasOnGround && !onGround && client.player.getDeltaMovement().y > 0.0) {
            MarkerManager.put(new CuboidMarker("jump_effect:" + client.player.tickCount,
                    client.player.getBoundingBox().inflate(0.15).move(0.0, -0.2, 0.0), MarkerStyle.CYAN, 12));
        }
        this.wasOnGround = onGround;
    }
}
