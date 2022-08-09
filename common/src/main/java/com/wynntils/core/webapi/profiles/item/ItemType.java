/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.webapi.profiles.item;

import com.wynntils.wc.objects.ClassType;
import java.util.Locale;

public enum ItemType {
    SPEAR(ClassType.Warrior, 16 * 1, 16 * 1),
    WAND(ClassType.Mage, 16 * 0, 16 * 1),
    DAGGER(ClassType.Assassin, 16 * 2, 16 * 1),
    BOW(ClassType.Archer, 16 * 3, 16 * 1),
    RELIK(ClassType.Shaman, 16 * 0, 16 * 2),
    RING(null, 16 * 1, 16 * 2),
    BRACELET(null, 16 * 2, 16 * 2),
    NECKLACE(null, 16 * 3, 16 * 2),
    HELMET(null, 16 * 0, 16 * 0),
    CHESTPLATE(null, 16 * 1, 16 * 0),
    LEGGINGS(null, 16 * 2, 16 * 0),
    BOOTS(null, 16 * 3, 16 * 0);

    private final ClassType classReq;
    private final int iconTextureX;
    private final int iconTextureY;

    ItemType(ClassType classReq, int iconTextureX, int iconTextureY) {
        this.classReq = classReq;
        this.iconTextureX = iconTextureX;
        this.iconTextureY = iconTextureY;
    }

    public ClassType getClassReq() {
        return classReq;
    }

    public int getIconTextureX() {
        return iconTextureX;
    }

    public int getIconTextureY() {
        return iconTextureY;
    }

    public static ItemType fromString(String typeStr) {
        return switch (typeStr.toLowerCase(Locale.ROOT)) {
            case "spear" -> SPEAR;
            case "wand" -> WAND;
            case "dagger" -> DAGGER;
            case "bow" -> BOW;
            case "relik"-> RELIK;
            case "ring" -> RING;
            case "necklace" -> NECKLACE;
            case "bracelet" -> BRACELET;
            case "helmet" -> HELMET;
            case "chestplate" -> CHESTPLATE;
            case "leggings" -> LEGGINGS;
            case "boots" -> BOOTS;
            default -> null;
        };
    }
}
