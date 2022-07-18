/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.objects;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BinaryOperator;
import net.minecraft.world.phys.Vec3;

/**
 * General bounding box interface.
 *
 * @author Kepler-17c
 */
public interface IBoundingBox {
    /**
     * Combines
     *
     * @param other
     * @return
     */
    default IBoundingBox mergeWith(final IBoundingBox other) {
        return AxisAlignedBoundingBox.mergeBounds(this, other);
    }

    default Vec3 getLower() {
        return this.getBounds().getLower();
    }

    default Vec3 getUpper() {
        return this.getBounds().getUpper();
    }

    default Vec3 size() {
        return this.getBounds().size();
    }

    default Vec3 mid() {
        return this.getBounds().mid();
    }

    default double squareDistance(final Vec3 point) {
        return AxisAlignedBoundingBox.squareDistance(AxisAlignedBoundingBox.fromIAABB(this), point);
    }

    default boolean contains(final IBoundingBox other) {
        return this.getBounds().contains(other);
    }

    default double volume() {
        return this.getBounds().volume();
    }

    default Pair<Vec3, Vec3> definingPoints() {
        return new Pair<>(this.getLower(), this.getUpper());
    }

    AxisAlignedBoundingBox getBounds();

    public class AxisAlignedBoundingBox implements IBoundingBox {
        public static final Vec3 NEUTRAL_LOWER = doubleToVec3(Double.POSITIVE_INFINITY);
        public static final Vec3 NEUTRAL_UPPER = doubleToVec3(Double.NEGATIVE_INFINITY);
        public static final double EPSILON = 1.0 / (1L << 48);
        public static final double ONE_PLUS_EPS = 1.0 + EPSILON;
        public static final double ONE_MINUS_EPS = 1.0 - EPSILON;

        private Vec3 lower;
        private Vec3 upper;
        private Vec3 size;
        private Vec3 mid;

        public AxisAlignedBoundingBox() {
            this.lower = NEUTRAL_LOWER;
            this.upper = NEUTRAL_UPPER;
            this.size = doubleToVec3(0);
            this.mid = doubleToVec3(0);
        }

        public AxisAlignedBoundingBox(final Vec3... points) {
            final Pair<Vec3, Vec3> definingPoints = Arrays.stream(points)
                    .collect(
                            () -> new Pair<>(NEUTRAL_LOWER, NEUTRAL_UPPER),
                            (acc, val) -> {
                                acc.a = min(acc.a, val);
                                acc.b = max(acc.b, val);
                            },
                            (acc, val) -> {
                                acc.a = min(acc.a, val.a);
                                acc.b = max(acc.b, val.b);
                            });
            this.lower = definingPoints.a;
            this.lower.scale(ONE_MINUS_EPS);
            this.upper = definingPoints.b;
            this.upper.scale(ONE_PLUS_EPS);
            this.updateSizeAndMid();
        }

        public AxisAlignedBoundingBox(final Collection<IBoundingBox> points) {
            final Pair<Vec3, Vec3> definingPoints = points.stream()
                    .map(IBoundingBox::definingPoints)
                    .collect(
                            () -> new Pair<>(NEUTRAL_LOWER, NEUTRAL_UPPER),
                            (acc, val) -> {
                                acc.a = min(acc.a, val.a);
                                acc.b = min(acc.b, val.b);
                            },
                            (acc, val) -> {
                                acc.a = min(acc.a, val.a);
                                acc.b = min(acc.b, val.b);
                            });
            this.lower = definingPoints.a;
            this.lower.scale(ONE_MINUS_EPS);
            this.upper = definingPoints.b;
            this.upper.scale(ONE_PLUS_EPS);
            this.updateSizeAndMid();
        }

        private static Vec3 doubleToVec3(final double d) {
            return new Vec3(d, d, d);
        }

        public static AxisAlignedBoundingBox fromIAABB(final IBoundingBox iaabb) {
            return new AxisAlignedBoundingBox(iaabb.getLower(), iaabb.getUpper());
        }

        private static Vec3 min(final Vec3 a, final Vec3 b) {
            return new Vec3(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
        }

        private static Vec3 max(final Vec3 a, final Vec3 b) {
            return new Vec3(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
        }

        public static AxisAlignedBoundingBox mergeBounds(final IBoundingBox a, final IBoundingBox b) {
            return new AxisAlignedBoundingBox(
                    mergePoints(a.getLower(), b.getLower(), Math::min),
                    mergePoints(a.getUpper(), b.getUpper(), Math::max));
        }

        private void updateSizeAndMid() {
            size = this.upper.subtract(lower);
            mid = size.scale(0.5).add(lower);
        }

        public void add(final Vec3 point) {
            final Vec3 marginLower = point.scale(ONE_MINUS_EPS);
            final Vec3 marginUpper = point.scale(ONE_PLUS_EPS);
            this.lower = min(this.lower, marginLower);
            this.upper = max(this.upper, marginUpper);
            this.updateSizeAndMid();
        }

        @Override
        public IBoundingBox mergeWith(final IBoundingBox boundingBox) {
            return mergeBounds(this, boundingBox);
        }

        @Override
        public Vec3 getLower() {
            return this.lower;
        }

        @Override
        public Vec3 getUpper() {
            return this.upper;
        }

        public double getMinX() {
            return this.lower.x;
        }

        public double getMinY() {
            return this.lower.y;
        }

        public double getMinZ() {
            return this.lower.z;
        }

        public double getMaxX() {
            return this.upper.x;
        }

        public double getMaxY() {
            return this.upper.y;
        }

        public double getMaxZ() {
            return this.upper.z;
        }

        @Override
        public Vec3 size() {
            return this.size;
        }

        @Override
        public Vec3 mid() {
            return this.mid;
        }

        @Override
        public double squareDistance(final Vec3 point) {
            return squareDistance(this, point);
        }

        @Override
        public boolean contains(final IBoundingBox other) {
            final Vec3 otherLower = other.getLower();
            final Vec3 otherUpper = other.getUpper();
            return this.lower.x <= otherLower.x
                    && this.lower.y <= otherLower.y
                    && this.lower.z <= otherLower.z
                    && otherUpper.x <= this.upper.x
                    && otherUpper.y <= this.upper.y
                    && otherUpper.z <= this.upper.z;
        }

        @Override
        public double volume() {
            return this.size.x * this.size.y * this.size.z;
        }

        @Override
        public AxisAlignedBoundingBox getBounds() {
            return this;
        }

        @Override
        public String toString() {
            return "[" + this.lower + ", " + this.upper + "]";
        }

        private static Vec3 mergePoints(final Vec3 a, final Vec3 b, final BinaryOperator<Double> mergeFuntion) {
            return new Vec3(mergeFuntion.apply(a.x, b.x), mergeFuntion.apply(a.y, b.y), mergeFuntion.apply(a.z, b.z));
        }

        private static double squareDistance(final AxisAlignedBoundingBox bb, final Vec3 point) {
            // view along coords as 1D intervals
            // separate calculation works because of super-position
            final double xDist =
                    point.x < bb.lower.x ? bb.lower.x - point.x : point.x > bb.upper.x ? point.x - bb.upper.x : 0;
            final double yDist =
                    point.y < bb.lower.y ? bb.lower.y - point.y : point.y > bb.upper.y ? point.y - bb.upper.y : 0;
            final double zDist =
                    point.z < bb.lower.z ? bb.lower.z - point.z : point.z > bb.upper.z ? point.z - bb.upper.z : 0;
            // combine 1D linear distances to 3D squared distance
            return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
        }
    }
}
