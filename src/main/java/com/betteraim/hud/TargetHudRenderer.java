package com.betteraim.hud;

import com.betteraim.config.BetterAimConfig;
import com.betteraim.config.ConfigManager;
import com.betteraim.feedback.HitFeedbackManager;
import com.betteraim.target.TargetDetector;
import com.betteraim.util.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;

/**
 * Renders the Target HUD overlay – entity name, optional 3D preview.
 * All colours are independent from the crosshair and highlight systems.
 */
@Environment(EnvType.CLIENT)
public final class TargetHudRenderer {

    // Default position: top-right corner
    private static final int DEFAULT_MARGIN = 8;
    private static final int HUD_W = 120;

    private TargetHudRenderer() {}

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        if (client.currentScreen != null) return;

        BetterAimConfig cfg = ConfigManager.getConfig();
        if (!cfg.enabled || !cfg.targetHud.enabled) return;

        LivingEntity target = TargetDetector.getCurrentTarget();
        if (target == null || !target.isAlive()) return;

        BetterAimConfig.TargetHudSettings hud = cfg.targetHud;

        // Resolve colours – hit feedback may override text colour
        int bgArgb     = hud.bgArgb();
        int borderArgb = hud.borderArgb();
        int textArgb   = hud.textArgb();

        if (HitFeedbackManager.isHitTarget(target) && cfg.hitFeedback.hudFeedback) {
            textArgb = cfg.hitFeedback.argb();
        }

        // ── Layout ────────────────────────────────────────────────────────
        float scale   = hud.scale;
        int sw        = context.getScaledWindowWidth();
        int sh        = context.getScaledWindowHeight();

        boolean show3D   = hud.showPreview && (target instanceof net.minecraft.entity.player.PlayerEntity);
        int previewH     = show3D ? 64 : 0;
        int textLineH    = 12;
        int paddingV     = 6;
        int paddingH     = 8;

        int hudW = (int)(HUD_W * scale);
        int lineH = (int)(textLineH * scale);
        int padH  = (int)(paddingH  * scale);
        int padV  = (int)(paddingV  * scale);
        int prevH = (int)(previewH  * scale);

        int hudH = padV + (hud.showLabel ? lineH + 2 : 0)
                       + (hud.showName  ? lineH + 2 : 0)
                       + prevH + padV;

        // Position
        int hudX = (hud.posX >= 0) ? hud.posX : (sw - hudW - DEFAULT_MARGIN);
        int hudY = (hud.posY >= 0) ? hud.posY : DEFAULT_MARGIN;

        // Clamp to screen
        hudX = Math.max(0, Math.min(sw - hudW, hudX));
        hudY = Math.max(0, Math.min(sh - hudH, hudY));

        // ── Background ────────────────────────────────────────────────────
        if (hud.background) {
            context.fill(hudX, hudY, hudX + hudW, hudY + hudH, bgArgb);
        }

        // ── Border ────────────────────────────────────────────────────────
        if (hud.border) {
            int bw = hud.borderWidth;
            context.fill(hudX,            hudY,            hudX + hudW, hudY + bw,         borderArgb); // top
            context.fill(hudX,            hudY + hudH - bw, hudX + hudW, hudY + hudH,      borderArgb); // bottom
            context.fill(hudX,            hudY + bw,       hudX + bw,   hudY + hudH - bw,  borderArgb); // left
            context.fill(hudX + hudW - bw, hudY + bw,      hudX + hudW, hudY + hudH - bw, borderArgb); // right
        }

        // ── Text content ──────────────────────────────────────────────────
        int textX = hudX + padH;
        int textY = hudY + padV;

        if (hud.showLabel) {
            context.drawTextWithShadow(client.textRenderer,
                    net.minecraft.text.Text.literal("TARGET"),
                    textX, textY, textArgb);
            textY += lineH + 2;
        }

        if (hud.showName) {
            String name = target.getName().getString();
            // Truncate if name is too long
            if (client.textRenderer.getWidth(name) > hudW - padH * 2) {
                while (name.length() > 1
                        && client.textRenderer.getWidth(name + "…") > hudW - padH * 2) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + "…";
            }
            context.drawTextWithShadow(client.textRenderer,
                    net.minecraft.text.Text.literal(name),
                    textX, textY, textArgb);
            textY += lineH + 2;
        }

        // ── 3D Entity Preview ─────────────────────────────────────────────
        if (show3D && prevH > 0) {
            int centerX = hudX + hudW / 2;
            int previewBottom = hudY + hudH - padV;
            int previewTop    = previewBottom - prevH;
            int entitySize    = prevH / 2;

            try {
                // InventoryScreen.drawEntity signature for 1.21.x:
                // (DrawContext, int x1, int y1, int x2, int y2, int size,
                //  float delta, float mouseX, float mouseY, LivingEntity)
                InventoryScreen.drawEntity(context,
                        centerX - entitySize, previewTop,
                        centerX + entitySize, previewBottom,
                        entitySize,
                        0.0625f,
                        centerX, previewTop,
                        target);
            } catch (Exception e) {
                // If the API is slightly different in 1.21.11, fall back to name
                context.drawCenteredTextWithShadow(client.textRenderer,
                        net.minecraft.text.Text.literal("⬛"),
                        centerX, previewTop + prevH / 2, 0x888888);
            }
        }
    }
}
