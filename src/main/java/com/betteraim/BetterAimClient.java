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
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class BetterAimClient implements ClientModInitializer {

    public static final String MOD_ID = "better-aim";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ── Custom keybind category ────────────────────────────────────────────
    private static final KeyBinding.Category BETTER_AIM_CATEGORY = KeyBinding.MISC_CATEGORY;

    // ── Keybindings ────────────────────────────────────────────────────────
    public static KeyBinding openConfigKey;
    public static KeyBinding toggleCrosshairKey;
    public static KeyBinding toggleTargetHudKey;
    public static KeyBinding toggleHighlightKey;
    public static KeyBinding toggleHitboxKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[BetterAIM] Initialising ...");

        ConfigManager.load();

        // ── Keybindings ────────────────────────────────────────────────────
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.better-aim.open_config",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M,
                BETTER_AIM_CATEGORY));

        toggleCrosshairKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.better-aim.toggle_crosshair",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
                BETTER_AIM_CATEGORY));

        toggleTargetHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.better-aim.toggle_target_hud",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
                BETTER_AIM_CATEGORY));

        toggleHighlightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.better-aim.toggle_highlight",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
                BETTER_AIM_CATEGORY));

        toggleHitboxKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.better-aim.toggle_hitbox",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
                BETTER_AIM_CATEGORY));

        // ── Tick events ────────────────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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

            TargetDetector.tick(client);
            HitFeedbackManager.tick();
        });

        // ── HUD rendering ──────────────────────────────────────────────────
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen != null) return;
            CrosshairRenderer.renderHud(drawContext, tickCounter);
            TargetHudRenderer.render(drawContext, tickCounter);
        });

        // ── World rendering ────────────────────────────────────────────────
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> {
            HighlightRenderer.render(context);
            HitboxRenderer.render(context);
        });

        LOGGER.info("[BetterAIM] Ready.");
    }
}
