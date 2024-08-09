/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.mc;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;

public final class SkinUtils {
    public static void setPlayerHeadFromUUID(ItemStack itemStack, String uuid) {
        JsonObject skinObject = new JsonObject();
        skinObject.addProperty("url", "https://textures.minecraft.net/texture/" + uuid);

        JsonObject texturesObject = new JsonObject();
        texturesObject.add("SKIN", skinObject);

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("textures", texturesObject);

        // Encode the jsonObject into a base64 string.
        String textureString =
                Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(Charset.defaultCharset()));

        setPlayerHeadSkin(itemStack, textureString);
    }

    public static void setPlayerHeadSkin(ItemStack itemStack, String textureString) {
        // If this starts being done repeatedly for the same texture string,
        // we should cache the UUID.
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), "");
        gameProfile.getProperties().put("textures", new Property("textures", textureString, null));

        itemStack.set(DataComponents.PROFILE, new ResolvableProfile(gameProfile));
    }

    public static ResourceLocation getSkin(UUID uuid) {
        ClientPacketListener connection = McUtils.mc().getConnection();

        if (connection == null) {
            return DefaultPlayerSkin.getDefaultTexture();
        }
        PlayerInfo playerInfo = connection.getPlayerInfo(uuid);
        if (playerInfo == null) {
            return DefaultPlayerSkin.getDefaultTexture();
        }

        return playerInfo.getSkin().texture();
    }
}
