package com.betteraim.highlight;

import com.betteraim.config.BetterAimConfig;
import com.betteraim.config.ConfigManager;
import com.betteraim.feedback.HitFeedbackManager;
import com.betteraim.util.ColorUtil;
import com.betteraim.util.RenderUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders coloured wireframe outlines around players and mobs.
 * Uses WorldRenderEvents.AFTER_ENTITIES – no per-frame world scan.
 * Only searches a capped radius around the player.
 */
@Environment(EnvType.CLIENT)
public final class HighlightRenderer {

    private static final double SEARCH_RADIUS = 32.0;

    private HighlightRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        BetterAimConfig cfg = ConfigManager.getConfig();
        if (!cfg.enabled) return;

        boolean needPlayers = cfg.playerHighlight.enabled;
        boolean needMobs    = cfg.mobHighlight.enabled;
        if (!needPlayers && !needMobs) return;

        // Collect entities within radius
        Box search = client.player.getBoundingBox().expand(SEARCH_RADIUS);
        List<Entity> nearby = client.world.getOtherEntities(
                client.player, search, e -> e instanceof LivingEntity && e.isAlive());
        if (nearby.isEmpty()) return;

        // Build a list of entries with resolved colors
        record Entry(Box box, float r, float g, float b, float a) {}
        List<Entry> entries = new ArrayList<>();

        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity living)) continue;

            boolean isPlayer = living instanceof PlayerEntity;
            boolean isMob    = living instanceof MobEntity;

            BetterAimConfig.HighlightSettings hs;
            if (isPlayer && needPlayers)     hs = cfg.playerHighlight;
            else if (isMob && needMobs)      hs = cfg.mobHighlight;
            else continue;

            int argb;
            if (HitFeedbackManager.isHitTarget(living) && cfg.hitFeedback.highlightFeedback) {
                argb = cfg.hitFeedback.argb();
            } else {
                argb = hs.lineArgb();
            }

            entries.add(new Entry(
                    living.getBoundingBox(),
                    ColorUtil.rf(argb), ColorUtil.gf(argb),
                    ColorUtil.bf(argb), ColorUtil.af(argb)
            ));
        }
        if (entries.isEmpty()) return;

        // Render
        MatrixStack matrices = context.matrixStack();
        Vec3d cam = context.camera().getPos();

        BufferBuilder buf = RenderUtil.beginLines(2.0f);
        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        Matrix4f mat  = matrices.peek().getPositionMatrix();
        Matrix3f norm = matrices.peek().getNormalMatrix();

        for (Entry e : entries) {
            RenderUtil.addBoxLines(buf, mat, norm, e.box(), e.r(), e.g(), e.b(), e.a());
        }

        matrices.pop();
        RenderUtil.endLines(buf);
        RenderUtil.restoreState();
    }
}
