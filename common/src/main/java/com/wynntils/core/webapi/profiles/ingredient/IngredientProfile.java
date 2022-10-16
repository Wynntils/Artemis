/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.webapi.profiles.ingredient;

import com.google.gson.annotations.SerializedName;
import com.wynntils.core.webapi.WebManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class IngredientProfile {
    String name;

    @SerializedName("tier")
    IngredientTier ingredientTier;

    boolean untradeable;
    int level;
    String material;
    List<ProfessionType> professions;
    Map<String, IngredientIdentificationContainer> statuses;
    IngredientItemModifiers itemModifiers;
    IngredientModifiers ingredientModifiers;

    public IngredientProfile(
            String name,
            IngredientTier ingredientTier,
            boolean untradeable,
            int level,
            String material,
            List<ProfessionType> professions,
            Map<String, IngredientIdentificationContainer> statuses,
            IngredientItemModifiers itemModifiers,
            IngredientModifiers ingredientModifiers) {
        this.name = name;
        this.ingredientTier = ingredientTier;
        this.untradeable = untradeable;
        this.level = level;
        this.material = material;
        this.professions = professions;
        this.statuses = statuses;
        this.itemModifiers = itemModifiers;
        this.ingredientModifiers = ingredientModifiers;
    }

    public String getIngredientStringFormatted() {
        return ChatFormatting.GRAY + name + " " + ingredientTier.getTierString();
    }

    public String getDisplayName() {
        return name;
    }

    public List<ProfessionType> getProfessions() {
        return professions;
    }

    public int getLevel() {
        return level;
    }

    public IngredientTier getTier() {
        return ingredientTier;
    }

    public Map<String, IngredientIdentificationContainer> getStatuses() {
        return this.statuses;
    }

    public IngredientModifiers getIngredientModifiers() {
        return ingredientModifiers;
    }

    public IngredientItemModifiers getItemModifiers() {
        return itemModifiers;
    }

    public boolean isUntradeable() {
        return untradeable;
    }

    public ItemStack asItemStack() {
        if (material == null) {
            return new ItemStack(Items.AIR);
        }

        ItemStack itemStack;

        if (material.matches("(.*\\d.*)")) {
            String[] split = material.split(":");

            int id = Integer.parseInt(split[0]);

            Optional<Item> item = Registry.ITEM.getOptional(new ResourceLocation(
                    "minecraft:" + WebManager.getMaterialIdMap().getOrDefault(id, "stone")));
            itemStack = item.map(ItemStack::new).orElseGet(() -> new ItemStack(Items.AIR));

            if (split.length > 1) {
                itemStack.setDamageValue(Integer.parseInt(split[1]));
            }
        } else {
            Optional<Item> item = Registry.ITEM.getOptional(new ResourceLocation(material));
            itemStack = item.map(ItemStack::new).orElseGet(() -> new ItemStack(Items.AIR));
        }

        itemStack.getOrCreateTag().putBoolean("Unbreakable", true);

        if (itemStack.getItem() == Items.PLAYER_HEAD) {
            HashMap<String, String> ingredientHeadTextures = WebManager.getIngredientHeadTextures();

            if (ingredientHeadTextures.containsKey(name)) {
                CompoundTag skullData = new CompoundTag();
                skullData.putString("Id", UUID.randomUUID().toString());

                CompoundTag properties = new CompoundTag();
                ListTag textures = new ListTag();
                CompoundTag textureEntry = new CompoundTag();
                textureEntry.putString("Value", ingredientHeadTextures.get(name));
                textures.add(textureEntry);
                properties.put("textures", textures);
                skullData.put("Properties", properties);

                itemStack.getOrCreateTag().put("SkullOwner", skullData);
            }
        }

        return itemStack;
    }
}
