/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render.type;

import net.minecraft.resources.ResourceLocation;

public interface AbstractTexture {
    ResourceLocation resource();

    int width();

    int height();
}
