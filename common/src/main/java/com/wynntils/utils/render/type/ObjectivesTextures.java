/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render.type;

public enum ObjectivesTextures implements BarTexture {
    WYNN(0, 9, 4),
    LIQUID(40, 49, 4),
    EMERALD(50, 59, 4),
    A(10, 19, 4),
    B(20, 29, 4),
    C(30, 39, 4);
    private final int textureY1, textureY2, height;

    ObjectivesTextures(int textureY1, int textureY2, int height) {
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
