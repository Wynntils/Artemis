/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.guides.emeraldpouch;

import com.wynntils.core.components.Services;
import com.wynntils.models.emeralds.type.EmeraldUnits;
import com.wynntils.models.items.items.game.EmeraldPouchItem;
import com.wynntils.screens.guides.GuideItemStack;
import com.wynntils.utils.MathUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Unbreakable;

public final class GuideEmeraldPouchItemStack extends GuideItemStack {
    private final int tier;

    private final List<Component> generatedTooltip;

    public GuideEmeraldPouchItemStack(int tier) {
        super(new ItemStack(Items.DIAMOND_AXE), new EmeraldPouchItem(tier, 0), "Emerald Pouch");
        this.setDamageValue(97);

        this.tier = tier;

        this.set(DataComponents.UNBREAKABLE, new Unbreakable(false));

        generatedTooltip = generatePouchLore(tier);
    }

    private static List<Component> generatePouchLore(int tier) {
        int upTo = (tier - 1) / 3;
        int rows = 0;
        String totalString = "";

        switch (tier % 3) {
            case 0 -> {
                rows = 6;
                totalString = "54";
            }
            case 1 -> {
                rows = 1;
                totalString = "9";
            }
            case 2 -> {
                rows = 3;
                totalString = "27";
            }
        }

        if (tier >= 10) {
            rows = 6;
        }

        if (tier >= 7) {
            totalString = tier - 6 + "stx";
        } else if (tier >= 4) {
            totalString += EmeraldUnits.LIQUID_EMERALD.getSymbol();
        } else {
            totalString += EmeraldUnits.EMERALD_BLOCK.getSymbol();
        }

        List<Component> itemLore = new ArrayList<>();

        itemLore.add(Component.empty());
        itemLore.add(Component.literal("Emerald Pouches allows the wearer to easily ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("store ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("and ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("convert ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("picked emeralds without spending extra inventory slots.")
                        .withStyle(ChatFormatting.GRAY)));
        itemLore.add(Component.empty());
        itemLore.add(Component.literal(" - " + rows + " Rows ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("(" + totalString + " Total)"))
                .withStyle(ChatFormatting.DARK_GRAY));

        switch (upTo) {
            case 0 -> itemLore.add(Component.literal("No Auto-Conversions").withStyle(ChatFormatting.GRAY));
            case 1 -> itemLore.add(Component.literal("Converts up to")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(" Emerald Blocks").withStyle(ChatFormatting.WHITE)));
            default -> itemLore.add(Component.literal("Converts up to")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(" Liquid Emeralds").withStyle(ChatFormatting.WHITE)));
        }

        return itemLore;
    }

    @Override
    public Component getHoverName() {
        return Component.literal("Emerald Pouch ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal("[Tier " + MathUtils.toRoman(tier) + "]")
                        .withStyle(ChatFormatting.DARK_GREEN));
    }

    @Override
    public List<Component> getTooltipLines(Item.TooltipContext context, Player player, TooltipFlag flag) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(getHoverName());
        tooltip.addAll(generatedTooltip);

        tooltip.add(Component.empty());
        if (Services.Favorites.isFavorite(this)) {
            tooltip.add(Component.translatable("screens.wynntils.wynntilsGuides.itemGuide.unfavorite")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("screens.wynntils.wynntilsGuides.itemGuide.favorite")
                    .withStyle(ChatFormatting.GREEN));
        }

        return tooltip;
    }

    public int getTier() {
        return tier;
    }
}
