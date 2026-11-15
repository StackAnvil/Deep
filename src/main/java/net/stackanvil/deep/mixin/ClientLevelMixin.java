package net.stackanvil.deep.mixin;

import net.stackanvil.deep.Deep;
import net.stackanvil.deep.event.BlockBreakingProgressEvent;
import net.stackanvil.deep.event.EntityAddedEvent;
import net.stackanvil.deep.event.EntityRemovedEvent;
import net.stackanvil.deep.event.ParticleEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Shadow
    protected abstract LevelEntityGetter<Entity> getEntities();

    @Inject(method = "addEntity", at = @At("RETURN"))
    private void deep$entityAdded(final Entity entity, final CallbackInfo info) {
        Deep.MODULES.call(new EntityAddedEvent(Minecraft.getInstance(), entity));
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void deep$entityRemoved(final int id, final RemovalReason reason, final CallbackInfo info) {
        Entity entity = this.getEntities().get(id);
        if (entity != null) {
            Deep.MODULES.call(new EntityRemovedEvent(Minecraft.getInstance(), entity, reason));
        }
    }

    @Inject(method = "destroyBlockProgress", at = @At("HEAD"))
    private void deep$blockBreakingProgress(final int id, final BlockPos pos, final int progress,
                                                     final CallbackInfo info) {
        Deep.MODULES.call(new BlockBreakingProgressEvent(Minecraft.getInstance(), id, pos, progress));
    }

    @Inject(method = "doAddParticle", at = @At("HEAD"), cancellable = true)
    private void deep$particle(final ParticleOptions particle, final boolean overrideLimiter,
                                        final boolean alwaysShowParticles, final double x, final double y,
                                        final double z, final double xd, final double yd, final double zd,
                                        final CallbackInfo info) {
        ParticleEvent event = Deep.MODULES.call(new ParticleEvent(Minecraft.getInstance(), particle, alwaysShowParticles));
        if (event.isCancelled()) {
            info.cancel();
        }
    }
}
