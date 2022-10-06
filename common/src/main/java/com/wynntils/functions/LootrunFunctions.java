/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.functions;

import com.wynntils.core.functions.Function;
import com.wynntils.features.statemanaged.LootrunFunctionsFeature;
import java.util.List;

public class LootrunFunctions {
    public static class DryStreakFunction extends Function<Integer> {

        @Override
        public Integer getValue(String argument) {
            return LootrunFunctionsFeature.INSTANCE.dryCount;
        }

        @Override
        public List<String> getAliases() {
            return List.of("dry_s");
        }
    }

    public static class DryBoxesFunction extends Function<Integer> {

        @Override
        public Integer getValue(String argument) {
            return LootrunFunctionsFeature.INSTANCE.dryBoxes;
        }

        @Override
        public List<String> getAliases() {
            return List.of("dry_b", "dry_boxes_count");
        }
    }
}
