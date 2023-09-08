/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.itemfilter.statproviders;

import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.properties.TargetedItemProperty;
import com.wynntils.services.itemfilter.type.StringStatProvider;
import java.util.List;

public class TargetStatProperty extends StringStatProvider {
    @Override
    public List<String> getValue(WynnItem wynnItem) {
        if (!(wynnItem instanceof TargetedItemProperty targetedItemProperty)) return List.of();

        return List.of(targetedItemProperty.getTarget());
    }
}
