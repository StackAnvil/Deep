package net.stackanvil.deep.mixin;

import net.stackanvil.deep.module.impl.SilentDisconnectModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerMixin {

    @Inject(method = "onDisconnect", at = @At("HEAD"), cancellable = true)
    private void deep$silentDisconnect(final DisconnectionDetails details, final CallbackInfo info) {
        Minecraft client = Minecraft.getInstance();
        if (!SilentDisconnectModule.shouldSuppress(client)) {
            return;
        }
        client.player.sendSystemMessage(Component.literal("Disconnected: ").append(details.reason()));
        info.cancel();
    }
}
