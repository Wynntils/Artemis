/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.quilt;

import com.wynntils.core.WynntilsMod;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class WynntilsModQuilt implements ClientModInitializer {
    @Override
    public void onInitializeClient(ModContainer mod) {
        WynntilsMod.init(mod.metadata().version().raw(), QuiltLoader.isDevelopmentEnvironment());
    }
}
