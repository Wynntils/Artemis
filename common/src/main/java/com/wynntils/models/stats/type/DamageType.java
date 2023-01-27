/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.stats.type;

import com.wynntils.models.concepts.Element;
import java.util.Optional;
import net.minecraft.ChatFormatting;

public enum DamageType {
    ALL(""),
    FIRE(Element.FIRE),
    WATER(Element.WATER),
    AIR(Element.AIR),
    THUNDER(Element.THUNDER),
    EARTH(Element.EARTH),
    RAINBOW("Elemental"),
    NEUTRAL("Neutral", "✣", ChatFormatting.GOLD); // NEUTRAL is only used once, for "Violet-Shift".

    private final Element element;
    private final String displayName;
    private final String apiName;
    private final String symbol;
    private final ChatFormatting colorCode;

    DamageType(String name) {
        this.element = null;
        // displayName needs padding if non-empty
        this.displayName = name.isEmpty() ? "" : name + " ";
        this.apiName = name;
        this.symbol = "";
        this.colorCode = null;
    }

    DamageType(String name, String symbol, ChatFormatting colorCode) {
        this.element = null;
        // displayName needs padding if non-empty
        this.displayName = name.isEmpty() ? "" : name + " ";
        this.apiName = name;

        this.symbol = symbol;
        this.colorCode = colorCode;
    }

    DamageType(Element element) {
        this.element = element;
        // displayName needs padding
        this.displayName = element.getDisplayName() + " ";
        this.apiName = element.getDisplayName();
        this.symbol = element.getSymbol();
        this.colorCode = element.getColorCode();
    }

    public static DamageType fromElement(Element element) {
        for (DamageType type : values()) {
            if (type.element == element) return type;
        }
        return null;
    }

    public Optional<Element> getElement() {
        return Optional.ofNullable(element);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getApiName() {
        return apiName;
    }

    public String getSymbol() {
        return symbol;
    }

    public ChatFormatting getColorCode() {
        return colorCode;
    }
}
