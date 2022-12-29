/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.model.item.game;

public class AmplifierItem extends GameItem implements NumberedTierItemProperty {
    private final int tier;

    public AmplifierItem(int tier) {
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }
}
