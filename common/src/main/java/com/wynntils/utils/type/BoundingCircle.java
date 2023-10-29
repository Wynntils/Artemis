/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.type;

public class BoundingCircle implements BoundingShape {
    private final float x;
    private final float z;
    private final float radius;

    public static BoundingCircle enclosingCircle(BoundingBox box) {
        return new BoundingCircle(
                (box.x1() + box.x2()) / 2f,
                (box.z1() + box.z2()) / 2f,
                (float) Math.sqrt(Math.pow(box.x2() - box.x1(), 2) + Math.pow(box.z2() - box.z1(), 2)) / 2f);
    }

    public BoundingCircle(float x, float z, float radius) {
        this.x = x;
        this.z = z;
        this.radius = radius;
    }

    public float x() {
        return x;
    }

    public float z() {
        return z;
    }

    public float radius() {
        return radius;
    }

    @Override
    public boolean contains(float x, float z) {
        return Math.pow(x - this.x, 2) + Math.pow(z - this.z, 2) <= Math.pow(radius, 2);
    }
}
