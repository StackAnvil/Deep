package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.mixin.LivingEntityAccessor;
import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public final class NoJumpDelayModule extends Module {

    public NoJumpDelayModule() {
        super("no_jump_delay", "No Jump Delay", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player instanceof LivingEntityAccessor accessor) {
            accessor.deep$setNoJumpDelay(0);
        }
    }
}
