package org.vmstudio.visor.loader.neoforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vmstudio.visor.api.client.ClientFeature;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.VisorState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * NeoForge 21.1 removed ForgeGui / NamedGuiOverlay / VanillaGuiOverlay entirely
 * (option (a) of the migration plan is not available). The vanilla {@link Gui}
 * now renders its overlays through private per-element methods; this mixin
 * cancels them with the same semantics as the old ForgeGui.pre() mixin:
 * <ul>
 *   <li>SLEEP_FADE - always hidden while Visor is active</li>
 *   <li>CHAT_PANEL - hidden while Visor is active and no ChatScreen is open</li>
 *   <li>PLAYER_HEALTH / ARMOR / FOOD / AIR / MOUNT_HEALTH / JUMP_BAR /
 *       EXPERIENCE_BAR - hidden while a screen is open or GUI_DISABLE_HUD
 *       is enabled (boss bar is handled by NeoForgeBossOverlayVRMixin)</li>
 * </ul>
 */
@Mixin(Gui.class)
public abstract class NeoForgeIngameGuiVRMixin {

    @Inject(method = "renderSleepOverlay", at = @At("HEAD"), cancellable = true)
    private void noSleepFade(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (VisorState.get().isNotActive()) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "renderChat", at = @At("HEAD"), cancellable = true)
    private void noChatPanel(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (VisorState.get().isNotActive()) {
            return;
        }
        if (!(Minecraft.getInstance().screen instanceof ChatScreen)) {
            ci.cancel();
        }
    }

    @Inject(method = "maybeRenderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void noPlayerHealth(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (visor$shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "maybeRenderVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void noVehicleHealth(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (visor$shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "maybeRenderJumpMeter", at = @At("HEAD"), cancellable = true)
    private void noJumpBar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (visor$shouldHideHud()) {
            ci.cancel();
        }
    }

    @Inject(method = "maybeRenderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void noExperienceBar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (visor$shouldHideHud()) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean visor$shouldHideHud() {
        if (VisorState.get().isNotActive()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.screen != null
                || ClientContext.visor.isFeatureEnabled(ClientFeature.GUI_DISABLE_HUD);
    }

}
