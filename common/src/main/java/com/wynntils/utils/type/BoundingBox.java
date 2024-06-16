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
}
