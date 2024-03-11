/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render.type;

public enum ManaTexture implements BarTexture {
    WYNN(0, 17, 8),
    BRUNE(83, 100, 8),
    AETHER(116, 131, 7),
    SKULL(132, 147, 8),
    INVERSE(100, 115, 7),
    SKYRIM(148, 163, 8),
    RUNE(164, 179, 8),
    A(18, 33, 7),
    B(34, 51, 8),
    C(52, 67, 7),
    D(83, 100, 8);
    private final int textureY1, textureY2, height;

    ManaTexture(int textureY1, int textureY2, int height) {
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
