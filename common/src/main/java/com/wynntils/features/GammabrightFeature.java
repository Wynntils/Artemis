/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features;

import com.wynntils.core.components.Managers;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.features.Feature;
import com.wynntils.core.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.mod.event.WynncraftConnectionEvent;
import com.wynntils.core.storage.Storage;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.mc.McUtils;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.UNCATEGORIZED)
public class GammabrightFeature extends Feature {
    @RegisterConfig
    public final Config<Boolean> gammabrightEnabled = new Config<>(false);

    private Storage<Double> lastGamma = new Storage<>(1.0);

    @RegisterKeyBind
    private final KeyBind gammabrightKeyBind =
            new KeyBind("Gammabright", GLFW.GLFW_KEY_G, true, this::toggleGammaBright);

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() != WorldState.WORLD) return;

        applyGammabright();
    }

    @SubscribeEvent
    public void onDisconnect(WynncraftConnectionEvent.Disconnected event) {
        if (gammabrightEnabled.get()) {
            resetGamma();
        }
    }

    @Override
    protected void onConfigUpdate(ConfigHolder configHolder) {
        if (configHolder.getFieldName().equals("gammabrightEnabled")) {
            applyGammabright();
        }
    }

    @Override
    public void onEnable() {
        if (gammabrightEnabled.get() && McUtils.options().gamma().get() != 1000d) {
            enableGammabright();
        }
    }

    @Override
    public void onDisable() {
        resetGamma();
    }

    private void applyGammabright() {
        if (!isEnabled()) return;
        if (gammabrightEnabled.get() && McUtils.options().gamma().get() == 1000d) return;

        if (gammabrightEnabled.get()) {
            enableGammabright();
        } else {
            resetGamma();
        }
    }

    private void toggleGammaBright() {
        gammabrightEnabled.updateConfig(!gammabrightEnabled.get());
        applyGammabright();

        Managers.Config.saveConfig();
    }

    private void resetGamma() {
        McUtils.options().gamma().value = lastGamma.get();
    }

    private void enableGammabright() {
        lastGamma.store(McUtils.options().gamma().get());
        McUtils.options().gamma().value = 1000d;
    }
}
