package net.stackanvil.deep.mixin;

import net.stackanvil.deep.Deep;
import net.stackanvil.deep.module.impl.BetterTabModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void deep$tabName(final PlayerInfo info, final CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(Deep.MODULES.tabPlayerName(Minecraft.getInstance(), info, cir.getReturnValue()));
    }

    @Inject(method = "getPlayerInfos", at = @At("RETURN"), cancellable = true)
    private void deep$rewritePlayerInfos(final CallbackInfoReturnable<List<PlayerInfo>> cir) {
        cir.setReturnValue(BetterTabModule.rewritePlayerInfos(cir.getReturnValue()));
    }
}
