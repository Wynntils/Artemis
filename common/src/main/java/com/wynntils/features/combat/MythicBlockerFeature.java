/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.ContainerCloseEvent;
import com.wynntils.models.containers.containers.reward.RewardContainer;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.items.game.EmeraldPouchItem;
import com.wynntils.models.items.items.gui.IngredientPouchItem;
import com.wynntils.models.items.properties.GearTierItemProperty;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.COMBAT)
public class MythicBlockerFeature extends Feature {
    // Note: At the time of adding this, opening the emerald/ingredient pouch while a reward container is open
    //       will cause the container to exit (without the pouch opening, in the ingredient pouch case).
    //       This is a bug in the game.
    @Persisted
    private final Config<Boolean> preventPouchClick = new Config<>(true);

    @SubscribeEvent
    public void onChestCloseAttempt(ContainerCloseEvent.Pre e) {
        if (!Models.WorldState.onWorld()) return;
        if (!(Models.Container.getCurrentContainer() instanceof RewardContainer)) return;

        NonNullList<ItemStack> items = ContainerUtils.getItems(McUtils.mc().screen);
        for (int i = 0; i < Models.LootChest.LOOT_CHEST_ITEM_COUNT; i++) {
            ItemStack itemStack = items.get(i);
            Optional<GearTierItemProperty> tieredItem =
                    Models.Item.asWynnItemProperty(itemStack, GearTierItemProperty.class);
            if (tieredItem.isPresent() && tieredItem.get().getGearTier() == GearTier.MYTHIC) {
                McUtils.sendMessageToClient(Component.translatable("feature.wynntils.mythicBlocker.closingBlocked")
                        .withStyle(ChatFormatting.RED));
                e.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onIngredientPouchClick(ContainerClickEvent event) {
        if (!Models.WorldState.onWorld()) return;
        if (!preventPouchClick.get()) return;
        if (!(Models.Container.getCurrentContainer() instanceof RewardContainer)) return;

        Optional<IngredientPouchItem> ingredientPouchItem =
                Models.Item.asWynnItem(event.getItemStack(), IngredientPouchItem.class);
        Optional<EmeraldPouchItem> emeraldPouchItem =
                Models.Item.asWynnItem(event.getItemStack(), EmeraldPouchItem.class);
        if (ingredientPouchItem.isEmpty() && emeraldPouchItem.isEmpty()) return;

        // Don't want to block emerald pouches in reward chests
        if (emeraldPouchItem.isPresent() && event.getSlotNum() < Models.LootChest.LOOT_CHEST_ITEM_COUNT) {
            return;
        }

        McUtils.sendMessageToClient(Component.translatable("feature.wynntils.mythicBlocker.pouchBlocked")
                .withStyle(ChatFormatting.RED));
        event.setCanceled(true);
    }
}
