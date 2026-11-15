package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.rotation.Rotation;
import net.stackanvil.deep.rotation.RotationManager;
import net.stackanvil.deep.rotation.RotationRequest;
import net.stackanvil.deep.rotation.RotationTurnMode;
import net.stackanvil.deep.setting.BooleanSetting;
import net.stackanvil.deep.setting.NumberSetting;
import net.stackanvil.deep.target.TargetPolicy;
import net.stackanvil.deep.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.Optional;

public final class BowAimbotModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(64.0)
            .min(8.0)
            .max(128.0)
            .step(4.0)
            .build()));
    private final BooleanSetting players = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("players")
            .name("Players")
            .defaultValue(true)
            .build()));
    private final BooleanSetting hostiles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hostiles")
            .name("Hostiles")
            .defaultValue(false)
            .build()));

    public BowAimbotModule() {
        super("bow_aimbot", "Bow Aimbot", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || !isAiming(player)) {
            RotationManager.clear(this.id());
            return;
        }
        TargetPolicy policy = TargetPolicy.of(this.players.value(), this.hostiles.value(), false,
                true, true, true, true);
        Optional<LivingEntity> target = TargetQuery.closest(
                client.level.entitiesForRendering(),
                player,
                policy,
                this.range.value(),
                Comparator.comparingDouble(player::distanceToSqr)
        );
        target.ifPresentOrElse(entity -> RotationManager.request(new RotationRequest(
                this.id(),
                Rotation.lookingAt(entity.getBoundingBox().getCenter(), player.getEyePosition()),
                75,
                30.0F,
                2,
                2.0F,
                RotationTurnMode.STEPPED,
                true
        )), () -> RotationManager.clear(this.id()));
    }

    static boolean isAiming(final LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        return stack.is(Items.BOW) && player.isUsingItem()
                || stack.is(Items.CROSSBOW) && (player.isUsingItem() || CrossbowItem.isCharged(stack));
    }
}
