package net.stackanvil.deep.mixin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.stackanvil.deep.Deep;
import net.stackanvil.deep.event.PacketEventSilencer;
import net.stackanvil.deep.event.PacketReceiveEvent;
import net.stackanvil.deep.event.PacketSendEvent;
import net.stackanvil.deep.event.PacketSentEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void deep$receivePacket(final ChannelHandlerContext context, final Packet<?> packet,
                                             final CallbackInfo info) {
        PacketReceiveEvent event = Deep.MODULES.call(new PacketReceiveEvent(
                Minecraft.getInstance(),
                (Connection) (Object) this,
                packet
        ));
        if (event.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void deep$sendPacket(final Packet<?> packet, final ChannelFutureListener listener,
                                          final boolean flush, final CallbackInfo info) {
        if (PacketEventSilencer.silent()) {
            return;
        }
        PacketSendEvent event = Deep.MODULES.call(new PacketSendEvent(
                Minecraft.getInstance(),
                (Connection) (Object) this,
                packet
        ));
        if (event.isCancelled()) {
            info.cancel();
            return;
        }
        if (event.packet() != packet) {
            info.cancel();
            PacketEventSilencer.runSilently(() -> ((Connection) (Object) this).send(event.packet(), listener, flush));
        }
    }

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("RETURN")
    )
    private void deep$sentPacket(final Packet<?> packet, final ChannelFutureListener listener,
                                          final boolean flush, final CallbackInfo info) {
        if (!PacketEventSilencer.silent()) {
            Deep.MODULES.call(new PacketSentEvent(Minecraft.getInstance(), (Connection) (Object) this, packet));
        }
    }
}
