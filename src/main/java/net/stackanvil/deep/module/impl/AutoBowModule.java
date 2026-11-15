package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.projectile.ProjectileAim;
import net.stackanvil.deep.rotation.RotationManager;
import net.stackanvil.deep.rotation.RotationRequest;
import net.stackanvil.deep.rotation.RotationTurnMode;
import net.stackanvil.deep.setting.NumberSetting;
import net.stackanvil.deep.target.TargetPolicy;
import net.stackanvil.deep.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

import java.util.Comparator;

public final class AutoBowModule extends Module {

    private final NumberSetting chargeTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("charge_ticks")
            .name("Charge")
            .defaultValue(18.0)
            .min(3.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(64.0)
            .min(8.0)
            .max(128.0)
            .step(4.0)
            .build()));

    public AutoBowModule() {
        super("auto_bow", "Auto Bow", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null
                || !player.getMainHandItem().is(Items.BOW) || !player.isUsingItem()) {
            return;
        }
        TargetQuery.closest(client.level.entitiesForRendering(), player,
                TargetPolicy.of(true, false, false, true, true, true, true),
                this.range.value(), Comparator.comparingDouble(player::distanceToSqr))
                .ifPresent(target -> {
                    RotationManager.request(new RotationRequest(this.id(),
                            ProjectileAim.rotationToHit(player.getEyePosition(), target, 3.0, 0.05),
                            90, 60.0F, 1, 1.0F, RotationTurnMode.STEPPED, true));
                    RotationManager.apply(player);
                    if (player.getTicksUsingItem() >= this.chargeTicks.value()) {
                        client.gameMode.releaseUsingItem(player);
                    }
                });
    }
}
