package org.vmstudio.visor.loader.neoforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vmstudio.visor.api.client.ClientFeature;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.VisorState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;

/**
 * Boss bar equivalent of the old {@code VanillaGuiOverlay.BOSS_EVENT_PROGRESS}
 * cancellation from the ForgeGui.pre() mixin: hidden while Visor is active and
 * a screen is open or GUI_DISABLE_HUD is enabled. In 1.21 the boss overlay is
 * rendered from a {@code Gui} layer lambda, so it is targeted directly.
 */
@Mixin(BossHealthOverlay.class)
public abstract class NeoForgeBossOverlayVRMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void noBossBar(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (VisorState.get().isNotActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null
                || ClientContext.visor.isFeatureEnabled(ClientFeature.GUI_DISABLE_HUD)) {
            ci.cancel();
        }
    }

}
