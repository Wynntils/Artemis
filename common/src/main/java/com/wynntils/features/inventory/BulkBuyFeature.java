/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.UserFeature;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.INVENTORY)
public class BulkBuyFeature extends UserFeature {
    @Config
    public int bulkBuyAmount = 4;

    // Test suite: https://regexr.com/7998g
    private static final Pattern PRICE_PATTERN = Pattern.compile("§6 - §r§(?:c✖|a✔) §r§f(\\d+)§r§7²");
    private static final ChatFormatting BULK_BUY_ACTIVE_COLOR = ChatFormatting.GREEN;

    @SubscribeEvent
    public void onSlotClicked(ContainerClickEvent e) {
        if (!KeyboardUtils.isShiftDown()) return;

        AbstractContainerMenu container = e.getContainerMenu();

        if (!isBulkBuyable(container, e.getItemStack())) return;

        if (e.getClickType() == ClickType.QUICK_MOVE) { // Shift + Left Click
            for (int i = 1; i < bulkBuyAmount; i++) {
                ContainerUtils.clickOnSlot(e.getSlotNum(), container.containerId, 10, container.getItems());
            }
        }
    }

    // This needs to be low so it runs after weapon tooltips are generated (for weapon merchants)
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltipPre(ItemTooltipRenderEvent.Pre event) {
        if (!isBulkBuyable(McUtils.player().containerMenu, event.getItemStack())) return;

        List<Component> tooltips = List.of(
                Component.literal(""), // Empty line
                Component.translatable("feature.wynntils.bulkBuy.bulkBuyNormal", bulkBuyAmount)
                        .withStyle(BULK_BUY_ACTIVE_COLOR),
                Component.translatable("feature.wynntils.bulkBuy.bulkBuyActive", bulkBuyAmount)
                        .withStyle(BULK_BUY_ACTIVE_COLOR));

        event.setTooltips(LoreUtils.appendTooltip(event.getItemStack(), replacePrices(event.getTooltips()), tooltips));
    }

    /**
     * When shift is pressed:
     * Replaces the price in the lore with the bulk buy price.
     * Also replaces the "✔" with a "✖" with a if the user can't afford the bulk buy.
     * @param oldLore Lore of the item that user wants to bulk buy
     * @return New lore with the above replacements
     */
    private List<Component> replacePrices(List<Component> oldLore) {
        if (!KeyboardUtils.isShiftDown()) return oldLore;

        String priceLine = ComponentUtils.getCoded(oldLore.get(oldLore.size() - 1));
        Matcher priceMatcher = PRICE_PATTERN.matcher(priceLine);
        if (!priceMatcher.find()) {
            WynntilsMod.warn("Could not find price for " + oldLore.get(0).getString() + " in " + priceLine);
            return oldLore;
        }
        int newPrice = Integer.parseInt(priceMatcher.group(1)) * bulkBuyAmount;

        String newLine = priceLine.replace(priceMatcher.group(1), BULK_BUY_ACTIVE_COLOR + Integer.toString(newPrice));

        if (newPrice > Models.Emerald.getAmountInInventory()) {
            newLine = newLine.replace("a✔", "c✖"); // Replace green checkmark with red x
        }

        List<Component> newLore = new ArrayList<>(oldLore);
        newLore.set(newLore.size() - 1, Component.nullToEmpty(newLine));
        return newLore;
    }

    private boolean isBulkBuyable(AbstractContainerMenu menu, ItemStack toBuy) {
        String title = menu.getSlot(4).getItem().getHoverName().getString();

        return title.startsWith(ChatFormatting.GREEN.toString())
                && title.endsWith(" Shop")
                && LoreUtils.getStringLore(toBuy).contains("§6Price:");
    }
}
