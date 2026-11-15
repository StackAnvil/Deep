package net.stackanvil.deep.mixin;

import net.stackanvil.deep.module.impl.MultiActionsModule;
import net.stackanvil.deep.module.impl.NoMiningTraceModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftActionsMixin {

    @Shadow
    public HitResult hitResult;

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void deep$replaceTraceForMining(final CallbackInfoReturnable<Boolean> info) {
        this.hitResult = NoMiningTraceModule.replacementHitResult((Minecraft) (Object) this, this.hitResult);
    }

    @Redirect(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z")
    )
    private boolean deep$allowStartAttackWhileUsing(final LocalPlayer player) {
        return !MultiActionsModule.attackWhileUsing() && player.isHandsBusy();
    }

    @Redirect(
            method = "continueAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z")
    )
    private boolean deep$allowBreakWhileUsing(final LocalPlayer player) {
        return !MultiActionsModule.breakWhileUsing() && player.isUsingItem();
    }

    @Redirect(
            method = "startUseItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z")
    )
    private boolean deep$allowPlaceWhileBreaking(final MultiPlayerGameMode gameMode) {
        return !MultiActionsModule.placeWhileBreaking() && gameMode.isDestroying();
    }

    @Redirect(
            method = "startUseItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z")
    )
    private boolean deep$allowUseWhileBusy(final LocalPlayer player) {
        return !MultiActionsModule.useWhileBusy() && player.isHandsBusy();
    }
}
