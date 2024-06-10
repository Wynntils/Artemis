/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.type;

public record BoundingCircle(float x, float z, float radius) implements BoundingShape {
    public static BoundingCircle enclosingCircle(BoundingBox box) {
        return new BoundingCircle(
                (box.x1() + box.x2()) / 2f,
                (box.z1() + box.z2()) / 2f,
                (float) Math.sqrt(Math.pow(box.x2() - box.x1(), 2) + Math.pow(box.z2() - box.z1(), 2)) / 2f);
    }

    @Override
    public boolean contains(float x, float z) {
        return Math.pow(x - this.x, 2) + Math.pow(z - this.z, 2) <= Math.pow(radius, 2);
    }

    @Override
    public boolean intersects(BoundingBox boundingBox) {
        return BoundingShape.intersects(boundingBox, this);
    }

    @Override
    public boolean intersects(BoundingCircle boundingCircle) {
        // Find the distance between the centers of the circles
        float deltaX = boundingCircle.x() - this.x();
        float deltaZ = boundingCircle.z() - this.z();

        // If the distance is less than the sum of the radii, the circles intersect
        return (deltaX * deltaX + deltaZ * deltaZ)
                < ((this.radius() + boundingCircle.radius()) * (this.radius() + boundingCircle.radius()));
    }

    @Override
    public boolean intersects(BoundingPolygon boundingPolygon) {
        return BoundingShape.intersects(this, boundingPolygon);
    }
}
