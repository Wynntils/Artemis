/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.item;

import com.wynntils.core.managers.Model;
import com.wynntils.wynn.item.IntelligenceSkillPointsItemStack;
import com.wynntils.wynn.item.parsers.WynnItemMatchers;
import com.wynntils.wynn.model.item.ItemStackTransformManager.ItemStackTransformer;

public class IntelligenceSkillPointsItemStackModel extends Model {
    private static final ItemStackTransformer INTELLIGENCESKILLPOINTS_TRANSFORMER = new ItemStackTransformer(
            WynnItemMatchers::isIntelligenceSkillPoints, IntelligenceSkillPointsItemStack::new);

    public static void init() {
        ItemStackTransformManager.registerTransformer(INTELLIGENCESKILLPOINTS_TRANSFORMER);
    }

    public static void disable() {
        ItemStackTransformManager.unregisterTransformer(INTELLIGENCESKILLPOINTS_TRANSFORMER);
    }
}
