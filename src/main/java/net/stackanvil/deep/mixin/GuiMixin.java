package net.stackanvil.deep.mixin;

import net.stackanvil.deep.Deep;
import net.stackanvil.deep.event.OpenScreenEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void deep$setScreen(final @Nullable Screen screen, final CallbackInfo info) {
        OpenScreenEvent event = Deep.MODULES.call(new OpenScreenEvent(Minecraft.getInstance(), screen));
        if (event.isCancelled()) {
            info.cancel();
        }
    }
}
