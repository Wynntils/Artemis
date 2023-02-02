/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gear.type;

import com.wynntils.utils.StringUtils;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum GearTier {
    NORMAL(ChatFormatting.WHITE, -1, 0),
    UNIQUE(ChatFormatting.YELLOW, 3, 0.5f),
    RARE(ChatFormatting.LIGHT_PURPLE, 8, 1.2f),
    SET(ChatFormatting.GREEN, 8, 1.2f),
    FABLED(ChatFormatting.RED, 12, 4.5f),
    LEGENDARY(ChatFormatting.AQUA, 16, 8.0f),
    MYTHIC(ChatFormatting.DARK_PURPLE, 90, 18.0f),
    CRAFTED(ChatFormatting.DARK_AQUA, -1, 0);

    private final ChatFormatting chatFormatting;
    private final int baseCost;
    private final float costMultiplier;

    GearTier(ChatFormatting chatFormatting, int baseCost, float costMultiplier) {
        this.chatFormatting = chatFormatting;
        this.baseCost = baseCost;
        this.costMultiplier = costMultiplier;
    }

    public static GearTier fromString(String typeStr) {
        try {
            return GearTier.valueOf(typeStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static GearTier fromFormattedString(String name) {
        if (name.charAt(0) == '§') {
            return fromChatFormatting(ChatFormatting.getByCode(name.charAt(1)));
        }

        return null;
    }

    public static GearTier fromComponent(Component component) {
        String name = component.getString();

        if (name.charAt(0) == '§') {
            return fromChatFormatting(ChatFormatting.getByCode(name.charAt(1)));
        }

        return null;
    }

    public static GearTier fromChatFormatting(ChatFormatting formatting) {
        return Arrays.stream(GearTier.values())
                .filter(t -> t.getChatFormatting() == formatting)
                .findFirst()
                .orElse(null);
    }

    public static GearTier fromBoxDamage(int damage) {
        if (damage > 6) return NORMAL;
        return GearTier.values()[damage];
    }

    public ChatFormatting getChatFormatting() {
        return chatFormatting;
    }

    public int getGearIdentificationCost(int level) {
        return this.baseCost + (int) Math.ceil(level * this.costMultiplier);
    }

    public String getName() {
        return StringUtils.capitalizeFirst(name().toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return getName();
    }
}
