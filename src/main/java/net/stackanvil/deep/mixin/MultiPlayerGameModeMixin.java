package net.stackanvil.deep.mixin;

import net.stackanvil.deep.Deep;
import net.stackanvil.deep.event.AttackEntityEvent;
import net.stackanvil.deep.event.BlockAttackEvent;
import net.stackanvil.deep.event.BlockInteractEvent;
import net.stackanvil.deep.event.EntityInteractEvent;
import net.stackanvil.deep.event.ItemUseEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void deep$attackEntity(final Player player, final Entity target, final CallbackInfo info) {
        AttackEntityEvent event = Deep.MODULES.call(new AttackEntityEvent(Minecraft.getInstance(), player, target));
        if (event.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void deep$attackBlock(final BlockPos pos, final Direction direction,
                                           final CallbackInfoReturnable<Boolean> info) {
        BlockAttackEvent event = Deep.MODULES.call(new BlockAttackEvent(Minecraft.getInstance(), pos, direction));
        if (event.isCancelled()) {
            info.setReturnValue(false);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void deep$useItemOn(final LocalPlayer player, final InteractionHand hand,
                                         final BlockHitResult blockHit,
                                         final CallbackInfoReturnable<InteractionResult> info) {
        BlockInteractEvent event = Deep.MODULES.call(new BlockInteractEvent(Minecraft.getInstance(), hand, blockHit));
        if (event.isCancelled()) {
            info.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void deep$entityInteract(final Player player, final Entity entity,
                                              final EntityHitResult hitResult, final InteractionHand hand,
                                              final CallbackInfoReturnable<InteractionResult> info) {
        EntityInteractEvent event = Deep.MODULES.call(
                new EntityInteractEvent(Minecraft.getInstance(), player, entity, hitResult, hand)
        );
        if (event.isCancelled()) {
            info.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void deep$useItem(final Player player, final InteractionHand hand,
                                       final CallbackInfoReturnable<InteractionResult> info) {
        ItemUseEvent event = Deep.MODULES.call(new ItemUseEvent(Minecraft.getInstance(), hand));
        if (event.isCancelled()) {
            info.setReturnValue(InteractionResult.FAIL);
        }
    }
}
