package com.seibel.lod.fabric.wrappers.config;

import com.seibel.lod.common.wrappers.config.ConfigGui;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * For making the config show up in modmenu
 */
@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    // For the custom config code
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> ConfigGui.getScreen(parent, "");
    }
}
