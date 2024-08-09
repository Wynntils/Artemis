/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.ui;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.models.containers.containers.SeaskipperContainer;
import com.wynntils.screens.maps.CustomSeaskipperScreen;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.PointerType;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UI)
public class CustomSeaskipperScreenFeature extends Feature {
    @Persisted
    public final Config<PointerType> pointerType = new Config<>(PointerType.ARROW);

    @Persisted
    public final Config<CustomColor> pointerColor = new Config<>(new CustomColor(1f, 1f, 1f, 1f));

    @SubscribeEvent
    public void onScreenOpen(ScreenOpenedEvent.Post event) {
        if (!(Models.Container.getCurrentContainer() instanceof SeaskipperContainer)) return;
        if (McUtils.player().isShiftKeyDown()) return;

        if (!Models.Seaskipper.isProfileLoaded()) {
            McUtils.sendErrorToClient(I18n.get("feature.wynntils.customSeaskipperScreen.error.noProfile"));
            return;
        }

        McUtils.mc().setScreen(CustomSeaskipperScreen.create());
    }
}
