package net.stackanvil.deep.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.stackanvil.deep.render.RenderSuppression;
import net.stackanvil.deep.rivet.GlassBackdrop;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void deep$noHurtCamera(final CameraRenderState cameraState, final PoseStack poseStack,
                                            final CallbackInfo info) {
        if (RenderSuppression.suppresses(RenderSuppression.Kind.HURT_CAMERA)) {
            info.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void deep$noViewBob(final CameraRenderState cameraState, final PoseStack poseStack,
                                         final CallbackInfo info) {
        if (RenderSuppression.suppresses(RenderSuppression.Kind.VIEW_BOB)) {
            info.cancel();
        }
    }

    @Inject(method = "processBlurEffect", at = @At("HEAD"))
    private void deep$glassCaptureSharp(final CallbackInfo info) {
        GlassBackdrop.onBlurStart(((GameRenderer) (Object) this).mainRenderTarget());
    }

    @Inject(method = "processBlurEffect", at = @At("RETURN"))
    private void deep$glassCaptureBlurred(final CallbackInfo info) {
        GlassBackdrop.onBlurEnd(((GameRenderer) (Object) this).mainRenderTarget());
    }
}
