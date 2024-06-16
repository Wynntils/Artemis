/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.hades.type;

import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;

public enum PlayerRelation {
    FRIEND(CommonColors.GREEN),
    GUILD(CommonColors.LIGHT_BLUE),
    PARTY(CommonColors.YELLOW),
    OTHER(CommonColors.WHITE);

    private final CustomColor relationColor;

    PlayerRelation(CustomColor relationColor) {
        this.relationColor = relationColor;
    }

    public CustomColor getRelationColor() {
        return relationColor;
    }
}
