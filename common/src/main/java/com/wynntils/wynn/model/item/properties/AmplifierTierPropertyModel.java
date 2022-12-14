/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.item.properties;

import com.wynntils.core.managers.Managers;
import com.wynntils.core.managers.Model;
import com.wynntils.wynn.item.parsers.WynnItemMatchers;
import com.wynntils.wynn.item.properties.AmplifierTierProperty;
import com.wynntils.wynn.model.item.ItemStackTransformManager.ItemPropertyWriter;

public class AmplifierTierPropertyModel extends Model {
    private static final ItemPropertyWriter AMPLIFIER_TIER_WRITER =
            new ItemPropertyWriter(WynnItemMatchers::isAmplifier, AmplifierTierProperty::new);

    public static void init() {
        Managers.ITEM_STACK_TRANSFORM.registerProperty(AMPLIFIER_TIER_WRITER);
    }

    public static void disable() {
        Managers.ITEM_STACK_TRANSFORM.unregisterProperty(AMPLIFIER_TIER_WRITER);
    }
}
