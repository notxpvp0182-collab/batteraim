package com.betteraim.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

/**
 * Rendering utilities shared across Better AIM renderers.
 */
@Environment(EnvType.CLIENT)
public final class RenderUtil {

    private RenderUtil() {}

    /**
     * Draw a 3-D wireframe box using the LINES draw mode.
     * Caller must set up the shader, blend state, and line width beforehand.
     *
     * @param buf    Buffer already started with DrawMode.LINES + VertexFormats.LINES
     * @param mat    Current position matrix
     * @param norm   Current normal matrix
     * @param box    Box in world space (caller must have applied camera translation)
     * @param r,g,b,a Components 0-1
     */
    public static void addBoxLines(BufferBuilder buf, Matrix4f mat, Matrix3f norm,
                                   Box box, float r, float g, float b, float a) {
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        // Bottom face
        addLine(buf, mat, norm, x1, y1, z1, x2, y1, z1, r, g, b, a);
        addLine(buf, mat, norm, x2, y1, z1, x2, y1, z2, r, g, b, a);
        addLine(buf, mat, norm, x2, y1, z2, x1, y1, z2, r, g, b, a);
        addLine(buf, mat, norm, x1, y1, z2, x1, y1, z1, r, g, b, a);
        // Top face
        addLine(buf, mat, norm, x1, y2, z1, x2, y2, z1, r, g, b, a);
        addLine(buf, mat, norm, x2, y2, z1, x2, y2, z2, r, g, b, a);
        addLine(buf, mat, norm, x2, y2, z2, x1, y2, z2, r, g, b, a);
        addLine(buf, mat, norm, x1, y2, z2, x1, y2, z1, r, g, b, a);
        // Vertical edges
        addLine(buf, mat, norm, x1, y1, z1, x1, y2, z1, r, g, b, a);
        addLine(buf, mat, norm, x2, y1, z1, x2, y2, z1, r, g, b, a);
        addLine(buf, mat, norm, x2, y1, z2, x2, y2, z2, r, g, b, a);
        addLine(buf, mat, norm, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    /** Add a single line segment (2 vertices) to an already-started LINES buffer. */
    public static void addLine(BufferBuilder buf, Matrix4f mat, Matrix3f norm,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0f) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;

        buf.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(norm, nx, ny, nz);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(norm, nx, ny, nz);
    }

    /**
     * Begin a LINES draw session.  Returns the BufferBuilder ready for vertices.
     * Call {@link #endLines(BufferBuilder)} when done.
     */
    public static BufferBuilder beginLines(float lineWidth) {
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
    }

    /**
     * Flush and draw the line buffer.  Safe to call even if no vertices were added.
     */
    public static void endLines(BufferBuilder buf) {
        try {
            BuiltBuffer built = buf.end();
            BufferRenderer.drawWithGlobalProgram(built);
        } catch (Exception ignored) {
            // empty buffer — nothing to draw
        }
    }

    /** Restore sane render state after world-space line drawing. */
    public static void restoreState() {
        RenderSystem.lineWidth(1f);
        RenderSystem.disableBlend();
    }
}
