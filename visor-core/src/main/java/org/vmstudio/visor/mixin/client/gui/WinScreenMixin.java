package org.vmstudio.visor.mixin.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.WinScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.vmstudio.visor.core.client.VisorState;

@Mixin(WinScreen.class)
public class WinScreenMixin {
    @Redirect(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFunc(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V"), method = "render")
    private void visor$dontDestroyAlpha(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor) {
        // vanilla path keeps vanilla blending untouched;
        // the alpha-channel preservation is only needed for VR eye targets
        if (VisorState.get().isNotActive()) {
            RenderSystem.blendFunc(sourceFactor, destFactor);
            return;
        }
        RenderSystem.blendFuncSeparate(sourceFactor, destFactor, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }
}
