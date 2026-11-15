package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;

public final class NoSlowBreakModule extends Module {

    private final BooleanSetting miningFatigue = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("mining_fatigue")
            .name("Fatigue")
            .defaultValue(true)
            .build()));

    public NoSlowBreakModule() {
        super("no_slow_break", "No Slow Break", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player != null && this.miningFatigue.value() && player.hasEffect(MobEffects.MINING_FATIGUE)) {
            player.removeEffect(MobEffects.MINING_FATIGUE);
        }
    }
}
