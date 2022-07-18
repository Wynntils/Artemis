/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.objects.bvh;

import com.wynntils.utils.objects.IBoundingBox;
import com.wynntils.utils.objects.IBoundingBox.AxisAlignedBoundingBox;
import com.wynntils.utils.objects.bvh.ISplitStrategy.ISplitStrategyFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.phys.Vec3;

public class BinTreeStrategy implements ISplitStrategy, ISplitStrategyFactory {

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public ISplitStrategy getStrategy(final Map<String, String> settings) {
        return this;
    }

    @Override
    public <T extends IBoundingBox> List<Set<T>> split(final Set<T> elements) {
        final AxisAlignedBoundingBox bounds = elements.parallelStream()
                .map(AxisAlignedBoundingBox::fromIAABB)
                .reduce(new AxisAlignedBoundingBox(), AxisAlignedBoundingBox::mergeBounds);
        final Vec3 span = bounds.size();
        final List<Set<T>> result = Arrays.asList(new HashSet<>(), new HashSet<>());
        if (span.x > span.y && span.x > span.z) {
            final double mid = bounds.mid().x;
            elements.forEach(e -> result.get(e.mid().x < mid ? 0 : 1).add(e));
        } else if (span.y > span.z) {
            final double mid = bounds.mid().y;
            elements.forEach(e -> result.get(e.mid().y < mid ? 0 : 1).add(e));
        } else {
            final double mid = bounds.mid().z;
            elements.forEach(e -> result.get(e.mid().z < mid ? 0 : 1).add(e));
        }
        return result;
    }

    @Override
    public int bucketCount() {
        return 2;
    }
}
