/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.utilities;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.LightmapEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.UTILITIES)
public class GammabrightFeature extends Feature {
    @Persisted
    public final Config<Boolean> gammabrightEnabled = new Config<>(false);

    @RegisterKeyBind
    private final KeyBind gammabrightKeyBind =
            new KeyBind("Gammabright", GLFW.GLFW_KEY_G, true, this::toggleGammaBright);

    @SubscribeEvent
    public void onLightmapUpdate(LightmapEvent lightmapEvent) {
        if (gammabrightEnabled.get()) {
            lightmapEvent.setRgb(0xFFFFFFFF);
        }
    }

    private void toggleGammaBright() {
        gammabrightEnabled.store(!gammabrightEnabled.get());
        gammabrightEnabled.touched();
    }
}
