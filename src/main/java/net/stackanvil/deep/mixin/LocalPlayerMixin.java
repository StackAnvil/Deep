package net.stackanvil.deep.mixin;

import net.stackanvil.deep.Deep;
import net.stackanvil.deep.event.ItemStopUseEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void deep$stopUsingItem(final CallbackInfo info) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        Deep.MODULES.call(new ItemStopUseEvent(
                Minecraft.getInstance(),
                player.getUsedItemHand(),
                player.getUseItem().copy(),
                player.getUseItemRemainingTicks()
        ));
    }
}
