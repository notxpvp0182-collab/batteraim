package com.betteraim.mixin;

import com.betteraim.config.BetterAimConfig;
import com.betteraim.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels vanilla crosshair rendering when Better AIM's crosshair system is enabled.
 *
 * Method target: {@code InGameHud#renderCrosshair(DrawContext, RenderTickCounter)}
 * (Yarn mapping for MC 1.21.x)
 */
@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderCrosshair",
            at = @At("HEAD"),
            cancellable = true)
    private void betterAim$cancelVanillaCrosshair(DrawContext context,
                                                  RenderTickCounter tickCounter,
                                                  CallbackInfo ci) {
        BetterAimConfig cfg = ConfigManager.getConfig();
        if (!cfg.enabled) return;

        // Cancel vanilla crosshair when either Better AIM crosshair is enabled.
        // Our own crosshair is drawn via HudRenderCallback in BetterAimClient.
        if (cfg.normalCrosshair.enabled || cfg.hitableCrosshair.enabled) {
            ci.cancel();
        }
    }
}
