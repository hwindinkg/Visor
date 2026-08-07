package org.vmstudio.visor.core.client.render;


import lombok.Getter;
import org.vmstudio.visor.core.client.render.helpers.RenderHelper;
import org.vmstudio.visor.core.client.render.shaders.*;


public class VRShaders {

    @Getter
    private static VRShaderPostProcessEye postProcess;

    @Getter
    private static VRShaderMixedReality mixedReality;

    @Getter
    private static VRShaderTeleportPoint teleportPoint;

    @Getter
    private static VRShaderEndPortal endPortal;

    @Getter
    private static VRShaderInBlockVignette inBlockVignette;


    private VRShaders() {

    }

    public static void setup() throws Exception {
        postProcess = new VRShaderPostProcessEye();
        postProcess.init();
        RenderHelper.logGLErrorSoft("init PostProcess shader");

        mixedReality = new VRShaderMixedReality();
        mixedReality.init();
        RenderHelper.logGLErrorSoft("init MixedReality shader");

        teleportPoint = new VRShaderTeleportPoint();
        teleportPoint.init();
        RenderHelper.logGLErrorSoft("init TeleportPoint shader");

        endPortal = new VRShaderEndPortal();
        endPortal.init();
        RenderHelper.logGLErrorSoft("init EndPortal shader");

        inBlockVignette = new VRShaderInBlockVignette();
        inBlockVignette.init();
        RenderHelper.logGLErrorSoft("init InBlockVignette shader");
    }


}
