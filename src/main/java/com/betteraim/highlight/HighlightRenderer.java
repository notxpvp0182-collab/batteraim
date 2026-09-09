package com.betteraim.highlight;

import com.betteraim.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

/**
 * Entity highlight renderer.
 * Pending port to the 1.21.11 GPU pipeline rendering API.
 */
@Environment(EnvType.CLIENT)
public final class HighlightRenderer {

    private HighlightRenderer() {}

    public static void render(WorldRenderContext context) {
        if (!ConfigManager.getConfig().enabled) return;
        // TODO: implement with 1.21.11 RenderPipeline API
    }
}
