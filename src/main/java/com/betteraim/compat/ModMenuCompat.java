package com.betteraim.compat;

import com.betteraim.gui.BetterAimScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Provides the Better AIM configuration screen to Mod Menu.
 * Listed under the "modmenu" entrypoint in fabric.mod.json.
 */
@Environment(EnvType.CLIENT)
public class ModMenuCompat implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BetterAimScreen::new;
    }
}
