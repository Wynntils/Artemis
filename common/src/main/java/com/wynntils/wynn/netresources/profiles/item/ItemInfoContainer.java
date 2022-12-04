/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.netresources.profiles.item;

import com.google.gson.annotations.SerializedName;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemInfoContainer {
    private static final Pattern COLOR_PATTERN = Pattern.compile("(\\d{1,3}),(\\d{1,3}),(\\d{1,3})");

    private final ItemType type;
    private final String set;
    private final ItemDropType dropType;
    private final String armorColor;

    @SerializedName("name")
    private final String materialName;

    @SerializedName("damage")
    private final String metadata;

    public ItemInfoContainer(
            ItemType type, String set, ItemDropType dropType, String armorColor, String materialName, String metadata) {
        this.type = type;
        this.set = set;
        this.dropType = dropType;
        this.armorColor = armorColor;
        this.materialName = materialName;
        this.metadata = metadata;
    }

    public ItemDropType getDropType() {
        return dropType;
    }

    public ItemType getType() {
        return type;
    }

    public String getArmorColor() {
        return armorColor;
    }

    public String getSet() {
        return set;
    }

    public boolean isArmorColorValid() {
        return armorColor != null && COLOR_PATTERN.matcher(armorColor).find();
    }

    public int getArmorColorAsInt() {
        if (armorColor == null) return 0;

        Matcher m = COLOR_PATTERN.matcher(getArmorColor());
        if (!m.find()) return 0;

        int r = Integer.parseInt(m.group(1));
        int g = Integer.parseInt(m.group(2));
        int b = Integer.parseInt(m.group(3));

        return (r << 16) + (g << 8) + b;
    }

    public ItemStack asItemStack() {
        if (materialName == null) {
            ItemStack stack = new ItemStack(type.getDefaultItem());
            stack.setDamageValue(type.getDefaultDamage());

            return stack;
        }

        Optional<Item> item = Registry.ITEM.getOptional(new ResourceLocation(materialName));
        ItemStack itemStack = item.map(ItemStack::new).orElseGet(() -> new ItemStack(Items.AIR));

        if (metadata != null) {
            itemStack.setDamageValue(Integer.parseInt(metadata));
        }

        return itemStack;
    }
}
