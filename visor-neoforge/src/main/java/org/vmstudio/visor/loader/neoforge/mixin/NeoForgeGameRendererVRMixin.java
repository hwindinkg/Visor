package org.vmstudio.visor.loader.neoforge.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.vmstudio.visor.core.client.render.VRRenderState;

/**
 * In 1.20.1 (Forge) the vanilla yaw/pitch rotation of the level camera was
 * re-applied inside {@code GameRenderer.renderLevel} via
 * {@code Camera.setAnglesInternal} and additional {@code PoseStack.mulPose}
 * calls; this mixin skipped them for VR eye passes.
 * <p>
 * In 1.21.1 that pipeline was rebuilt: {@code GameRenderer.renderLevel} no
 * longer rotates the camera (no {@code setAnglesInternal}, no {@code mulPose}),
 * the rotation now happens once in {@link Camera#setup} via
 * {@code setRotation(yaw, pitch)} (protected, opened via visor.accesstransformer).
 * The mixin moves the same cancellation to that single rotation point; the
 * old mulPose X/Y/Z hooks have no equivalent target in 1.21.1 and were removed.
 */
@Mixin(Camera.class)
public class NeoForgeGameRendererVRMixin {

    @Shadow
    private void setRotation(float yaw, float pitch) { throw new AssertionError(); }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0), method = "setup")
    public void removeVanillaCameraRotation(Camera camera, float yaw, float pitch) {
        if (VRRenderState.getPhase().isVanilla()
                || !VRRenderState.getRenderPass().isEye()) {
            this.setRotation(yaw, pitch);
        }
    }

}
