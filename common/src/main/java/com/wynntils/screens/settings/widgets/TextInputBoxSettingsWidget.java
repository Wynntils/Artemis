/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.wynntils.core.persisted.config.Config;
import com.wynntils.screens.base.TextboxScreen;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.ComponentUtils;
import java.util.List;
import net.minecraft.network.chat.Component;

public class TextInputBoxSettingsWidget<T> extends TextInputBoxWidget {
    protected final Config<T> config;

    protected TextInputBoxSettingsWidget(Config<T> config, TextboxScreen textboxScreen, int width) {
        super(0, 7, width, 20, null, textboxScreen);
        this.config = config;
        setTextBoxInput(config.get().toString());
        tooltip = ComponentUtils.wrapTooltips(List.of(Component.literal(config.getDescription())), 150);
    }

    public TextInputBoxSettingsWidget(Config<T> config, TextboxScreen textboxScreen) {
        this(config, textboxScreen, 100);
    }

    @Override
    protected void onUpdate(String text) {
        T parsedValue = config.tryParseStringValue(text);
        if (parsedValue != null) {
            if (!parsedValue.equals(config.get())) {
                config.setValue(parsedValue);
            }

            setRenderColor(CommonColors.GREEN);
        } else {
            setRenderColor(CommonColors.RED);
        }
    }
}
