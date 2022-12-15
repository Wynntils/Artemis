/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.item.properties;

import com.wynntils.core.managers.Managers;
import com.wynntils.core.managers.Model;
import com.wynntils.wynn.item.parsers.WynnItemMatchers;
import com.wynntils.wynn.item.properties.ConsumableChargeProperty;
import com.wynntils.wynn.model.item.ItemStackTransformManager.ItemPropertyWriter;

public final class ConsumableChargePropertyModel extends Model {
    private static final ItemPropertyWriter CONSUMABLE_CHARGE_WRITER =
            new ItemPropertyWriter(WynnItemMatchers::isConsumable, ConsumableChargeProperty::new);

    @Override
    public void init() {
        Managers.ItemStackTransform.registerProperty(CONSUMABLE_CHARGE_WRITER);
    }

    @Override
    public void disable() {
        Managers.ItemStackTransform.unregisterProperty(CONSUMABLE_CHARGE_WRITER);
    }
}
