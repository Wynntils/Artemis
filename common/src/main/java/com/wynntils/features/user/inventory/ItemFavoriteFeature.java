/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.inventory;

import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.TypeOverride;
import com.wynntils.core.features.UserFeature;
import com.wynntils.mc.event.ContainerCloseEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.WynnItemCache;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.wynn.ContainerUtils;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.INVENTORY)
public class ItemFavoriteFeature extends UserFeature {
    public static ItemFavoriteFeature INSTANCE;

    // This should really move to FavoritesModel, but for now, models cannot have configs
    @Config(visible = false)
    public Set<String> favoriteItems = new HashSet<>();

    @TypeOverride
    private final Type favoriteItemsType = new TypeToken<Set<String>>() {}.getType();

    @SubscribeEvent
    public void onChestCloseAttempt(ContainerCloseEvent.Pre e) {
        if (!Models.WorldState.onWorld()) return;
        if (!Models.Container.isLootOrRewardChest(McUtils.mc().screen)) return;

        NonNullList<ItemStack> items = ContainerUtils.getItems(McUtils.mc().screen);
        for (int i = 0; i < 27; i++) {
            ItemStack itemStack = items.get(i);

            if (isFavorited(itemStack)) {
                McUtils.sendMessageToClient(Component.translatable("feature.wynntils.itemFavorite.closingBlocked")
                        .withStyle(ChatFormatting.RED));
                e.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onRenderSlot(SlotRenderEvent.Post event) {
        ItemStack itemStack = event.getSlot().getItem();

        if (isFavorited(itemStack)) {
            renderFavoriteItem(event);
        }
    }

    private boolean isFavorited(ItemStack itemStack) {
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(itemStack);
        if (wynnItemOpt.isEmpty()) return false;

        WynnItem wynnItem = wynnItemOpt.get();
        int currentRevision = Models.Favorites.getRevision();
        Integer revision = wynnItem.getCache().get(WynnItemCache.FAVORITE_KEY);
        if (revision != null && (revision == currentRevision || revision == -currentRevision)) {
            // The cache is up to date; positive value means it is a favorite
            return revision > 0;
        }

        // Cache is missing or outdated
        boolean isFavorite = Models.Favorites.calculateFavorite(itemStack, wynnItem);
        wynnItem.getCache().store(WynnItemCache.FAVORITE_KEY, isFavorite ? currentRevision : -currentRevision);
        return isFavorite;
    }

    private static void renderFavoriteItem(SlotRenderEvent.Post event) {
        RenderUtils.drawScalingTexturedRect(
                new PoseStack(),
                Texture.FAVORITE.resource(),
                event.getSlot().x + 10,
                event.getSlot().y,
                400,
                9,
                9,
                Texture.FAVORITE.width(),
                Texture.FAVORITE.height());
    }
}
