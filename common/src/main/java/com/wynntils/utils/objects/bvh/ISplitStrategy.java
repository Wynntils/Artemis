/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.objects.bvh;

import com.wynntils.utils.objects.IBoundingBox;
import com.wynntils.utils.objects.bvh.NSplitStrategy.NSplitFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ISplitStrategy {
    /**
     * Bucket-sort the given elements according to the strategy.
     *
     * @param <T>
     *            super-type of {@link IBoundingBox} to allow spatial sorting.
     * @param set
     *            to be sorted.
     * @return A list containing the buckets as lists.
     */
    <T extends IBoundingBox> List<Set<T>> split(Set<T> set);

    /**
     * Number of buckets the strategy sorts to.
     *
     * @return the number of buckets.
     */
    int bucketCount();

    public interface ISplitStrategyFactory {
        /**
         * Info on available settings to configure the strategies properties.
         *
         * @return A list of the settings types and descriptions.
         */
        List<Setting> getSettings();

        /**
         * Creates and returns a split-strategy with given settings.
         *
         * @param settings
         *            for the new split-strategy.
         * @return the new split-strategy.
         */
        ISplitStrategy getStrategy(Map<String, String> settings);

        public class Setting {
            public String name;
            public String type;
            public String description;

            public Setting(final String name, final String type, final String description) {
                this.name = name;
                this.type = type;
                this.description = description;
            }
        }
    }

    public enum StrategyFactory {
        OCT_TREE(new OctTreeStrategy()),
        BIN_TREE(new BinTreeStrategy()),
        N_SPLIT(new NSplitFactory());

        private final ISplitStrategyFactory factory;

        StrategyFactory(final ISplitStrategyFactory factory) {
            this.factory = factory;
        }

        public ISplitStrategy run(final Map<String, String> settings) {
            return this.factory.getStrategy(settings != null ? settings : Collections.emptyMap());
        }

        public static StrategyFactory fromString(final String name) {
            return Arrays.stream(values())
                    .filter(v -> v.toString().equals(name))
                    .findAny()
                    .orElse(null);
        }
    }
}
