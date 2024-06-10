/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.type;

public record BoundingBox(float x1, float z1, float x2, float z2) implements BoundingShape {
    public static final BoundingBox EMPTY = new BoundingBox(0, 0, 0, 0);

    public static BoundingBox centered(float centerX, float centerZ, float widthX, float widthZ) {
        return new BoundingBox(
                centerX - widthX / 2f, centerZ - widthZ / 2f, centerX + widthX / 2f, centerZ + widthZ / 2f);
    }

    public BoundingBox(float x1, float z1, float x2, float z2) {
        this.x1 = x1;
        this.z1 = z1;
        this.x2 = x2;
        this.z2 = z2;

        assert x1 <= x2 && z1 <= z2;
    }

    @Override
    public boolean contains(float x, float z) {
        return x1 <= x && x <= x2 && z1 <= z && z <= z2;
    }

    @Override
    public boolean intersects(BoundingBox boundingBox) {
        // Check if the bounding boxes intersect in the x and z dimensions
        boolean xIntersects = Math.max(this.x1(), boundingBox.x1()) < Math.min(this.x2(), boundingBox.x2());
        boolean zIntersects = Math.max(this.z1(), boundingBox.z1()) < Math.min(this.z2(), boundingBox.z2());

        // If the bounding boxes intersect in both dimensions, they intersect
        return xIntersects && zIntersects;
    }

    @Override
    public boolean intersects(BoundingCircle boundingCircle) {
        // Nearest point on the bounding box to the center of the circle
        float nearestX = Math.max(this.x1(), Math.min(this.x2(), boundingCircle.x()));
        float nearestZ = Math.max(this.z1(), Math.min(this.z2(), boundingCircle.z()));

        // Find the distance between the nearest point and the center of the circle
        float deltaX = boundingCircle.x() - nearestX;
        float deltaZ = boundingCircle.z() - nearestZ;

        // If the distance is less than the radius, the circle intersects the bounding box
        return (deltaX * deltaX + deltaZ * deltaZ) < (boundingCircle.radius() * boundingCircle.radius());
    }

    @Override
    public boolean intersects(BoundingPolygon boundingPolygon) {
        return BoundingShape.intersects(this, boundingPolygon);
    }
}
