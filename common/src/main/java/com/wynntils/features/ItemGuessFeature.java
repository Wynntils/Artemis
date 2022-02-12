/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features;

import com.google.common.collect.ImmutableList;
import com.wynntils.core.Reference;
import com.wynntils.core.features.*;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.GameplayImpact;
import com.wynntils.core.features.properties.PerformanceImpact;
import com.wynntils.core.features.properties.Stability;
import com.wynntils.core.webapi.WebManager;
import com.wynntils.core.webapi.profiles.ItemGuessProfile;
import com.wynntils.mc.event.InventoryRenderEvent;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.mc.utils.ItemUtils;
import com.wynntils.wc.objects.ItemTier;
import com.wynntils.wc.objects.ItemType;
import com.wynntils.wc.utils.WynnItemMatchers;
import com.wynntils.wc.utils.WynnUtils;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(
        performance = PerformanceImpact.SMALL,
        gameplay = GameplayImpact.LARGE,
        stability = Stability.STABLE)
public class ItemGuessFeature extends Feature {

    @Override
    public void init(
            ImmutableList.Builder<WebProviderSupplier> apis,
            ImmutableList.Builder<KeySupplier> keybinds,
            ImmutableList.Builder<Condition> conditions) {
        apis.add(WebManager::getItemGuessesProvider);
    }

    @SubscribeEvent
    public static void onInventoryRender(InventoryRenderEvent e) {
        if (!WynnUtils.onWorld()) return;

        Slot hoveredSlot = e.getHoveredSlot();
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return;

        ItemStack stack = hoveredSlot.getItem();

        if (ItemUtils.hasMarker(stack, "itemGuesses")) return;

        ItemUtils.addMarker(stack, "itemGuesses");

        if (!WynnItemMatchers.isUnidentified(stack)) return;

        generateGuesses(stack);
    }

    private static void generateGuesses(ItemStack stack) {
        String name =
                WynnUtils.normalizeBadString(
                        ChatFormatting.stripFormatting(stack.getHoverName().getString()));
        String itemType = name.split(" ", 2)[1];
        if (itemType == null) return;

        ListTag lore = ItemUtils.getLoreTagElseEmpty(stack);
        if (lore.isEmpty()) return;

        String levelRange = null;

        for (Tag tag : lore) {
            String line = ComponentUtils.getUnformatted(tag.getAsString());
            if (line != null && line.contains("Lv. Range")) {
                levelRange = line.replace("- Lv. Range: ", "");
                break;
            }
        }

        if (levelRange == null) return;

        ItemGuessProfile igp = WebManager.getItemGuessesProvider().getValue().get(levelRange);
        if (igp == null) return;

        Map<ItemTier, List<String>> rarityMap;
        try {
            rarityMap = igp.getItems().get(ItemType.valueOf(itemType));
        } catch (IllegalArgumentException exception) { // itemType is invalid
            Reference.LOGGER.warn(String.format("ItemType was invalid for itemType: %s", itemType));
            return;
        }
        if (rarityMap == null) return;

        ItemTier tier = ItemTier.fromComponent(stack.getHoverName());
        if (tier == null) return;

        List<String> items = rarityMap.get(tier);
        if (items == null) return;

        StringBuilder itemNamesAndCosts = new StringBuilder();
        for (String possibleItem : items) {
            // ItemProfile itemProfile = WebManager.getItems().get(possibleItem);

            String itemDescription = tier.getChatFormatting() + possibleItem;

            //        (UtilitiesConfig.INSTANCE.favoriteItems.contains(possibleItem) ? UNDERLINE :
            // "") + possibleItem; // underline favs
            /*
            if (UtilitiesConfig.Identifications.INSTANCE.showGuessesPrice && itemProfile != null) {
                int level = itemProfile.getRequirements().getLevel();
                int itemCost = tier.getItemIdentificationCost(level);
                itemDescription += GRAY + " [" + GREEN + itemCost + " " + EmeraldSymbols.E_STRING + GRAY + "]";
            }

             */
            if (itemNamesAndCosts.length() > 0) {
                itemNamesAndCosts.append(ChatFormatting.GRAY).append(", ");
            }

            itemNamesAndCosts.append(itemDescription);
        }

        lore.add(
                StringTag.valueOf(
                        ItemUtils.toLoreForm(
                                ChatFormatting.GREEN
                                        + "- "
                                        + ChatFormatting.GRAY
                                        + "Possibilities: "
                                        + itemNamesAndCosts)));

        ItemUtils.replaceLore(stack, lore);
    }
}
