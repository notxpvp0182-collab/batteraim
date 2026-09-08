package com.betteraim.target;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Lightweight client-side target detection.
 * Updated once per client tick via crosshairTarget – no world scan.
 *
 * DESIGN NOTE:
 * All detection is read-only and purely visual.
 * Nothing here modifies reach, attack speed, or combat mechanics.
 */
@Environment(EnvType.CLIENT)
public final class TargetDetector {

    public enum State { NONE, TARGETED, HITABLE }

    /** Standard survival attack reach + small tolerance. */
    private static final float ATTACK_REACH = 3.5f;

    private static LivingEntity currentTarget = null;
    private static State        currentState  = State.NONE;

    private TargetDetector() {}

    // ── Called once per client tick ────────────────────────────────────────

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            currentTarget = null;
            currentState  = State.NONE;
            return;
        }

        // Use Minecraft's own crosshair result – no additional ray-cast needed.
        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
            currentTarget = null;
            currentState  = State.NONE;
            return;
        }

        Entity hitEntity = ((EntityHitResult) hit).getEntity();

        if (!(hitEntity instanceof LivingEntity living) || !living.isAlive()) {
            currentTarget = null;
            currentState  = State.NONE;
            return;
        }

        currentTarget = living;
        currentState  = canAttack(client, living) ? State.HITABLE : State.TARGETED;
    }

    // ── Public queries ─────────────────────────────────────────────────────

    public static LivingEntity getCurrentTarget()  { return currentTarget; }
    public static State        getCurrentState()   { return currentState;  }
    public static boolean      hasTarget()         { return currentTarget != null; }
    public static boolean      isTargetHitable()   { return currentState == State.HITABLE; }

    public static boolean isPlayer(Entity e) { return e instanceof PlayerEntity; }
    public static boolean isMob(Entity e)    { return e instanceof MobEntity; }

    // ── Internal ────────────────────────────────────────────────────────────

    private static boolean canAttack(MinecraftClient client, LivingEntity entity) {
        if (client.player == null) return false;
        if (!entity.isAlive())      return false;
        if (entity == client.player) return false;
        // Use simple distance check – avoids deprecated or version-varying
        // reach-distance APIs that differ between 1.21.x sub-versions.
        return (float) client.player.distanceTo(entity) <= ATTACK_REACH;
    }
}
