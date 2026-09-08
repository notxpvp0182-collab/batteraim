package com.betteraim.util;

public final class ColorUtil {

    private ColorUtil() {}

    /** Extract red component (0-255) from ARGB. */
    public static int red(int argb)   { return (argb >> 16) & 0xFF; }
    /** Extract green component (0-255) from ARGB. */
    public static int green(int argb) { return (argb >> 8)  & 0xFF; }
    /** Extract blue component (0-255) from ARGB. */
    public static int blue(int argb)  { return argb & 0xFF; }
    /** Extract alpha component (0-255) from ARGB. */
    public static int alpha(int argb) { return (argb >> 24) & 0xFF; }

    /** Convert 0-255 ARGB components to packed int. */
    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /** Build ARGB from an RGB int and a 0-1 opacity. */
    public static int withOpacity(int rgb, float opacity) {
        int a = (int)(clamp01(opacity) * 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** Build ARGB from an RGB int and a 0-255 alpha. */
    public static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    /** Red component as 0-1 float. */
    public static float rf(int argb) { return red(argb)   / 255f; }
    /** Green component as 0-1 float. */
    public static float gf(int argb) { return green(argb) / 255f; }
    /** Blue component as 0-1 float. */
    public static float bf(int argb) { return blue(argb)  / 255f; }
    /** Alpha component as 0-1 float. */
    public static float af(int argb) { return alpha(argb) / 255f; }

    /** Convert RGB to HSV.  Returns float[3] = {h(0-360), s(0-1), v(0-1)}. */
    public static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        float h = 0, s = (max == 0) ? 0 : delta / max, v = max;
        if (delta != 0) {
            if (max == rf)      h = 60f * (((gf - bf) / delta) % 6);
            else if (max == gf) h = 60f * (((bf - rf) / delta) + 2);
            else                h = 60f * (((rf - gf) / delta) + 4);
        }
        if (h < 0) h += 360;
        return new float[]{ h, s, v };
    }

    /** Convert HSV to packed RGB int. */
    public static int hsvToRgb(float h, float s, float v) {
        int hi = (int)(h / 60f) % 6;
        float f = h / 60f - (int)(h / 60f);
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (hi) {
            case 0  -> { r = v; g = t; b = p; }
            case 1  -> { r = q; g = v; b = p; }
            case 2  -> { r = p; g = v; b = t; }
            case 3  -> { r = p; g = q; b = v; }
            case 4  -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int)(r*255) << 16) | ((int)(g*255) << 8) | (int)(b*255);
    }

    /** Format packed ARGB (or RGB) as upper-case 6-digit hex, without alpha. */
    public static String toHex(int argb) {
        return String.format("%06X", argb & 0xFFFFFF);
    }

    /** Parse a 6-digit hex string to packed RGB.  Returns defaultVal on error. */
    public static int fromHex(String hex, int defaultVal) {
        if (hex == null) return defaultVal;
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return (int) Long.parseLong(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
}
