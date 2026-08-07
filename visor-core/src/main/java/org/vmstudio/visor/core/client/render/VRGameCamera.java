package org.vmstudio.visor.core.client.render;

import org.vmstudio.visor.api.common.player.VRPose;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.client.render.VRRenderPass;
import org.vmstudio.visor.api.common.utils.VRMathUtils;
import org.vmstudio.visor.core.client.player.VRClientPlayers;
import org.vmstudio.visor.core.client.render.helpers.RenderPoseHelper;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import org.vmstudio.visor.core.client.ClientContext;


public class VRGameCamera extends Camera {

    @Override
    public void setup(@NotNull BlockGetter level,
                      @NotNull Entity entity,
                      boolean thirdPerson,
                      boolean thirdPersonReverse,
                      float partialTicks) {
        if (VRRenderState.getPhase().isVanilla()) {
            super.setup(level, entity, thirdPerson, thirdPersonReverse, partialTicks);
            if (VRRenderState.isSpectatedVRView(entity)) {
                setupSpectatedVR(entity);
            }
        } else {
            setupVR(level, entity);
        }
    }


    @Override
    public void tick() {
        if (VRRenderState.getPhase().isVanilla()) {
            super.tick();
        }
    }


    @Override
    public boolean isDetached() {
        if (VRRenderState.getPhase().isVanilla()) {
            return super.isDetached();
        }
        return VRRenderState.isSelfModelRenderCamera();
    }



    private void setupVR(BlockGetter level, Entity entity) {
        this.initialized = true;
        this.level = level;
        this.entity = entity;

        VRRenderPass renderPass = VRRenderState.getRenderPass();
        VRPose cameraElement = ClientContext.localPlayer
                .getPoseData(PlayerPoseType.RENDER)
                .getCameraPose(renderPass);

        // Position
        this.setPosition(new Vec3(
                (Vector3f) RenderPoseHelper.getCameraPosition(
                        renderPass,
                        ClientContext.localPlayer.getPoseData(PlayerPoseType.RENDER)
                )
        ));

        // Orientation
        this.xRot = -cameraElement.getPitchDegrees();
        this.yRot =  cameraElement.getYawDegrees();

        // Look, Up, Left vectors
        var dir = cameraElement.getDirection();
        var upVec = cameraElement.getCustomVector(VRMathUtils.UP_VECTOR);
        var leftVec = cameraElement.getCustomVector(VRMathUtils.LEFT_VECTOR);

        this.getLookVector().set(dir.x(), dir.y(), dir.z());
        this.getUpVector().set(upVec.x, upVec.y, upVec.z);
        this.getLeftVector().set(leftVec.x, leftVec.y, leftVec.z);

        // Full camera orientation from the OpenXR pose.
        // GameRenderer.renderLevel consumes rotation().conjugated() as the view
        // matrix (view = R^-1) — the same value 1.20.1 applied to the PoseStack
        // via RenderPoseHelper.applyCameraOrientation (rotation.transpose()).
        // Reconstructing the rotation from yaw/pitch angles here (Y(-yRot)*X(xRot))
        // mirrored the view: vanilla builds rotationYXZ(PI-yaw, -pitch, 0), so the
        // camera ended up yawed 180° with inverted pitch (all axes flipped).
        this.rotation().setFromNormalized(cameraElement.getRotation());
    }

    private void setupSpectatedVR(Entity entity) {
        var vrPlayer = VRClientPlayers.getPlayer(entity.getUUID());
        if (vrPlayer == null) {
            return;
        }
        VRPose hmd = vrPlayer.getPoseData(PlayerPoseType.RENDER).getHmd();

        this.setPosition(new Vec3((Vector3f) hmd.getPosition()));

        // Orientation
        this.xRot = -hmd.getPitchDegrees();
        this.yRot =  hmd.getYawDegrees();

        var dir = hmd.getDirection();
        var upVec = hmd.getCustomVector(VRMathUtils.UP_VECTOR);
        var leftVec = hmd.getCustomVector(VRMathUtils.LEFT_VECTOR);

        this.getLookVector().set(dir.x(), dir.y(), dir.z());
        this.getUpVector().set(upVec.x, upVec.y, upVec.z);
        this.getLeftVector().set(leftVec.x, leftVec.y, leftVec.z);

        // Same as setupVR: use the full pose rotation (view = rotation().conjugated()).
        this.rotation().setFromNormalized(hmd.getRotation());
    }

}
