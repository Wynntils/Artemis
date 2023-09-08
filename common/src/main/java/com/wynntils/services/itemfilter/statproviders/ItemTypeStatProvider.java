/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.itemfilter.statproviders;

import com.wynntils.models.items.WynnItem;
import com.wynntils.services.itemfilter.type.StringStatProvider;
import java.util.List;

public class ItemTypeStatProvider extends StringStatProvider {
    @Override
    public List<String> getValue(WynnItem wynnItem) {
        return List.of(wynnItem.getClass().getSimpleName().replace("Item", ""));
    }
}
