package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.NumberSetting;
import net.stackanvil.deep.target.TargetPolicy;
import net.stackanvil.deep.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

import java.util.Comparator;

public final class MaceKillModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.5)
            .min(2.0)
            .max(8.0)
            .step(0.25)
            .build()));
    private final NumberSetting minFall = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_fall")
            .name("Fall")
            .defaultValue(3.0)
            .min(0.0)
            .max(30.0)
            .step(0.5)
            .build()));

    public MaceKillModule() {
        super("mace_kill", "Mace Kill", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null
                || !player.getMainHandItem().is(Items.MACE)
                || player.fallDistance < this.minFall.value()
                || player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        TargetQuery.closest(client.level.entitiesForRendering(), player,
                TargetPolicy.of(true, true, false, true, true, true, true),
                this.range.value(), Comparator.comparingDouble(player::distanceToSqr))
                .ifPresent(target -> {
                    client.gameMode.attack(player, target);
                    player.swing(InteractionHand.MAIN_HAND);
                });
    }
}
