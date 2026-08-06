package org.vmstudio.visor.mixin.client.gui;

import org.vmstudio.visor.api.client.ClientFeature;
import org.vmstudio.visor.core.client.ClientContext;
import org.vmstudio.visor.core.client.VisorState;
import org.vmstudio.visor.extensions.client.GuiExtension;
import org.vmstudio.visor.api.client.settings.VRClientSettings;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Gui.class)
public abstract class GuiMixin implements GuiExtension {

    @Final
    @Shadow
    private Minecraft minecraft;

    /* ********************************** *\
  //--------DISABLE VANILLA OVERLAYS--------\\
    \* ********************************** */
    @Inject(at = @At("HEAD"), method = "renderHotbar", cancellable = true)
    public void visor$noVanillaHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if(VisorState.get().isNotActive()
                || (minecraft.screen == null
                && !VRClientSettings.isHudDisableHotBar()
                && ClientContext.visor.isFeatureDisabled(ClientFeature.GUI_DISABLE_HUD))) return;
        ci.cancel();
    }
    @Inject(at = @At("HEAD"), method = "renderPlayerHealth", cancellable = true)
    public void visor$noVanillaPlayerHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if(VisorState.get().isNotActive() || (minecraft.screen == null
                && ClientContext.visor.isFeatureDisabled(ClientFeature.GUI_DISABLE_HUD))) return;
        ci.cancel();
    }
    @Inject(at = @At("HEAD"), method = "renderVehicleHealth", cancellable = true)
    public void visor$noVanillaVehicleHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if(VisorState.get().isNotActive() || (minecraft.screen == null
                && ClientContext.visor.isFeatureDisabled(ClientFeature.GUI_DISABLE_HUD))) return;
        ci.cancel();
    }
    @Inject(at = @At("HEAD"), method = "renderJumpMeter", cancellable = true)
    public void visor$noVanillaJumpMeter(PlayerRideableJumping playerRideableJumping, GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if(VisorState.get().isNotActive() || (minecraft.screen == null
                && ClientContext.visor.isFeatureDisabled(ClientFeature.GUI_DISABLE_HUD))) return;
        ci.cancel();
    }
    @Inject(at = @At("HEAD"), method = "renderExperienceBar", cancellable = true)
    public void visor$noVanillaExperienceBar(GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if(VisorState.get().isNotActive() || (minecraft.screen == null
                && ClientContext.visor.isFeatureDisabled(ClientFeature.GUI_DISABLE_HUD))) return;
        ci.cancel();
    }
    @Redirect(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;)V"),
            method = "render")
    public void visor$noVanillaGuiBossHealth(BossHealthOverlay instance,
                                             GuiGraphics guiGraphics) {
        if(VisorState.get().isNotActive() || (minecraft.screen == null
                && ClientContext.visor.isFeatureDisabled(ClientFeature.GUI_DISABLE_HUD))) {
            instance.render(guiGraphics);
        }
    }
    @Redirect(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V"),
            method = "render")
    public void visor$noVanillaGuiChat(ChatComponent instance,
                                       GuiGraphics guiGraphics,
                                       int i, int j, int k, boolean bl) {
        if(VisorState.get().isNotActive()) {
            instance.render(guiGraphics,i,j,k,bl);
            return;
        }
        if(minecraft.screen instanceof ChatScreen) {
            instance.render(guiGraphics, i, j, k, bl);
        }
    }


    @Inject(at = @At("HEAD"), method = "renderVignette", cancellable = true)
    public void visor$noVanillaVignette(GuiGraphics guiGraphics, Entity entity, CallbackInfo ci) {
        if(VisorState.get().isNotActive()) return;
        ci.cancel();
    }
    @Inject(at = @At("HEAD"), method = "renderSpyglassOverlay", cancellable = true)
    public void visor$noVanillaSpyglassOverlay(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
        if(VisorState.get().isNotActive()) return;
        ci.cancel();
    }
    @Inject(at = @At("HEAD"), method = "renderEffects", cancellable = true)
    public void visor$noVanillaEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if(VisorState.get().isNotActive()) return;
        ci.cancel();
    }
    @Inject(at = @At("HEAD"), method = "renderSelectedItemName", cancellable = true)
    public void visor$noVanillaSelectedItemName(GuiGraphics guiGraphics, CallbackInfo ci) {
        if(VisorState.get().isNotActive()) return;
        ci.cancel();
    }
    @Inject(at = @At("HEAD"), method = "renderSavingIndicator", cancellable = true)
    public void visor$noAutoSaveText(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if(VisorState.get().isNotActive()) return;
        ci.cancel();
    }

    @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
    public void visor$noTextureOverlay(GuiGraphics guiGraphics, ResourceLocation resourceLocation, float f, CallbackInfo ci) {
        if(VisorState.get().isNotActive()) return;
        ci.cancel();
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    public void visor$noPortalOverlay(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
        if(VisorState.get().isNotActive()) return;
        ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "renderCrosshair", cancellable = true)
    public void visor$noCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if(VisorState.get().isNotActive()) return;
        ci.cancel();
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getSleepTimer()I"), method = "render")
    public int visor$noSleepOverlay(Player instance) {
        return VisorState.get().isActive()
                ? 0
                : instance.getSleepTimer();
    }

}
