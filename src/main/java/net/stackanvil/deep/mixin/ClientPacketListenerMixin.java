package net.stackanvil.deep.mixin;

import net.stackanvil.deep.Deep;
import net.stackanvil.deep.event.GameJoinedEvent;
import net.stackanvil.deep.event.GameLeftEvent;
import net.stackanvil.deep.event.SendChatEvent;
import net.stackanvil.deep.event.SoundPacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void deep$handleSoundEvent(final ClientboundSoundPacket packet, final CallbackInfo info) {
        Deep.MODULES.call(new SoundPacketEvent(Minecraft.getInstance(), packet));
    }

    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void deep$handleLogin(final ClientboundLoginPacket packet, final CallbackInfo info) {
        Deep.MODULES.call(new GameJoinedEvent(Minecraft.getInstance(), (ClientPacketListener) (Object) this));
    }

    @Inject(method = "clearLevel", at = @At("HEAD"))
    private void deep$clearLevel(final CallbackInfo info) {
        Deep.MODULES.call(new GameLeftEvent(Minecraft.getInstance(), (ClientPacketListener) (Object) this));
    }

    @ModifyVariable(method = "sendChat", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String deep$sendChat(final String message) {
        return Deep.MODULES.call(new SendChatEvent(Minecraft.getInstance(), message, false)).message();
    }

    @ModifyVariable(method = "sendCommand", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String deep$sendCommand(final String command) {
        return Deep.MODULES.call(new SendChatEvent(Minecraft.getInstance(), command, true)).message();
    }
}
