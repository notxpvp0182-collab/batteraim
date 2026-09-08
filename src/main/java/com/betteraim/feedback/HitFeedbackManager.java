package com.betteraim.feedback;

import com.betteraim.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;

/**
 * Tracks whether the player just hit a target and for how long to show feedback.
 * Triggered by the attack mixin; consumed by renderers.
 */
@Environment(EnvType.CLIENT)
public final class HitFeedbackManager {

    private static LivingEntity hitTarget      = null;
    private static int          remainingTicks  = 0;

    private HitFeedbackManager() {}

    // ── Called from mixin on every attack ─────────────────────────────────

    public static void triggerHit(LivingEntity target) {
        hitTarget     = target;
        remainingTicks = ConfigManager.getConfig().hitFeedback.durationTicks;
    }

    // ── Called once per client tick ────────────────────────────────────────

    public static void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks <= 0) {
                hitTarget     = null;
                remainingTicks = 0;
            }
        }
    }

    // ── Public queries ─────────────────────────────────────────────────────

    public static boolean isActive()                  { return remainingTicks > 0; }
    public static LivingEntity getHitTarget()         { return hitTarget; }
    public static boolean isHitTarget(LivingEntity e) { return hitTarget == e && isActive(); }
}
