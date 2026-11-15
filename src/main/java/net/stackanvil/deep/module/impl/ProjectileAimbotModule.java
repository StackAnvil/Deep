package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.projectile.ProjectileAim;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.Optional;

public final class ProjectileAimbotModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(48.0)
            .min(4.0)
            .max(128.0)
            .step(4.0)
            .build()));
    private final BooleanSetting hostiles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hostiles")
            .name("Hostiles")
            .defaultValue(false)
            .build()));

    public ProjectileAimbotModule() {
        super("projectile_aimbot", "Projectile Aimbot", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || !isProjectile(player.getMainHandItem())) {
            RotationManager.clear(this.id());
            return;
        }
        TargetPolicy policy = TargetPolicy.of(true, this.hostiles.value(), false, true, true, true, true);
        Optional<LivingEntity> target = TargetQuery.closest(client.level.entitiesForRendering(), player, policy,
                this.range.value(), Comparator.comparingDouble(player::distanceToSqr));
        target.ifPresentOrElse(entity -> RotationManager.request(new RotationRequest(
                this.id(),
                ProjectileAim.rotationToHit(player.getEyePosition(), entity, ProjectileAim.throwableVelocity(projectileType(player.getMainHandItem())),
                        ProjectileAim.gravity(projectileType(player.getMainHandItem()))),
                70,
                35.0F,
                2,
                2.0F,
                RotationTurnMode.STEPPED,
                true
        )), () -> RotationManager.clear(this.id()));
    }

    static boolean isProjectile(final ItemStack stack) {
        return stack.is(Items.ENDER_PEARL)
                || stack.is(Items.SNOWBALL)
                || stack.is(Items.EGG)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.TRIDENT);
    }

    private static String projectileType(final ItemStack stack) {
        if (stack.is(Items.TRIDENT)) {
            return "trident";
        }
        if (stack.is(Items.SPLASH_POTION)) {
            return "potion";
        }
        return "throwable";
    }
}
