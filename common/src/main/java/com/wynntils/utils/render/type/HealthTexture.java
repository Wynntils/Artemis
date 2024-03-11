/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render.type;

public enum HealthTexture implements BarTexture {
    WYNN(0, 17, 8),
    GRUNE(84, 99, 7),
    AETHER(100, 115, 7),
    SKULL(116, 131, 8),
    SKYRIM(132, 147, 8),
    RUNE(148, 163, 8),
    A(18, 33, 7),
    B(34, 51, 8),
    C(52, 67, 7),
    D(68, 83, 7);
    private final int textureY1, textureY2, height;

    HealthTexture(int textureY1, int textureY2, int height) {
        this.textureY1 = textureY1;
        this.textureY2 = textureY2;
        this.height = height;
    }

    public int getTextureY1() {
        return textureY1;
    }

    public int getTextureY2() {
        return textureY2;
    }

    public int getHeight() {
        return height;
    }
}
