package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.module.ModuleCategory;
import net.stackanvil.deep.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.phys.EntityHitResult;

public final class EndermanLookModule extends Module {

    private final NumberSetting pitch = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("pitch")
            .name("Pitch")
            .defaultValue(45.0)
            .min(-90.0)
            .max(90.0)
            .step(1.0)
            .build()));

    public EndermanLookModule() {
        super("enderman_look", "Enderman Look", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof Enderman)) {
            return;
        }
        player.setXRot(this.pitch.value().floatValue());
    }
}
