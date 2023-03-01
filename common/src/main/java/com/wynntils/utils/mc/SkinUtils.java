/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.mc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class SkinUtils {
    public static void setPlayerHeadSkin(ItemStack stack, String textureString) {
        // If this starts being done repeatedly for the same texture string, we should cache
        // the UUID.
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), null);
        gameProfile.getProperties().put("textures", new Property("textures", textureString, ""));

        CompoundTag compoundTag = stack.getOrCreateTag();
        compoundTag.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), gameProfile));
    }

    public static ResourceLocation getSkin(UUID uuid) {
        ClientPacketListener connection = McUtils.mc().getConnection();

        if (connection == null) {
            return DefaultPlayerSkin.getDefaultSkin(uuid);
        }
        PlayerInfo playerInfo = connection.getPlayerInfo(uuid);
        if (playerInfo == null) {
            return DefaultPlayerSkin.getDefaultSkin(uuid);
        }

        return playerInfo.getSkinLocation();
    }
}
