/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.tooltips;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.models.emeralds.type.EmeraldUnits;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.items.game.GearBoxItem;
import com.wynntils.utils.mc.LoreUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.TOOLTIPS)
public class ItemGuessFeature extends Feature {
    @Persisted
    public final Config<Boolean> showGuessesPrice = new Config<>(true);

    @SubscribeEvent
    public void onTooltipPre(ItemTooltipRenderEvent.Pre event) {
        Optional<GearBoxItem> gearBoxItemOpt = Models.Item.asWynnItem(event.getItemStack(), GearBoxItem.class);
        if (gearBoxItemOpt.isEmpty()) return;

        List<Component> tooltips = LoreUtils.appendTooltip(
                event.getItemStack(), event.getTooltips(), getTooltipAddon(gearBoxItemOpt.get()));
        event.setTooltips(tooltips);
    }

    private List<Component> getTooltipAddon(GearBoxItem gearBoxItem) {
        List<Component> addon = new ArrayList<>();
        List<GearInfo> possibleGear = Models.Gear.getPossibleGears(gearBoxItem);
        GearTier gearTier = gearBoxItem.getGearTier();

        if (possibleGear.isEmpty()) return addon; // nothing to put in tooltip

        addon.add(Component.literal("- ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.translatable("feature.wynntils.itemGuess.possibilities")
                        .withStyle(ChatFormatting.GRAY)));

        Map<Integer, List<MutableComponent>> levelToItems = new TreeMap<>();

        for (GearInfo gearInfo : possibleGear) {
            int level = (gearInfo != null) ? gearInfo.requirements().level() : -1;

            MutableComponent itemDesc = Component.literal(gearInfo.name()).withStyle(gearTier.getChatFormatting());

            if (Services.Favorites.isFavorite(gearInfo.name())) {
                itemDesc.withStyle(ChatFormatting.UNDERLINE);
            }

            levelToItems.computeIfAbsent(level, i -> new ArrayList<>()).add(itemDesc);
        }

        for (Map.Entry<Integer, List<MutableComponent>> entry : levelToItems.entrySet()) {
            int level = entry.getKey();
            List<MutableComponent> itemsForLevel = entry.getValue();

            if (itemsForLevel.isEmpty()) {
                WynntilsMod.warn("No items for level " + level + " in gear box " + gearBoxItem.getGearTier() + " "
                        + gearBoxItem.getLevelRange() + "!");
                continue;
            }

            MutableComponent guesses = Component.literal("    ");

            guesses.append(Component.literal("- ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.translatable("feature.wynntils.itemGuess.levelLine", level == -1 ? "?" : level)
                            .withStyle(ChatFormatting.GRAY)));

            if (showGuessesPrice.get() && level != -1) {
                guesses.append(Component.literal(" [")
                        .append(Component.literal((gearTier.getGearIdentificationCost(level) + " "
                                        + EmeraldUnits.EMERALD.getSymbol()))
                                .withStyle(ChatFormatting.GREEN))
                        .append(Component.literal("]"))
                        .withStyle(ChatFormatting.GRAY));
            }

            guesses.append(StyledText.fromString("§7: ").getComponent());

            MutableComponent itemsComponent = Component.empty();
            itemsComponent.append(itemsForLevel.getFirst());
            itemsForLevel.stream().skip(1).forEach(i -> itemsComponent
                    .append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
                    .append(i));

            if (!itemsForLevel.isEmpty()) {
                guesses.append(itemsComponent);

                addon.add(guesses);
            }
        }

        return addon;
    }
}
