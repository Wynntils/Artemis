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
}
