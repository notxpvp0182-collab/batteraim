package com.betteraim.config;

import com.betteraim.crosshair.CrosshairStyle;

/**
 * Root configuration object for Better AIM.
 * Serialised to / deserialised from JSON by ConfigManager.
 * All fields have safe defaults.
 */
public class BetterAimConfig {

    // ── Global ─────────────────────────────────────────────────────────────
    public boolean enabled = true;

    // ── Crosshair perspectives ─────────────────────────────────────────────
    public boolean crosshairFirstPerson   = true;
    public boolean crosshairThirdPerson   = false;
    public boolean crosshairOtherCamera   = false;

    // ── Crosshair settings ─────────────────────────────────────────────────
    public CrosshairSettings normalCrosshair  = CrosshairSettings.defaultNormal();
    public CrosshairSettings hitableCrosshair = CrosshairSettings.defaultHitable();

    // ── Highlight ──────────────────────────────────────────────────────────
    public HighlightSettings playerHighlight = HighlightSettings.defaultPlayer();
    public HighlightSettings mobHighlight    = HighlightSettings.defaultMob();

    // ── Hitbox ─────────────────────────────────────────────────────────────
    public HitboxSettings hitbox = new HitboxSettings();

    // ── Hit feedback ───────────────────────────────────────────────────────
    public HitFeedbackSettings hitFeedback = new HitFeedbackSettings();

    // ── Target HUD ─────────────────────────────────────────────────────────
    public TargetHudSettings targetHud = new TargetHudSettings();

    // ───────────────────────────────────────────────────────────────────────
    // Validate / clamp every field after deserialisation.
    // ───────────────────────────────────────────────────────────────────────
    public void validate() {
        if (normalCrosshair  == null) normalCrosshair  = CrosshairSettings.defaultNormal();
        if (hitableCrosshair == null) hitableCrosshair = CrosshairSettings.defaultHitable();
        if (playerHighlight  == null) playerHighlight  = HighlightSettings.defaultPlayer();
        if (mobHighlight     == null) mobHighlight     = HighlightSettings.defaultMob();
        if (hitbox           == null) hitbox           = new HitboxSettings();
        if (hitFeedback      == null) hitFeedback      = new HitFeedbackSettings();
        if (targetHud        == null) targetHud        = new TargetHudSettings();

        normalCrosshair.validate();
        hitableCrosshair.validate();
        playerHighlight.validate();
        mobHighlight.validate();
        hitbox.validate();
        hitFeedback.validate();
        targetHud.validate();
    }

    // ── Crosshair settings inner class ─────────────────────────────────────
    public static class CrosshairSettings {
        public boolean enabled    = true;
        public String  style      = CrosshairStyle.CROSS.id;
        public int     size       = 6;
        public int     thickness  = 2;
        public int     gap        = 4;
        public float   opacity    = 1.0f;
        public int     color      = 0xFFFFFF;  // RGB; alpha controlled by opacity

        public static CrosshairSettings defaultNormal() {
            CrosshairSettings s = new CrosshairSettings();
            s.style = CrosshairStyle.CROSS.id;
            s.color = 0xFFFFFF;
            return s;
        }

        public static CrosshairSettings defaultHitable() {
            CrosshairSettings s = new CrosshairSettings();
            s.style   = CrosshairStyle.HOLLOW_CIRCLE.id;
            s.size    = 8;
            s.gap     = 0;
            s.color   = 0x00FF00;   // green
            return s;
        }

        public void validate() {
            if (CrosshairStyle.fromId(style) == null) style = CrosshairStyle.CROSS.id;
            size      = clamp(size,      1, 64);
            thickness = clamp(thickness, 1, 32);
            gap       = clamp(gap,       0, 32);
            opacity   = clampF(opacity,  0.05f, 1.0f);
            color     = color & 0xFFFFFF;
        }

        public CrosshairStyle getStyle() { return CrosshairStyle.fromId(style); }

        /** Returns an ARGB int with the configured opacity baked in. */
        public int argb() {
            int a = (int)(opacity * 255) & 0xFF;
            return (a << 24) | (color & 0xFFFFFF);
        }
    }

    // ── Highlight settings ─────────────────────────────────────────────────
    public static class HighlightSettings {
        public boolean enabled      = true;
        public int     color        = 0x00FF00;
        public float   opacity      = 0.5f;
        public boolean outline      = true;
        public int     outlineColor = 0x00CC00;
        public float   outlineOpacity = 0.9f;
        public float   outlineWidth = 2.0f;

        public static HighlightSettings defaultPlayer() {
            HighlightSettings s = new HighlightSettings();
            s.color        = 0x00FF00;
            s.outlineColor = 0x00CC00;
            return s;
        }

        public static HighlightSettings defaultMob() {
            HighlightSettings s = new HighlightSettings();
            s.enabled      = false;
            s.color        = 0xFF8800;
            s.outlineColor = 0xCC6600;
            return s;
        }

        public void validate() {
            color          = color & 0xFFFFFF;
            outlineColor   = outlineColor & 0xFFFFFF;
            opacity        = clampF(opacity, 0f, 1f);
            outlineOpacity = clampF(outlineOpacity, 0f, 1f);
            outlineWidth   = clampF(outlineWidth, 0.5f, 10f);
        }

        public int lineArgb() {
            int a = (int)(outlineOpacity * 255) & 0xFF;
            return (a << 24) | (outlineColor & 0xFFFFFF);
        }
    }

    // ── Hitbox settings ────────────────────────────────────────────────────
    public static class HitboxSettings {
        public boolean enabled      = false;
        public boolean showPlayers  = true;
        public boolean showMobs     = true;
        public int     color        = 0x0088FF;
        public float   opacity      = 0.9f;
        public float   lineWidth    = 2.0f;
        public boolean fill         = false;
        public int     fillColor    = 0x0055AA;
        public float   fillOpacity  = 0.15f;

        public void validate() {
            color       = color & 0xFFFFFF;
            fillColor   = fillColor & 0xFFFFFF;
            opacity     = clampF(opacity, 0f, 1f);
            fillOpacity = clampF(fillOpacity, 0f, 1f);
            lineWidth   = clampF(lineWidth, 0.5f, 10f);
        }

        public int lineArgb() {
            int a = (int)(opacity * 255) & 0xFF;
            return (a << 24) | (color & 0xFFFFFF);
        }

        public int fillArgb() {
            int a = (int)(fillOpacity * 255) & 0xFF;
            return (a << 24) | (fillColor & 0xFFFFFF);
        }
    }

    // ── Hit feedback settings ──────────────────────────────────────────────
    public static class HitFeedbackSettings {
        public boolean enabled           = true;
        public int     color             = 0xFF2222;
        public float   opacity           = 0.9f;
        public int     durationTicks     = 6;
        public boolean crosshairFeedback = true;
        public boolean highlightFeedback = true;
        public boolean hudFeedback       = true;

        public void validate() {
            color         = color & 0xFFFFFF;
            opacity       = clampF(opacity, 0f, 1f);
            durationTicks = clamp(durationTicks, 1, 60);
        }

        public int argb() {
            int a = (int)(opacity * 255) & 0xFF;
            return (a << 24) | (color & 0xFFFFFF);
        }
    }

    // ── Target HUD settings ────────────────────────────────────────────────
    public static class TargetHudSettings {
        public boolean enabled       = true;
        public boolean showLabel     = true;
        public boolean showName      = true;
        public boolean showPreview   = true;

        public boolean background    = true;
        public int     bgColor       = 0x111111;
        public float   bgOpacity     = 0.70f;

        public boolean border        = true;
        public int     borderColor   = 0x444444;
        public float   borderOpacity = 0.85f;
        public int     borderWidth   = 1;

        public int     textColor     = 0xFFFFFF;
        public float   textOpacity   = 1.0f;

        public float   scale         = 1.0f;
        public int     posX          = -1;   // -1 = auto (top-right)
        public int     posY          = -1;   // -1 = auto

        public void validate() {
            bgColor     = bgColor & 0xFFFFFF;
            borderColor = borderColor & 0xFFFFFF;
            textColor   = textColor & 0xFFFFFF;
            bgOpacity     = clampF(bgOpacity,     0f, 1f);
            borderOpacity = clampF(borderOpacity, 0f, 1f);
            textOpacity   = clampF(textOpacity,   0f, 1f);
            borderWidth   = clamp(borderWidth, 1, 8);
            scale         = clampF(scale, 0.5f, 3.0f);
        }

        public int bgArgb() {
            int a = (int)(bgOpacity * 255) & 0xFF;
            return (a << 24) | (bgColor & 0xFFFFFF);
        }

        public int borderArgb() {
            int a = (int)(borderOpacity * 255) & 0xFF;
            return (a << 24) | (borderColor & 0xFFFFFF);
        }

        public int textArgb() {
            int a = (int)(textOpacity * 255) & 0xFF;
            return (a << 24) | (textColor & 0xFFFFFF);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private static int   clamp(int v, int min, int max)     { return Math.max(min, Math.min(max, v)); }
    private static float clampF(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
}
