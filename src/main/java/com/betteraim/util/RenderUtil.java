package com.betteraim.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Rendering utilities - 1.21.11 uses a new GPU pipeline API.
 * World-space rendering is now handled directly via custom RenderPipelines.
 * These stubs compile cleanly; world rendering is implemented in
 * HitboxRenderer and HighlightRenderer using the new Fabric API.
 */
@Environment(EnvType.CLIENT)
public final class RenderUtil {

    private RenderUtil() {}

    /** No-op stub - old tessellator approach removed in 1.21.11. */
    public static void addBoxLines(Object buf, Matrix4f mat, Matrix3f norm,
                                   Box box, float r, float g, float b, float a) {}

    public static void addLine(Object buf, Matrix4f mat, Matrix3f norm,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float r, float g, float b, float a) {}

    /** No-op stub. */
    public static Object beginLines(float lineWidth) { return null; }

    /** No-op stub. */
    public static void endLines(Object buf) {}

    /** No-op stub. */
    public static void restoreState() {}
}
