/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.cosmetics;

import com.mojang.blaze3d.platform.NativeImage;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Service;
import com.wynntils.models.players.WynntilsUser;
import com.wynntils.models.players.type.CosmeticInfo;
import com.wynntils.services.cosmetics.type.WynntilsCapeLayer;
import com.wynntils.services.cosmetics.type.WynntilsElytraLayer;
import com.wynntils.services.cosmetics.type.WynntilsLayer;
import com.wynntils.utils.mc.McUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;

public class CosmeticsService extends Service {
    private static final BiFunction<
                    RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>,
                    EntityModelSet,
                    WynntilsLayer>
            CAPE_LAYER = (playerRenderer, playerModel) -> new WynntilsCapeLayer(playerRenderer);
    private static final BiFunction<
                    RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>,
                    EntityModelSet,
                    WynntilsLayer>
            ELYTRA_LAYER = (playerRenderer, playerModel) -> new WynntilsElytraLayer(playerRenderer, playerModel);

    private static final List<
                    BiFunction<
                            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>,
                            EntityModelSet,
                            WynntilsLayer>>
            REGISTERED_LAYERS = List.of(CAPE_LAYER, ELYTRA_LAYER);

    private final Map<UUID, ResourceLocation[]> cosmeticTextures = new ConcurrentHashMap<>();

    public CosmeticsService() {
        super(List.of());
    }

    // Note: This method is static deliberately.
    // We don't want to reference Models in the mixin this is called, due to it's unpredictable class loading.
    public static List<
                    BiFunction<
                            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>,
                            EntityModelSet,
                            WynntilsLayer>>
            getRegisteredLayers() {
        return REGISTERED_LAYERS;
    }

    public boolean shouldRenderCape(Player player, boolean elytra) {
        if (player.isInvisible() || !player.isModelPartShown(PlayerModelPart.CAPE)) return false;

        if (Models.Player.getUser(player.getUUID()) == null || getUserCosmeticTexture(player.getUUID()) == null)
            return false;

        CosmeticInfo cosmetics = Models.Player.getUser(player.getUUID()).cosmetics();
        return (elytra ? cosmetics.hasElytra() : cosmetics.hasCape());
    }

    // TODO: implement ear rendering
    public boolean shouldRenderEars(AbstractClientPlayer player) {
        if (player.isInvisible()) return false;

        if (Models.Player.getUser(player.getUUID()) == null) return false;

        return Models.Player.getUser(player.getUUID()).cosmetics().hasEars();
    }

    public ResourceLocation getCapeTexture(Player player) {
        ResourceLocation[] textures = getUserCosmeticTexture(player.getUUID());
        if (textures == null) return null;

        int frames = textures.length;
        if (frames == 1) return textures[0];

        // This makes animated capes cycle through all their frames in 2 seconds,
        // regardless of how many frames of animation are present. Could be changed to an account setting?
        double percentage = ((System.currentTimeMillis() % 2000) / 2000d);
        int currentFrame = (int) (frames * percentage);
        return textures[currentFrame];
    }

    public void loadCosmeticTextures(UUID uuid, WynntilsUser user) {
        try {
            if (user.cosmetics().texture() == null || user.cosmetics().texture().isEmpty()) return;
            if (cosmeticTextures.containsKey(uuid)) return;

            byte[] textureBytes = Base64.getDecoder().decode(user.cosmetics().texture());
            ByteArrayInputStream byteStream = new ByteArrayInputStream(textureBytes);
            NativeImage image = NativeImage.read(byteStream);

            int frames = (image.getHeight() * 2) / image.getWidth();
            int frameHeight = image.getHeight() / frames;

            ResourceLocation[] locations = new ResourceLocation[frames];
            String baseLocation = "wynntils:capes/" + uuid.toString().replace("-", "");

            if (frames == 1) { // not animated
                locations[0] = ResourceLocation.parse(baseLocation);
                McUtils.mc().getTextureManager().register(locations[0], new DynamicTexture(image));
            } else { // animated
                for (int i = 0; i < frames; i++) {
                    NativeImage frame = new NativeImage(frameHeight * 2, frameHeight, false);
                    image.copyRect(frame, 0, frameHeight * i, 0, 0, frameHeight * 2, frameHeight, false, false);

                    locations[i] = ResourceLocation.parse(baseLocation + "/" + i);
                    McUtils.mc().getTextureManager().register(locations[i], new DynamicTexture(frame));
                }
            }

            cosmeticTextures.put(uuid, locations);
        } catch (IOException e) {
            WynntilsMod.warn("IOException occurred while loading cosmetics for user " + uuid, e);
        }
    }

    public ResourceLocation[] getUserCosmeticTexture(UUID uuid) {
        return cosmeticTextures.getOrDefault(uuid, null);
    }
}
