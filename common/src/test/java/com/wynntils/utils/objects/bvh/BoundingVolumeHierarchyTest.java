/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.objects.bvh;

import com.wynntils.utils.objects.IBoundingBox;
import com.wynntils.utils.objects.IBoundingBox.AxisAlignedBoundingBox;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests all functionality of the BVH.
 *
 * @author Kepler-17c
 */
class BoundingVolumeHierarchyTest {
    /**
     * Adds and removes a large number of elements in quick succession.
     */
    @Test
    void addRemoveBulkTest() {
        final BoundingVolumeHierarchy<IBoundingBox> bvh = new BoundingVolumeHierarchy<>();
        final Random random = new Random("Wynntils".hashCode()); // fixed seed for reproducible test
        final Supplier<Vec3> nextPoint = () -> new Vec3(random.nextDouble(), random.nextDouble(), random.nextDouble());
        final int cycles = 1 << 10;
        final int inserts = 1 << 10;
        IBoundingBox nearestTest;
        Vec3 reference;
        Vec3 offset = new Vec3(1, 0, 0);
        for (int c = 0; c < cycles; c++) {
            nearestTest = null;
            reference = nextPoint.get().add(offset);
            bvh.clear();
            // prepare bvh and reference element for findNearest()
            final List<IBoundingBox> removals = new ArrayList<>(inserts);
            for (int i = 0; i < inserts; i++) {
                final IBoundingBox bb = new AxisAlignedBoundingBox(nextPoint.get(), nextPoint.get());
                bvh.add(bb);
                if (nearestTest == null || bb.squareDistance(reference) < nearestTest.squareDistance(reference)) {
                    nearestTest = bb;
                }
                if (random.nextBoolean()) {
                    removals.add(bb);
                }
            }
            // test bvh against reference element
            IBoundingBox bvhNearest = bvh.findNearest(reference);
            Assertions.assertEquals(
                    nearestTest,
                    bvhNearest,
                    "Cycle: " + c + ", expected dist: " + nearestTest.squareDistance(reference) + ", actual dist: "
                            + bvhNearest.squareDistance(reference));
            // remove marked elements
            for (final IBoundingBox bb : removals) {
                if (bb == nearestTest) {
                    nearestTest = null;
                }
                bvh.remove(bb);
            }
            // test again after deletions
            if (nearestTest != null) {
                bvhNearest = bvh.findNearest(reference);
                Assertions.assertEquals(
                        nearestTest,
                        bvhNearest,
                        "Cycle: " + c + ", expected dist: " + nearestTest.squareDistance(reference) + ", actual dist: "
                                + bvhNearest.squareDistance(reference));
            }
        }
    }

    /**
     * Adds a bunch of points and checks that findNearest returns the correct ones.
     */
    @Test
    void addTest() {
        final BoundingVolumeHierarchy<IBoundingBox> bvh = new BoundingVolumeHierarchy<>();
        final int range = 1 << 7;
        final IBoundingBox[][] expected = new IBoundingBox[range][range];
        for (int y = 0; y < range; y++) {
            for (int x = 0; x < range; x++) {
                final IBoundingBox bb = new AxisAlignedBoundingBox(new Vec3(x, y, 0));
                expected[y][x] = bb;
            }
            bvh.addAll(Arrays.asList(expected[y]));
            System.out.println("added " + (y * range));
        }
        for (int y = 0; y < range; y++) {
            for (int x = 0; x < range; x++) {
                Assertions.assertEquals(expected[y][x], bvh.findNearest(new Vec3(x, y, 0)));
            }
            System.out.println("checked " + (y * range));
        }
    }

    /**
     * Adds elements while checking the size.
     */
    @Test
    void sizeTest() {
        final BoundingVolumeHierarchy<IBoundingBox> bvh = new BoundingVolumeHierarchy<>();
        final int totalLimit = 1 << 12;
        final int batchLimit = 1 << 8;
        final Random random = new Random("Ragni".hashCode());
        for (int size = 0; size < totalLimit; ) {
            final int batch = random.nextInt(batchLimit);
            for (int b = 0; b < batch; b++, size++) {
                bvh.add(generatePointBB(random));
                try {
                    Thread.sleep(10);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Assertions.assertEquals(size, bvh.size());
        }
    }

    /**
     * Creates a new random point element implementing IBoundingBox from a given generator.
     *
     * @param random
     *            RNG source.
     * @return the random point.
     */
    private static IBoundingBox generatePointBB(final Random random) {
        return new AxisAlignedBoundingBox(new Vec3(random.nextDouble(), random.nextDouble(), random.nextDouble()));
    }
}
