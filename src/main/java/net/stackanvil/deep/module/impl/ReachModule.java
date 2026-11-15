package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.NumberSetting;

public final class ReachModule extends Module {

    private static double blockBonus;
    private static double entityBonus;

    private final NumberSetting block = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("block")
            .name("Block")
            .defaultValue(1.0)
            .min(0.0)
            .max(6.0)
            .step(0.1)
            .build()));
    private final NumberSetting entity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("entity")
            .name("Entity")
            .defaultValue(1.0)
            .min(0.0)
            .max(6.0)
            .step(0.1)
            .build()));

    public ReachModule() {
        super("reach", "Reach", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final net.minecraft.client.Minecraft client) {
        blockBonus = this.block.value();
        entityBonus = this.entity.value();
    }

    @Override
    protected void onDisable() {
        blockBonus = 0.0;
        entityBonus = 0.0;
    }

    public static double blockBonus() {
        return blockBonus;
    }

    public static double entityBonus() {
        return entityBonus;
    }
}
