package net.stackanvil.deep.mixin;

import net.stackanvil.deep.Deep;
import net.stackanvil.deep.event.MouseClickEvent;
import net.stackanvil.deep.event.MouseScrollInputEvent;
import net.stackanvil.deep.module.impl.FreeLookModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void deep$mouseClick(final long window, final MouseButtonInfo buttonInfo, final int action,
                                          final CallbackInfo info) {
        MouseClickEvent event = Deep.MODULES.call(new MouseClickEvent(Minecraft.getInstance(), buttonInfo, action));
        if (event.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void deep$mouseScroll(final long window, final double xOffset, final double yOffset,
                                           final CallbackInfo info) {
        MouseScrollInputEvent event = Deep.MODULES.call(new MouseScrollInputEvent(Minecraft.getInstance(), xOffset, yOffset));
        if (event.isCancelled()) {
            info.cancel();
        }
    }

    @Redirect(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void deep$freeLookTurn(final LocalPlayer player, final double xOffset, final double yOffset) {
        if (!FreeLookModule.handleTurn(Minecraft.getInstance(), player, xOffset, yOffset)) {
            player.turn(xOffset, yOffset);
        }
    }
}
