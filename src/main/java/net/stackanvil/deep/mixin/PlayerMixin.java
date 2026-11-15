package net.stackanvil.deep.mixin;

import net.stackanvil.deep.Deep;
import net.stackanvil.deep.event.PreventEdgeFallEvent;
import net.stackanvil.deep.module.impl.ReachModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    private void deep$preventEdgeFall(final CallbackInfoReturnable<Boolean> info) {
        PreventEdgeFallEvent event = Deep.MODULES.call(new PreventEdgeFallEvent(Minecraft.getInstance(), (Player) (Object) this));
        if (event.isCancelled()) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "blockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void deep$extendBlockReach(final CallbackInfoReturnable<Double> info) {
        info.setReturnValue(info.getReturnValue() + ReachModule.blockBonus());
    }

    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void deep$extendEntityReach(final CallbackInfoReturnable<Double> info) {
        info.setReturnValue(info.getReturnValue() + ReachModule.entityBonus());
    }
}
