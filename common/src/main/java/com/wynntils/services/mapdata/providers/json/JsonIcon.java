/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.json;

import com.mojang.blaze3d.platform.NativeImage;
import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.utils.mc.McUtils;
import java.io.IOException;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public class JsonIcon implements MapIcon {
    private final String iconId;
    private final NativeImage nativeImage;
    private final int width;
    private final int height;
    private boolean registered;
    private ResourceLocation resource;

    public JsonIcon(String iconId, byte[] texture) throws IOException {
        this.iconId = iconId;
        this.nativeImage = NativeImage.read(texture);
        this.width = nativeImage.getWidth();
        this.height = nativeImage.getHeight();
        this.resource = ResourceLocation.fromNamespaceAndPath("wynntils", "icons/" + iconId.replace(":", "."));
    }

    @Override
    public ResourceLocation getResourceLocation() {
        if (!registered) {
            // We canot do this in the constructor since GL is not initiated at that time
            registered = true;
            McUtils.mc().getTextureManager().register(resource, new DynamicTexture(nativeImage));
        }

        return resource;
    }

    @Override
    public String getIconId() {
        return iconId;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
