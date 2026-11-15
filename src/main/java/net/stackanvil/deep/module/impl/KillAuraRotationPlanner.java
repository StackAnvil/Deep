package net.stackanvil.deep.module.impl;

import net.stackanvil.deep.module.Module;
import net.stackanvil.deep.rotation.Rotation;
import net.stackanvil.deep.rotation.RotationManager;
import net.stackanvil.deep.rotation.RotationRequest;
import net.stackanvil.deep.rotation.RotationTurnMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

final class KillAuraRotationPlanner {

    void request(final Module owner, final LocalPlayer player, final LivingEntity entity, final float maxTurnDegrees,
                 final float resetThreshold, final RotationTurnMode turnMode, final boolean pauseInInventory) {
        Rotation targetRotation = Rotation.lookingAt(entity.getBoundingBox().getCenter(), player.getEyePosition());
        RotationManager.request(new RotationRequest(
                owner.id(),
                targetRotation,
                100,
                maxTurnDegrees,
                2,
                resetThreshold,
                turnMode,
                pauseInInventory
        ));
        RotationManager.apply(player);
    }

    void clear(final Module owner) {
        RotationManager.clear(owner.id());
    }

    static RotationTurnMode turnMode(final String value) {
        return switch (value) {
            case "Instant" -> RotationTurnMode.INSTANT;
            case "Linear" -> RotationTurnMode.LINEAR;
            default -> RotationTurnMode.STEPPED;
        };
    }
}
