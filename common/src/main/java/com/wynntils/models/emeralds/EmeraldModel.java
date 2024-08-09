/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.emeralds;

import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.mc.event.ContainerCloseEvent;
import com.wynntils.mc.event.MenuEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.emeralds.type.EmeraldUnits;
import com.wynntils.models.items.items.game.EmeraldPouchItem;
import com.wynntils.models.items.properties.EmeraldValuedItemProperty;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.mc.McUtils;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

public final class EmeraldModel extends Model {
    public static final int EMERALD_BLOCK_VALUE = 64;
    public static final int LIQUID_EMERALD_VALUE = 4096;
    public static final int LIQUID_EMERALD_STACK_VALUE = 262144;

    private static final Pattern STX_PATTERN = Pattern.compile("(\\.?\\d+\\.?\\d*)\\s*(s|stx|stacks)");
    private static final Pattern LE_PATTERN = Pattern.compile("(\\.?\\d+\\.?\\d*)\\s*(l|le)");
    private static final Pattern EB_PATTERN = Pattern.compile("(\\.?\\d+\\.?\\d*)\\s*(b|eb)");
    private static final Pattern K_PATTERN = Pattern.compile("(\\.?\\d+\\.?\\d*)\\s*(k|thousand)");
    private static final Pattern M_PATTERN = Pattern.compile("(\\.?\\d+\\.?\\d*)\\s*(m|million)");
    private static final Pattern E_PATTERN = Pattern.compile("(\\d+)($|\\s|\\s*e|\\s*em)(?![^\\d\\s-])");
    private static final Pattern RAW_PRICE_PATTERN = Pattern.compile("\\d+");
    private static final double SILVERBULL_TAX_AMOUNT = 1.03;
    private static final double NORMAL_TAX_AMOUNT = 1.05;

    private int inventoryEmeralds = 0;
    private int containerEmeralds = 0;

    public EmeraldModel() {
        super(List.of());
    }

    @SubscribeEvent
    public void onWorldChange(WorldStateEvent e) {
        if (e.getNewState() != WorldState.WORLD) return;

        inventoryEmeralds = 0;
        containerEmeralds = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        recountEmeralds();
    }

    private void recountEmeralds() {
        inventoryEmeralds = 0;

        // Rescan inventory after merging items
        List<ItemStack> items = McUtils.inventoryMenu().getItems();
        for (ItemStack item : items) {
            adjustBalance(item, true);
        }

        containerEmeralds = 0;

        // Rescan container after merging items
        items = McUtils.containerMenu().getItems();
        for (ItemStack item : items) {
            adjustBalance(item, false);
        }
    }

    @SubscribeEvent
    public void onMenuClosed(MenuEvent.MenuClosedEvent e) {
        containerEmeralds = 0;
    }

    @SubscribeEvent
    public void onContainerClose(ContainerCloseEvent.Post event) {
        containerEmeralds = 0;
    }

    private void adjustBalance(ItemStack newItemStack, boolean isInventory) {
        int adjustValue = 0;
        Optional<EmeraldValuedItemProperty> newItemValueOpt =
                Models.Item.asWynnItemProperty(newItemStack, EmeraldValuedItemProperty.class);
        if (newItemValueOpt.isPresent()) {
            adjustValue += newItemValueOpt.get().getEmeraldValue();
        }

        if (isInventory) {
            inventoryEmeralds += adjustValue;
        } else {
            containerEmeralds += adjustValue;
        }
    }

    public String getFormattedString(int emeralds, boolean appendZeros) {
        StringBuilder builder = new StringBuilder();

        int[] emeraldAmounts = emeraldsPerUnit(emeralds);

        for (int i = emeraldAmounts.length - 1; i >= 0; i--) {
            if (emeraldAmounts[i] == 0 && !appendZeros) continue;

            builder.append(emeraldAmounts[i])
                    .append(EmeraldUnits.values()[i].getSymbol())
                    .append(" ");
        }

        return builder.toString().trim();
    }

    public int[] emeraldsPerUnit(int emeralds) {
        return new int[] {
            emeralds % EMERALD_BLOCK_VALUE,
            (emeralds / EMERALD_BLOCK_VALUE) % 64,
            (emeralds / LIQUID_EMERALD_VALUE) % 64,
            emeralds / LIQUID_EMERALD_STACK_VALUE
        };
    }

    public boolean isEmeraldPouch(ItemStack itemStack) {
        Optional<EmeraldPouchItem> itemOpt = Models.Item.asWynnItem(itemStack, EmeraldPouchItem.class);
        return itemOpt.isPresent();
    }

    public int getAmountInInventory() {
        return inventoryEmeralds;
    }

    public int getAmountInContainer() {
        return containerEmeralds - inventoryEmeralds;
    }

    public String convertEmeraldPrice(String inputStr) {
        Matcher rawMatcher = RAW_PRICE_PATTERN.matcher(inputStr);
        if (rawMatcher.matches()) return "";

        String input = inputStr.toLowerCase(Locale.ROOT);
        long emeralds = 0;

        try {
            // stx
            Matcher stxMatcher = STX_PATTERN.matcher(input);
            while (stxMatcher.find()) {
                emeralds += (long) (Double.parseDouble(stxMatcher.group(1)) * LIQUID_EMERALD_STACK_VALUE);
            }

            // le
            Matcher leMatcher = LE_PATTERN.matcher(input);
            while (leMatcher.find()) {
                emeralds += (long) (Double.parseDouble(leMatcher.group(1)) * LIQUID_EMERALD_VALUE);
            }

            // eb
            Matcher ebMatcher = EB_PATTERN.matcher(input);
            while (ebMatcher.find()) {
                emeralds += (long) (Double.parseDouble(ebMatcher.group(1)) * EMERALD_BLOCK_VALUE);
            }
            // k
            Matcher kMatcher = K_PATTERN.matcher(input);
            while (kMatcher.find()) {
                emeralds += (long) (Double.parseDouble(kMatcher.group(1)) * 1000);
            }

            // m
            Matcher mMatcher = M_PATTERN.matcher(input);
            while (mMatcher.find()) {
                emeralds += (long) (Double.parseDouble(mMatcher.group(1)) * 1000000);
            }

            // standard numbers/emeralds
            Matcher eMatcher = E_PATTERN.matcher(input);
            while (eMatcher.find()) {
                emeralds += Long.parseLong(eMatcher.group(1));
            }

            // account for tax if flagged
            if (input.contains("-t")) {
                emeralds = Math.round(emeralds / getTaxAmount());
            }
        } catch (NumberFormatException e) {
            return "";
        }

        return (emeralds > 0) ? String.valueOf(emeralds) : "";
    }

    public double getTaxAmount() {
        return Models.Character.isSilverbullSubscriber() ? SILVERBULL_TAX_AMOUNT : NORMAL_TAX_AMOUNT;
    }
}
