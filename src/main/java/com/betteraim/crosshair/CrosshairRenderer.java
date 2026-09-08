package com.betteraim.crosshair;

import com.betteraim.config.BetterAimConfig;
import com.betteraim.config.ConfigManager;
import com.betteraim.feedback.HitFeedbackManager;
import com.betteraim.target.TargetDetector;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Renders the Better AIM custom crosshair on the in-game HUD.
 * Registered on HudRenderCallback.EVENT from BetterAimClient.
 *
 * All drawing uses DrawContext.fill() – no custom shaders.
 */
@Environment(EnvType.CLIENT)
public final class CrosshairRenderer {

    private CrosshairRenderer() {}

    public static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) return;
        if (client.currentScreen != null) return;

        BetterAimConfig cfg = ConfigManager.getConfig();
        if (!cfg.enabled) return;

        // ── Perspective check ─────────────────────────────────────────────
        Perspective persp = client.options.getPerspective();
        // Enum comparison – avoids relying on isFirstPerson() availability.
        boolean isFirst     = (persp == Perspective.FIRST_PERSON);
        boolean isThirdBack = (persp == Perspective.THIRD_PERSON_BACK);
        boolean isOther     = !isFirst && !isThirdBack;   // THIRD_PERSON_FRONT etc.

        if (isFirst    && !cfg.crosshairFirstPerson) return;
        if (isThirdBack && !cfg.crosshairThirdPerson) return;
        if (isOther     && !cfg.crosshairOtherCamera) return;

        // ── Choose settings ───────────────────────────────────────────────
        boolean hitActive = TargetDetector.isTargetHitable() && cfg.hitableCrosshair.enabled;
        BetterAimConfig.CrosshairSettings settings;

        if (hitActive) {
            settings = cfg.hitableCrosshair;
        } else if (cfg.normalCrosshair.enabled) {
            settings = cfg.normalCrosshair;
        } else {
            return;
        }

        // ── Resolve colour (hit feedback may override) ────────────────────
        int argb = settings.argb();
        if (HitFeedbackManager.isActive() && cfg.hitFeedback.crosshairFeedback) {
            argb = cfg.hitFeedback.argb();
        }

        int cx = context.getScaledWindowWidth()  / 2;
        int cy = context.getScaledWindowHeight() / 2;

        drawStyle(context, settings.getStyle(),
                cx, cy, settings.size, settings.thickness, settings.gap, argb);
    }

    // ─── Style dispatch ────────────────────────────────────────────────────

    private static void drawStyle(DrawContext ctx, CrosshairStyle style,
                                  int cx, int cy, int size, int thick, int gap, int argb) {
        switch (style) {
            case CROSS         -> drawCross(ctx, cx, cy, size, thick, gap, argb);
            case DOT           -> drawDot(ctx, cx, cy, size, argb);
            case CIRCLE        -> drawFilledCircle(ctx, cx, cy, size, argb);
            case HOLLOW_CIRCLE -> drawHollowCircle(ctx, cx, cy, size, thick, argb);
            case SMALL_BOX     -> drawFilledBox(ctx, cx, cy, Math.max(1, size / 2), argb);
            case LARGE_BOX     -> drawFilledBox(ctx, cx, cy, size, argb);
            case HOLLOW_BOX    -> drawHollowBox(ctx, cx, cy, size, thick, argb);
            case FOUR_CORNERS  -> drawFourCorners(ctx, cx, cy, size, thick, gap, argb);
            case DIAGONAL      -> drawDiagonal(ctx, cx, cy, size, thick, argb);
            case MINIMAL       -> drawMinimal(ctx, cx, cy, size, thick, argb);
            case ARROW         -> drawArrow(ctx, cx, cy, size, thick, argb);
        }
    }

    // ─── Shape renderers ──────────────────────────────────────────────────

    private static void drawCross(DrawContext ctx, int cx, int cy,
                                  int size, int thick, int gap, int argb) {
        int h = Math.max(0, thick / 2);
        ctx.fill(cx - size - gap, cy - h,     cx - gap,          cy + h + 1, argb); // left
        ctx.fill(cx + gap,        cy - h,     cx + size + gap + 1, cy + h + 1, argb); // right
        ctx.fill(cx - h,          cy - size - gap, cx + h + 1,  cy - gap,    argb); // top
        ctx.fill(cx - h,          cy + gap,        cx + h + 1,  cy + size + gap + 1, argb); // bottom
    }

    private static void drawDot(DrawContext ctx, int cx, int cy, int size, int argb) {
        int h = Math.max(1, size / 2);
        ctx.fill(cx - h, cy - h, cx + h + 1, cy + h + 1, argb);
    }

    private static void drawFilledBox(DrawContext ctx, int cx, int cy, int half, int argb) {
        int h = Math.max(1, half);
        ctx.fill(cx - h, cy - h, cx + h + 1, cy + h + 1, argb);
    }

    private static void drawHollowBox(DrawContext ctx, int cx, int cy,
                                       int size, int thick, int argb) {
        int t = Math.max(1, thick);
        ctx.fill(cx - size,     cy - size,          cx + size + 1,     cy - size + t,      argb); // top
        ctx.fill(cx - size,     cy + size - t + 1,  cx + size + 1,     cy + size + 1,      argb); // bottom
        ctx.fill(cx - size,     cy - size + t,      cx - size + t,     cy + size - t + 1,  argb); // left
        ctx.fill(cx + size - t + 1, cy - size + t,  cx + size + 1,     cy + size - t + 1,  argb); // right
    }

    private static void drawFourCorners(DrawContext ctx, int cx, int cy,
                                         int size, int thick, int gap, int argb) {
        int inner = gap;
        int outer = gap + size;
        int t     = Math.max(1, thick);
        // Top-left
        ctx.fill(cx - outer, cy - outer, cx - inner,     cy - outer + t, argb);
        ctx.fill(cx - outer, cy - outer, cx - outer + t, cy - inner,     argb);
        // Top-right
        ctx.fill(cx + inner + 1, cy - outer, cx + outer + 1,     cy - outer + t, argb);
        ctx.fill(cx + outer - t + 1, cy - outer, cx + outer + 1, cy - inner,     argb);
        // Bottom-left
        ctx.fill(cx - outer, cy + outer - t + 1, cx - inner,     cy + outer + 1, argb);
        ctx.fill(cx - outer, cy + inner + 1,     cx - outer + t, cy + outer + 1, argb);
        // Bottom-right
        ctx.fill(cx + inner + 1,     cy + outer - t + 1, cx + outer + 1, cy + outer + 1, argb);
        ctx.fill(cx + outer - t + 1, cy + inner + 1,     cx + outer + 1, cy + outer + 1, argb);
    }

    private static void drawFilledCircle(DrawContext ctx, int cx, int cy, int radius, int argb) {
        int r2 = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            int maxDx = (int) Math.sqrt(r2 - (long)dy * dy);
            ctx.fill(cx - maxDx, cy + dy, cx + maxDx + 1, cy + dy + 1, argb);
        }
    }

    private static void drawHollowCircle(DrawContext ctx, int cx, int cy,
                                          int radius, int thick, int argb) {
        int outer2 = radius * radius;
        int inner2 = (radius - thick) * (radius - thick);
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int d2 = dx * dx + dy * dy;
                if (d2 <= outer2 && d2 >= inner2) {
                    ctx.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, argb);
                }
            }
        }
    }

    private static void drawDiagonal(DrawContext ctx, int cx, int cy,
                                      int size, int thick, int argb) {
        for (int i = -size; i <= size; i++) {
            for (int t = 0; t < thick; t++) {
                ctx.fill(cx + i + t, cy + i, cx + i + t + 1, cy + i + 1, argb);   // \
                ctx.fill(cx + i + t, cy - i, cx + i + t + 1, cy - i + 1, argb);   // /
            }
        }
    }

    private static void drawMinimal(DrawContext ctx, int cx, int cy,
                                     int size, int thick, int argb) {
        int h = Math.max(0, thick / 2);
        ctx.fill(cx - size, cy - h, cx + size + 1, cy + h + 1, argb); // horizontal
        ctx.fill(cx - h, cy - size, cx + h + 1, cy + size + 1, argb); // vertical
    }

    private static void drawArrow(DrawContext ctx, int cx, int cy,
                                   int size, int thick, int argb) {
        int h = Math.max(0, thick / 2);
        // Vertical stem
        ctx.fill(cx - h, cy - size, cx + h + 1, cy + size / 2, argb);
        // Arrowhead (expanding rows downward)
        for (int i = 0; i <= size / 2; i++) {
            ctx.fill(cx - i - h, cy + size / 2 + i, cx + i + h + 2, cy + size / 2 + i + 1, argb);
        }
    }
}
