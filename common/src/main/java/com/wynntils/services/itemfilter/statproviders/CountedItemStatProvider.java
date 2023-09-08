/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.itemfilter.statproviders;

import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.properties.CountedItemProperty;
import com.wynntils.services.itemfilter.type.IntegerStatProvider;
import java.util.List;

public class CountedItemStatProvider extends IntegerStatProvider {
    @Override
    public List<Integer> getValue(WynnItem wynnItem) {
        if (!(wynnItem instanceof CountedItemProperty countedItemProperty)) return List.of();

        return List.of(countedItemProperty.getCount());
    }
}
