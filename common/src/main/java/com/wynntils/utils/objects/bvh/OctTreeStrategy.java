/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.objects.bvh;

import com.wynntils.utils.objects.IBoundingBox;
import com.wynntils.utils.objects.bvh.ISplitStrategy.ISplitStrategyFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.world.phys.Vec3;

public class OctTreeStrategy implements ISplitStrategy, ISplitStrategyFactory {
    @Override
    public <T extends IBoundingBox> List<Set<T>> split(final Set<T> elements) {
        Vec3 mid = new Vec3(0, 0, 0);
        for (T e : elements) {
            mid = mid.add(e.mid());
        }
        // find weighted mid as split point
        mid = mid.scale(1. / elements.size());
        // decide octant by xyz 'digits' (less than mid = 1 / else = 0)
        // e.g. x less, y,z greater -> 100 -> bin 4 -> oct[4]
        final List<Set<T>> octantList =
                IntStream.range(0, 8).mapToObj(i -> new HashSet<T>()).collect(Collectors.toList());
        Vec3 finalMid = mid;
        elements.forEach(e -> {
            int index = 0;
            final Vec3 bbMid = e.mid();
            if (bbMid.x < finalMid.x) {
                index |= (1 << 0);
            }
            if (bbMid.y < finalMid.y) {
                index |= (1 << 1);
            }
            if (bbMid.z < finalMid.z) {
                index |= (1 << 2);
            }
            octantList.get(index).add(e);
        });
        return octantList;
    }

    @Override
    public int bucketCount() {
        return 8;
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public ISplitStrategy getStrategy(final Map<String, String> settings) {
        return this;
    }
}
