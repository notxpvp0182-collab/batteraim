package com.betteraim;

import com.betteraim.config.BetterAimConfig;
import com.betteraim.config.ConfigManager;
import com.betteraim.crosshair.CrosshairRenderer;
import com.betteraim.feedback.HitFeedbackManager;
import com.betteraim.gui.BetterAimScreen;
import com.betteraim.highlight.HighlightRenderer;
import com.betteraim.hitbox.HitboxRenderer;
import com.betteraim.hud.TargetHudRenderer;
import com.betteraim.target.TargetDetector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side mod initialiser for Better AIM.
 *
 * Registers:
 *  - Configuration loading
 *  - Keybindings (M = open config; others unbound by default)
 *  - Tick event: keybind handling + target detection + hit feedback timing
 *  - HUD render: custom crosshair + target HUD
 *  - World render: entity highlights + hitbox visualization
 */
@Environment(EnvType.CLIENT)
public class BetterAimClient implements ClientModInitializer {

    public static final String MOD_ID = "better-aim";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ── Keybindings ────────────────────────────────────────────────────────
    public static KeyBinding openConfigKey;
    public static KeyBinding toggleCrosshairKey;
    public static KeyBinding toggleTargetHudKey;
    public static KeyBinding toggleHighlightKey;
    public static KeyBinding toggleHitboxKey;

    private static final String KEY_CATEGORY = "category.better-aim";

    @Override
    public void onInitializeClient() {
        LOGGER.info("[BetterAIM] Initialising …");

        // ── 1. Load config ─────────────────────────────────────────────────
        ConfigManager.load();

        // ── 2. Register keybindings ────────────────────────────────────────
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.better-aim.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                KEY_CATEGORY));

        toggleCrosshairKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.better-aim.toggle_crosshair",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY));

        toggleTargetHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.better-aim.toggle_target_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY));

        toggleHighlightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.better-aim.toggle_highlight",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY));

        toggleHitboxKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.better-aim.toggle_hitbox",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY));

        // ── 3. Tick events ─────────────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Keybind handling
            while (openConfigKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new BetterAimScreen(null));
                }
            }
            while (toggleCrosshairKey.wasPressed()) {
                BetterAimConfig cfg = ConfigManager.getConfig();
                boolean cur = cfg.normalCrosshair.enabled;
                cfg.normalCrosshair.enabled  = !cur;
                cfg.hitableCrosshair.enabled = !cur;
                ConfigManager.save();
            }
            while (toggleTargetHudKey.wasPressed()) {
                BetterAimConfig cfg = ConfigManager.getConfig();
                cfg.targetHud.enabled = !cfg.targetHud.enabled;
                ConfigManager.save();
            }
            while (toggleHighlightKey.wasPressed()) {
                BetterAimConfig cfg = ConfigManager.getConfig();
                boolean cur = cfg.playerHighlight.enabled || cfg.mobHighlight.enabled;
                cfg.playerHighlight.enabled = !cur;
                cfg.mobHighlight.enabled    = !cur;
                ConfigManager.save();
            }
            while (toggleHitboxKey.wasPressed()) {
                BetterAimConfig cfg = ConfigManager.getConfig();
                cfg.hitbox.enabled = !cfg.hitbox.enabled;
                ConfigManager.save();
            }

            // Target detection (cached per-tick, not per-frame)
            TargetDetector.tick(client);

            // Hit feedback countdown
            HitFeedbackManager.tick();
        });

        // ── 4. HUD rendering ───────────────────────────────────────────────
        // Fires every frame while in-game, after all vanilla HUD elements.
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            // Skip if a screen is open (avoids drawing crosshair over GUIs)
            if (client.currentScreen != null) return;
            CrosshairRenderer.renderHud(drawContext, tickCounter);
            TargetHudRenderer.render(drawContext, tickCounter);
        });

        // ── 5. World rendering ─────────────────────────────────────────────
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            HighlightRenderer.render(context);
            HitboxRenderer.render(context);
        });

        LOGGER.info("[BetterAIM] Ready.");
    }
}
