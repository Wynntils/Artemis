/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.objects.bvh;

import com.wynntils.utils.objects.IBoundingBox;
import com.wynntils.utils.objects.IBoundingBox.AxisAlignedBoundingBox;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NSplitStrategy implements ISplitStrategy {
    private static final String SPLITS_SETTING = "splits";
    private final int splits;

    public NSplitStrategy(final int splits) {
        this.splits = splits;
    }

    @Override
    public <T extends IBoundingBox> List<Set<T>> split(final Set<T> elements) {
        /* split n times along widest ordinate span */
        final AxisAlignedBoundingBox bounds = elements.parallelStream()
                .map(AxisAlignedBoundingBox::fromIAABB)
                .reduce(new AxisAlignedBoundingBox(), AxisAlignedBoundingBox::mergeBounds);
        // create buckets for density analysis
        final int elementCount = elements.size();
        final int[] buckets = new int[(int) Math.round(Math.sqrt(elementCount))];
        // create functions for axis ordinates & bucket sorting
        double bucketWidth;
        Consumer<T> countToBucket;
        Function<T, Double> getOrdinate;
        if (bounds.size().x > bounds.size().y && bounds.size().x > bounds.size().z) {
            bucketWidth = bounds.size().x / buckets.length;
            countToBucket = e -> buckets[(int) ((e.mid().x - bounds.getMinX()) / bucketWidth)]++;
            getOrdinate = e -> e.mid().x;
        } else if (bounds.size().y > bounds.size().z) {
            bucketWidth = bounds.size().y / buckets.length;
            countToBucket = e -> buckets[(int) ((e.mid().y - bounds.getMinY()) / bucketWidth)]++;
            getOrdinate = e -> e.mid().y;
        } else {
            bucketWidth = bounds.size().z / buckets.length;
            countToBucket = e -> buckets[(int) ((e.mid().z - bounds.getMinZ()) / bucketWidth)]++;
            getOrdinate = e -> e.mid().z;
        }
        elements.forEach(countToBucket);
        // use density to set cuts
        final double[] cuts = new double[this.splits];
        int bucketSum = 0;
        int bucketCounter = -1;
        for (int i = 0; i < cuts.length; i++) {
            final int sectionCount = (i + 1) * elementCount / this.splits; // elements less than i-th cut
            while (bucketSum < sectionCount) {
                bucketSum += buckets[++bucketCounter];
            }
            final int prevBucketSum = bucketSum - buckets[bucketCounter];
            // lerp cut position
            cuts[i] = bucketWidth * (sectionCount - prevBucketSum) / (bucketSum - prevBucketSum)
                    + bucketWidth * bucketCounter;
        }
        // then split in n parts
        final List<Set<T>> childNodeSplits =
                IntStream.range(0, this.splits).mapToObj(i -> new HashSet<T>()).collect(Collectors.toList());
        elements.forEach(e -> {
            final double position = getOrdinate.apply(e);
            for (int i = 0; i < cuts.length; i++) {
                if (cuts[i] > position) {
                    childNodeSplits.get(i).add(e);
                    return;
                }
            }
            // the last cut is equal to bounds max, so the loop will always match
        });
        return childNodeSplits;
    }

    @Override
    public int bucketCount() {
        return this.splits;
    }

    public static class NSplitFactory implements ISplitStrategyFactory {
        @Override
        public List<Setting> getSettings() {
            return Arrays.asList(
                    new Setting(
                            SPLITS_SETTING,
                            "int",
                            "Number of buckets to sort to. Elements are distributed about equally.\n"
                                    + "Minimum and default value is 2. Missing or invalid (less than 2) values default to that."));
        }

        @Override
        public ISplitStrategy getStrategy(final Map<String, String> settings) {
            final String splitSetting = settings.get("splits");
            int splits = 2;
            if (splitSetting != null && splitSetting.matches("[0-9]+")) {
                splits = Math.max(Integer.parseInt(splitSetting), splits);
            }
            return new NSplitStrategy(splits);
        }
    }
}
