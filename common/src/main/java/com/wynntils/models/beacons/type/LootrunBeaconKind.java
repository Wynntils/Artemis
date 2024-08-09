/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.beacons.type;

import com.wynntils.core.WynntilsMod;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomModelData;

public enum LootrunBeaconKind {
    GREEN(79, CustomColor.fromInt(0x00FF80), CommonColors.GREEN),
    YELLOW(79, CustomColor.fromInt(0xFFFF33), CommonColors.YELLOW),
    BLUE(79, CustomColor.fromInt(0x5C5CE6), CommonColors.BLUE),
    PURPLE(79, CustomColor.fromInt(0xFF00FF), CommonColors.PURPLE),
    GRAY(79, CustomColor.fromInt(0xBFBFBF), CommonColors.LIGHT_GRAY),
    ORANGE(79, CustomColor.fromInt(0xFF9500), CommonColors.ORANGE),
    RED(79, CustomColor.fromInt(0xFF0000), CommonColors.RED),
    DARK_GRAY(79, CustomColor.fromInt(0x808080), CommonColors.GRAY),
    WHITE(79, CommonColors.WHITE, CommonColors.WHITE),
    AQUA(79, CustomColor.fromInt(0x55FFFF), CommonColors.AQUA),
    RAINBOW(80, CommonColors.WHITE, CommonColors.RAINBOW);

    private static final int COLOR_CUSTOM_MODEL_DATA = 79;

    // These values are used to identify the beacon kind
    private final int customModelData;
    private final CustomColor customColor;

    // This value is used to render the beacon kind in Wynntils
    private final CustomColor displayColor;

    LootrunBeaconKind(int customModelData, CustomColor customColor, CustomColor displayColor) {
        this.customModelData = customModelData;
        this.customColor = customColor;
        this.displayColor = displayColor;
    }

    public static LootrunBeaconKind fromItemStack(ItemStack itemStack) {
        if (itemStack.getItem() != Items.POTION) return null;

        // Extract custom color from potion
        PotionContents potionContents = itemStack.get(DataComponents.POTION_CONTENTS);
        if (potionContents == null) return null;

        // Extract custom model data from potion
        CustomModelData customModelData = itemStack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (customModelData == null) return null;

        int customModel = customModelData.value();

        // Extract custom color from potion
        // If there is no custom color, assume it's white
        int customColor = potionContents.customColor().orElse(CommonColors.WHITE.asInt());

        // Find the corresponding beacon kind
        for (LootrunBeaconKind color : values()) {
            if (color.customModelData == customModel && color.customColor.equals(CustomColor.fromInt(customColor))) {
                return color;
            }
        }

        // Log the color if it's likely to be a new beacon kind
        if (customModel == COLOR_CUSTOM_MODEL_DATA) {
            WynntilsMod.warn("Unknown beacon kind: " + customModel + " " + customColor);
        }

        return null;
    }

    public static LootrunBeaconKind fromName(String name) {
        for (LootrunBeaconKind color : values()) {
            if (color.name().equalsIgnoreCase(name)) {
                return color;
            }
        }

        return null;
    }

    public CustomColor getDisplayColor() {
        return displayColor;
    }
}
