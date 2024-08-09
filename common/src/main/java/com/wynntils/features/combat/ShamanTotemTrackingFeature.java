/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.extension.EntityExtension;
import com.wynntils.models.abilities.event.TotemEvent;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.COMBAT)
public class ShamanTotemTrackingFeature extends Feature {
    private static final int ENTITY_GLOWING_FLAG = 6;

    @Persisted
    public final Config<Boolean> highlightShamanTotems = new Config<>(true);

    @Persisted
    public final Config<CustomColor> firstTotemColor = new Config<>(CommonColors.WHITE);

    @Persisted
    public final Config<CustomColor> secondTotemColor = new Config<>(CommonColors.BLUE);

    @Persisted
    public final Config<CustomColor> thirdTotemColor = new Config<>(CommonColors.RED);

    @Persisted
    public final Config<CustomColor> fourthTotemColor = new Config<>(CommonColors.GREEN);

    @SubscribeEvent
    public void onTotemSummoned(TotemEvent.Summoned e) {
        if (!highlightShamanTotems.get()) return;

        int totemNumber = e.getTotemNumber();
        ArmorStand totemAS = e.getTotemEntity();

        CustomColor color =
                switch (totemNumber) {
                    case 1 -> firstTotemColor.get();
                    case 2 -> secondTotemColor.get();
                    case 3 -> thirdTotemColor.get();
                    case 4 -> fourthTotemColor.get();
                    default -> throw new IllegalArgumentException(
                            "totemNumber should be 1, 2, 3 or 4! (color switch in #onTotemSummoned in ShamanTotemTrackingFeature");
                };

        ((EntityExtension) totemAS).setGlowColor(color);

        totemAS.setGlowingTag(true);
        totemAS.setSharedFlag(ENTITY_GLOWING_FLAG, true);
    }
}
