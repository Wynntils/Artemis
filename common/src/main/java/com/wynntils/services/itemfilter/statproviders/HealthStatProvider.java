/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.itemfilter.statproviders;

import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.services.itemfilter.type.IntegerStatProvider;
import java.util.Collections;
import java.util.List;

public class HealthStatProvider extends IntegerStatProvider {
    @Override
    public List<Integer> getValue(WynnItem wynnItem) {
        if (wynnItem instanceof GearItem gearItem) {
            return List.of(gearItem.getGearInfo().fixedStats().healthBuff());
        }

        if (wynnItem instanceof CraftedGearItem craftedGearItem) {
            return Collections.singletonList(craftedGearItem.getHealth());
        }

        return List.of();
    }
}
