/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.wynntils.core.persisted.config.Config;
import com.wynntils.screens.settings.WynntilsBookSettingsScreen;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class BooleanSettingsButton extends GeneralSettingsButton {
    private final Config<Boolean> config;

    public BooleanSettingsButton(Config<Boolean> config, float translationX, float translationY) {
        super(
                0,
                7,
                100,
                20,
                getTitle(config),
                ComponentUtils.wrapTooltips(List.of(Component.literal(config.getDescription())), 150),
                translationX,
                translationY);
        this.config = config;
    }

    @Override
    public void onPress() {
        config.setValue(!isEnabled(config));
        setMessage(getTitle(config));

        // Reload the configurables in case the enabled button was toggled so the checkboxes
        // can change state
        if (McUtils.mc().screen instanceof WynntilsBookSettingsScreen bookSettingsScreen) {
            bookSettingsScreen.populateConfigurables();
        }
    }

    private static MutableComponent getTitle(Config<Boolean> config) {
        return isEnabled(config)
                ? Component.translatable("screens.wynntils.settingsScreen.booleanConfig.enabled")
                : Component.translatable("screens.wynntils.settingsScreen.booleanConfig.disabled");
    }

    @Override
    protected CustomColor getTextColor() {
        return isEnabled(config) ? CommonColors.GREEN : CommonColors.RED;
    }

    private static boolean isEnabled(Config<Boolean> config) {
        return config.get();
    }
}
