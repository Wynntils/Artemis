/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.ui;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UI)
public class BulkBuyFeature extends Feature {
    @Persisted
    public final Config<Integer> bulkBuyAmount = new Config<>(4);

    // Test in BulkBuyFeature_PRICE_PATTERN
    private static final Pattern PRICE_PATTERN = Pattern.compile("§6 - §(?:c✖|a✔) §f(\\d+)§7²");
    private static final ChatFormatting BULK_BUY_ACTIVE_COLOR = ChatFormatting.GREEN;
    private static final StyledText PRICE_STR = StyledText.fromString("§6Price:");

    @SubscribeEvent
    public void onSlotClicked(ContainerClickEvent e) {
        if (!KeyboardUtils.isShiftDown()) return;

        AbstractContainerMenu container = e.getContainerMenu();

        if (!isBulkBuyable(container, e.getItemStack())) return;

        if (e.getClickType() == ClickType.QUICK_MOVE) { // Shift + Left Click
            for (int i = 1; i < bulkBuyAmount.get(); i++) {
                ContainerUtils.clickOnSlot(e.getSlotNum(), container.containerId, 10, container.getItems());
            }
        }
    }

    // This needs to be low so it runs after weapon tooltips are generated (for weapon merchants)
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltipPre(ItemTooltipRenderEvent.Pre event) {
        if (!isBulkBuyable(McUtils.containerMenu(), event.getItemStack())) return;

        List<Component> tooltips = List.of(
                Component.literal(""), // Empty line
                Component.translatable("feature.wynntils.bulkBuy.bulkBuyNormal", bulkBuyAmount.get())
                        .withStyle(BULK_BUY_ACTIVE_COLOR),
                Component.translatable("feature.wynntils.bulkBuy.bulkBuyActive", bulkBuyAmount.get())
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

        List<Component> returnable = new ArrayList<>(oldLore);

        // iterate through lore to find the price, then replace it with the bulk buy price
        // there is no better way to do this since we cannot tell which line is the price (user may or may not have nbt
        // lines enabled/disabled)
        for (Component line : oldLore) {
            StyledText oldLine = StyledText.fromComponent(line);
            Matcher priceMatcher = oldLine.getMatcher(PRICE_PATTERN);
            if (!priceMatcher.find()) continue;

            int newPrice = Integer.parseInt(priceMatcher.group(1)) * bulkBuyAmount.get();
            StyledText newLine = StyledText.fromString(oldLine.getString())
                    .replaceFirst(priceMatcher.group(1), BULK_BUY_ACTIVE_COLOR + Integer.toString(newPrice));
            if (newPrice > Models.Emerald.getAmountInInventory()) {
                newLine = StyledText.fromString(
                        newLine.getString().replace("a✔", "c✖")); // Replace green checkmark with red x
            }
            returnable.set(returnable.indexOf(line), newLine.getComponent());
            break;
        }
        if (returnable == oldLore) {
            WynntilsMod.warn("Could not find price for " + oldLore.getFirst().getString());
        }
        return returnable;
    }

    private boolean isBulkBuyable(AbstractContainerMenu menu, ItemStack toBuy) {
        String title = menu.getSlot(4).getItem().getHoverName().getString();

        return title.startsWith(ChatFormatting.GREEN.toString())
                && title.endsWith(" Shop")
                && LoreUtils.getStringLore(toBuy).contains(PRICE_STR);
    }
}
