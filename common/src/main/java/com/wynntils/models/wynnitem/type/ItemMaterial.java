/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.wynnitem.type;

import com.wynntils.core.components.Models;
import com.wynntils.models.gear.type.GearType;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.SkinUtils;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.ItemIdFix;
import net.minecraft.util.datafix.fixes.ItemStackTheFlatteningFix;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.Unbreakable;

public record ItemMaterial(ItemStack itemStack) {
    public static ItemMaterial getDefaultTomeItemMaterial() {
        ItemStack itemStack = createItemStack(Items.ENCHANTED_BOOK, 0);
        return new ItemMaterial(itemStack);
    }

    public static ItemMaterial getDefaultCharmItemMaterial() {
        // All charms are different items, this is as good as any other item
        ItemStack itemStack = createItemStack(Items.CLAY, 0);
        return new ItemMaterial(itemStack);
    }

    public static ItemMaterial fromArmorType(String materialType, GearType gearType, CustomColor color) {
        String itemId = (materialType.equals("chain") ? "chainmail" : materialType) + "_"
                + gearType.name().toLowerCase(Locale.ROOT);

        ItemStack itemStack = createItemStack(getItem("minecraft:" + itemId), 0);
        if (color != null) {
            // color is only set in case of leather
            itemStack.set(DataComponents.DYED_COLOR, new DyedItemColor(color.asInt(), false));
        }

        return new ItemMaterial(itemStack);
    }

    public static ItemMaterial fromPlayerHeadUUID(String uuid) {
        ItemStack itemStack = createItemStack(Items.PLAYER_HEAD, 0);
        SkinUtils.setPlayerHeadFromUUID(itemStack, uuid);

        return new ItemMaterial(itemStack);
    }

    public static ItemMaterial fromGearType(GearType gearType) {
        // Material is missing, so just give generic icon for this type of gear (weapon or accessory)
        ItemStack itemStack = createItemStack(gearType.getDefaultItem(), gearType.getDefaultModel());

        return new ItemMaterial(itemStack);
    }

    public static ItemMaterial fromItemId(String itemId, int damageCode) {
        ItemStack itemStack = createItemStack(getItem(itemId), damageCode);

        return new ItemMaterial(itemStack);
    }

    public static ItemMaterial fromItemTypeCode(int itemTypeCode, int damageCode) {
        String itemId;

        Optional<String> materialNameOverrideOpt = Models.WynnItem.getMaterialName(itemTypeCode, damageCode);
        if (materialNameOverrideOpt.isPresent()) {
            // The vanilla lookup fails for a handful of items, so we have a correctional data set
            itemId = "minecraft:" + materialNameOverrideOpt.get();
        } else {
            // Use normal vanilla lookup
            String toIdString = ItemIdFix.getItem(itemTypeCode);
            String alternativeName = ItemStackTheFlatteningFix.updateItem(toIdString, damageCode);
            itemId = alternativeName != null ? alternativeName : toIdString;
        }

        return fromItemId(itemId, damageCode);
    }

    private static ItemStack createItemStack(Item item, int modelValue) {
        ItemStack itemStack = new ItemStack(item);

        itemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelValue));
        itemStack.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        return itemStack;
    }

    private static Item getItem(String itemId) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
    }
}
