/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.custom.item;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.webapi.WebManager;
import com.wynntils.core.webapi.profiles.ItemGuessProfile;
import com.wynntils.core.webapi.profiles.item.ItemProfile;
import com.wynntils.core.webapi.profiles.item.ItemTier;
import com.wynntils.core.webapi.profiles.item.ItemType;
import com.wynntils.features.user.tooltips.ItemGuessFeature;
import com.wynntils.utils.reference.EmeraldSymbols;
import com.wynntils.wc.utils.WynnUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class UnidentifiedItemStack extends WynnItemStack {

    private ItemGuessProfile guessProfile;
    private ItemTier tier;

    private List<Component> tooltip;

    public UnidentifiedItemStack(ItemStack stack) {
        super(stack);

        tooltip = getOriginalTooltip();

        tier = ItemTier.fromComponent(getHoverName());
        if (tier == null) return;

        String itemType = itemName.split(" ", 2)[1];
        if (itemType == null) return;

        String levelRange = null;
        for (Component lineComp : tooltip) {
            String line = WynnUtils.normalizeBadString(lineComp.getString());
            if (line.contains("Lv. Range")) {
                levelRange = line.replace("- Lv. Range: ", "");
                break;
            }
        }

        if (levelRange == null) return;
        if (WebManager.getItemGuesses() == null || WebManager.getItemsMap() == null) return;

        guessProfile = WebManager.getItemGuesses().get(levelRange);
        if (guessProfile == null) return;

        Map<ItemTier, List<String>> rarityMap;
        try {
            rarityMap = guessProfile.getItems().get(ItemType.valueOf(itemType.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) { // itemType is invalid
            WynntilsMod.warn(String.format("ItemType was invalid for itemType: %s", itemType));
            return;
        }
        if (rarityMap == null) return;

        List<String> items = rarityMap.get(tier);
        if (items == null) return;

        MutableComponent guesses = new TextComponent("");
        for (String item : items) {
            MutableComponent itemDesc = new TextComponent(item).withStyle(tier.getChatFormatting());

            ItemProfile itemProfile = WebManager.getItemsMap().get(item);
            if (ItemGuessFeature.showGuessesPrice && itemProfile != null) {
                int level = itemProfile.getLevelRequirement();
                int itemCost = tier.getItemIdentificationCost(level);
                itemDesc.append(new TextComponent(" [")
                        .append(new TextComponent(itemCost + " " + EmeraldSymbols.E_STRING)
                                .withStyle(ChatFormatting.GREEN))
                        .append(new TextComponent("]"))
                        .withStyle(ChatFormatting.GRAY));
            }

            if (guesses.getSiblings().size() > 0)
                guesses.append(new TextComponent(", ").withStyle(ChatFormatting.GRAY));

            guesses.append(itemDesc);
        }

        tooltip.add(new TranslatableComponent("feature.wynntils.itemGuess.possibilities", guesses));
    }

    @Override
    public List<Component> getTooltipLines(Player player, TooltipFlag flag) {
        return tooltip;
    }
}
