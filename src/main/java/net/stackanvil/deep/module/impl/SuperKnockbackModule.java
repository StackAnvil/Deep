package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class SuperKnockbackModule extends Module {

    private final BooleanSetting resetSprint = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("reset_sprint")
            .name("Reset")
            .defaultValue(true)
            .build()));

    public SuperKnockbackModule() {
        super("super_knockback", "Super Knockback", ModuleCategory.COMBAT);
    }

    @Override
    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        if (this.resetSprint.value() && player != null && player.isSprinting()) {
            player.setSprinting(false);
            player.setSprinting(true);
        }
        return false;
    }
}
