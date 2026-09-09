package com.betteraim.hitbox;

import com.betteraim.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

/**
 * Hitbox visualisation.
 * 1.21.11 uses a new GPU pipeline rendering API (RenderPipelines + GpuBuffer).
 * World-space line rendering is pending full port to the new API.
 */
@Environment(EnvType.CLIENT)
public final class HitboxRenderer {

    private HitboxRenderer() {}

    public static void render(WorldRenderContext context) {
        if (!ConfigManager.getConfig().enabled) return;
        if (!ConfigManager.getConfig().hitbox.enabled) return;
        // TODO: implement with 1.21.11 RenderPipeline API
    }
}
