/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.items.game;

import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.properties.GearTierItemProperty;
import com.wynntils.models.items.properties.NumberedTierItemProperty;

public class AmplifierItem extends GameItem implements NumberedTierItemProperty, GearTierItemProperty {
    private final int tier;

    public AmplifierItem(int tier) {
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    public GearTier getGearTier() {
        return GearTier.LEGENDARY;
    }

    @Override
    public String toString() {
        return "AmplifierItem{" + "tier=" + tier + '}';
    }
}
