package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class SlippyModule extends Module {

    private final NumberSetting multiplier = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("multiplier")
            .name("Multiplier")
            .defaultValue(1.08)
            .min(1.0)
            .max(1.4)
            .step(0.01)
            .build()));

    public SlippyModule() {
        super("slippy", "Slippy", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.onGround() || player.isInWater() || player.isInLava() || !MovementVelocity.moving(client)) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x * this.multiplier.value(), movement.y, movement.z * this.multiplier.value());
    }
}
