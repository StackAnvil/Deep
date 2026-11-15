package net.stackanvil.deep.mixin;

import net.stackanvil.deep.Deep;
import net.stackanvil.deep.event.ClientInputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void deep$updateInput(final CallbackInfo info) {
        Deep.MODULES.call(new ClientInputEvent(Minecraft.getInstance(), this));
    }
}
