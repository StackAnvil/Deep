package net.stackanvil.deep.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

public final class ItemUseEvent extends CancellableDeepEvent {

    private final Minecraft client;
    private final InteractionHand hand;

    public ItemUseEvent(final Minecraft client, final InteractionHand hand) {
        this.client = client;
        this.hand = hand;
    }

    public Minecraft client() {
        return this.client;
    }

    public InteractionHand hand() {
        return this.hand;
    }
}
