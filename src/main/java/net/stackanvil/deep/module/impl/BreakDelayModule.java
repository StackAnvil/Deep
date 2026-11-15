package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.mixin.MinecraftAccessor;
import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class BreakDelayModule extends Module {

    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(0.0)
            .min(0.0)
            .max(6.0)
            .step(1.0)
            .build()));

    public BreakDelayModule() {
        super("break_delay", "Break Delay", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        ((MinecraftAccessor) client).deep$setRightClickDelay(this.delay.value().intValue());
    }
}
