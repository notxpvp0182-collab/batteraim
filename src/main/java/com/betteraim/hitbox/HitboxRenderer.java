package com.betteraim.hitbox;

import com.betteraim.config.BetterAimConfig;
import com.betteraim.config.ConfigManager;
import com.betteraim.util.ColorUtil;
import com.betteraim.util.RenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
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
 * Draws configurable wireframe bounding boxes around entities.
 * Completely independent from highlight, crosshair, and HUD systems.
 */
@Environment(EnvType.CLIENT)
public final class HitboxRenderer {

    private static final double SEARCH_RADIUS = 32.0;

    // ─── Package-visible entry to avoid anonymous/local-type issues ────────
    static final class BoxEntry {
        final Box   box;
        final float r, g, b, a;            // line colour
        final float fr, fg, fb, fa;        // fill colour
        BoxEntry(Box box,
                 float r,  float g,  float b,  float a,
                 float fr, float fg, float fb, float fa) {
            this.box = box;
            this.r = r; this.g = g; this.b = b; this.a = a;
            this.fr = fr; this.fg = fg; this.fb = fb; this.fa = fa;
        }
    }

    private HitboxRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        BetterAimConfig cfg = ConfigManager.getConfig();
        if (!cfg.enabled || !cfg.hitbox.enabled) return;

        Box search = client.player.getBoundingBox().expand(SEARCH_RADIUS);
        List<Entity> nearby = client.world.getOtherEntities(
                client.player, search,
                e -> e instanceof LivingEntity && e.isAlive());
        if (nearby.isEmpty()) return;

        int lineArgb = cfg.hitbox.lineArgb();
        int fillArgb = cfg.hitbox.fillArgb();
        float lr = ColorUtil.rf(lineArgb), lg = ColorUtil.gf(lineArgb),
              lb = ColorUtil.bf(lineArgb), la = ColorUtil.af(lineArgb);
        float fr = ColorUtil.rf(fillArgb), fg = ColorUtil.gf(fillArgb),
              fb = ColorUtil.bf(fillArgb), fa = ColorUtil.af(fillArgb);

        List<BoxEntry> entries = new ArrayList<>();
        for (Entity entity : nearby) {
            boolean isPlayer = entity instanceof PlayerEntity;
            boolean isMob    = entity instanceof MobEntity;
            if (isPlayer && !cfg.hitbox.showPlayers) continue;
            if (isMob    && !cfg.hitbox.showMobs)    continue;
            if (!isPlayer && !isMob) continue;
            entries.add(new BoxEntry(entity.getBoundingBox(),
                    lr, lg, lb, la, fr, fg, fb, fa));
        }
        if (entries.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d cam = context.camera().getPos();

        // ── 1. Wireframe lines ────────────────────────────────────────────
        BufferBuilder lineBuf = RenderUtil.beginLines(cfg.hitbox.lineWidth);

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f mat  = matrices.peek().getPositionMatrix();
        Matrix3f norm = matrices.peek().getNormalMatrix();

        for (BoxEntry e : entries) {
            RenderUtil.addBoxLines(lineBuf, mat, norm, e.box, e.r, e.g, e.b, e.a);
        }
        matrices.pop();
        RenderUtil.endLines(lineBuf);

        // ── 2. Optional transparent fill ──────────────────────────────────
        if (cfg.hitbox.fill) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);

            Tessellator tess  = Tessellator.getInstance();
            BufferBuilder fillBuf = tess.begin(VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_COLOR);

            matrices.push();
            matrices.translate(-cam.x, -cam.y, -cam.z);
            mat = matrices.peek().getPositionMatrix();

            for (BoxEntry e : entries) {
                addBoxQuads(fillBuf, mat, e.box, e.fr, e.fg, e.fb, e.fa);
            }
            matrices.pop();

            try { BufferRenderer.drawWithGlobalProgram(fillBuf.end()); }
            catch (Exception ignored) {}

            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }

        RenderUtil.restoreState();
    }

    private static void addBoxQuads(BufferBuilder buf, Matrix4f mat,
                                    Box box, float r, float g, float b, float a) {
        float x1=(float)box.minX, y1=(float)box.minY, z1=(float)box.minZ;
        float x2=(float)box.maxX, y2=(float)box.maxY, z2=(float)box.maxZ;
        // Bottom
        buf.vertex(mat,x1,y1,z1).color(r,g,b,a); buf.vertex(mat,x2,y1,z1).color(r,g,b,a);
        buf.vertex(mat,x2,y1,z2).color(r,g,b,a); buf.vertex(mat,x1,y1,z2).color(r,g,b,a);
        // Top
        buf.vertex(mat,x1,y2,z1).color(r,g,b,a); buf.vertex(mat,x1,y2,z2).color(r,g,b,a);
        buf.vertex(mat,x2,y2,z2).color(r,g,b,a); buf.vertex(mat,x2,y2,z1).color(r,g,b,a);
        // North
        buf.vertex(mat,x1,y1,z1).color(r,g,b,a); buf.vertex(mat,x1,y2,z1).color(r,g,b,a);
        buf.vertex(mat,x2,y2,z1).color(r,g,b,a); buf.vertex(mat,x2,y1,z1).color(r,g,b,a);
        // South
        buf.vertex(mat,x1,y1,z2).color(r,g,b,a); buf.vertex(mat,x2,y1,z2).color(r,g,b,a);
        buf.vertex(mat,x2,y2,z2).color(r,g,b,a); buf.vertex(mat,x1,y2,z2).color(r,g,b,a);
        // West
        buf.vertex(mat,x1,y1,z1).color(r,g,b,a); buf.vertex(mat,x1,y1,z2).color(r,g,b,a);
        buf.vertex(mat,x1,y2,z2).color(r,g,b,a); buf.vertex(mat,x1,y2,z1).color(r,g,b,a);
        // East
        buf.vertex(mat,x2,y1,z1).color(r,g,b,a); buf.vertex(mat,x2,y2,z1).color(r,g,b,a);
        buf.vertex(mat,x2,y2,z2).color(r,g,b,a); buf.vertex(mat,x2,y1,z2).color(r,g,b,a);
    }
}
