package net.stackanvil.deep.mixin;

import net.stackanvil.deep.Deep;
import net.stackanvil.deep.event.CameraTransformEvent;
import net.stackanvil.deep.render.RenderSuppression;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract float yRot();

    @Shadow
    public abstract float xRot();

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void deep$modifyFov(final float partialTicks, final CallbackInfoReturnable<Float> info) {
        if (RenderSuppression.suppresses(RenderSuppression.Kind.DYNAMIC_FOV)) {
            return;
        }
        info.setReturnValue(Deep.MODULES.fov(Minecraft.getInstance(), info.getReturnValue()));
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void deep$suppressBlockingEffects(final CameraRenderState cameraState,
                                                       final DeltaTracker deltaTracker,
                                                       final CallbackInfo info) {
        if (RenderSuppression.suppresses(RenderSuppression.Kind.BLINDNESS)
                || RenderSuppression.suppresses(RenderSuppression.Kind.DARKNESS)) {
            cameraState.entityRenderState.doesMobEffectBlockSky = false;
        }
    }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void deep$disableCameraClip(final float cameraDist, final CallbackInfoReturnable<Float> info) {
        if (RenderSuppression.suppresses(RenderSuppression.Kind.CAMERA_CLIP)) {
            info.setReturnValue(cameraDist);
        }
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;calculateFov(F)F"))
    private void deep$modifyCamera(final DeltaTracker deltaTracker, final CallbackInfo info) {
        CameraTransformEvent event = Deep.MODULES.cameraTransform(
                Minecraft.getInstance(),
                this.position(),
                this.yRot(),
                this.xRot()
        );
        this.setPosition(event.position());
        this.setRotation(event.yaw(), event.pitch());
    }
}
