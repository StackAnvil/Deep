package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class TridentBoostModule extends Module {

    private final NumberSetting strength = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("strength")
            .name("Strength")
            .defaultValue(0.18)
            .min(0.02)
            .max(1.0)
            .step(0.02)
            .build()));

    public TridentBoostModule() {
        super("trident_boost", "Trident Boost", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.isUsingItem() || !player.getUseItem().is(Items.TRIDENT)) {
            return;
        }
        Vec3 look = player.getViewVector(0.0F).normalize().scale(this.strength.value());
        player.setDeltaMovement(player.getDeltaMovement().add(look));
    }
}
