/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.itemrecord.type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.wynntils.core.components.Models;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.encoding.type.EncodingSettings;
import com.wynntils.utils.EncodedByteBuffer;
import com.wynntils.utils.type.ErrorOr;
import java.lang.reflect.Type;
import java.util.Set;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;

public record SavedItem(String base64, Set<String> categories, ItemStack itemStack) implements Comparable<SavedItem> {
    // This is the encoding settings used to encode the item when it was saved
    // We cannot let users not save extended identification and share item name as it would break the item if the API
    // changes
    private static final EncodingSettings SAVED_ITEM_ENCODING_SETTINGS = new EncodingSettings(true, true);

    public static SavedItem create(WynnItem wynnItem, Set<String> categories, ItemStack itemStack) {
        ErrorOr<EncodedByteBuffer> errorOrEncodedByteBuffer =
                Models.ItemEncoding.encodeItem(wynnItem, SAVED_ITEM_ENCODING_SETTINGS);

        if (errorOrEncodedByteBuffer.hasError()) {
            throw new IllegalArgumentException(
                    "Tried to construct a SavedItem with unencodable WynnItem: " + errorOrEncodedByteBuffer.getError());
        }

        return new SavedItem(errorOrEncodedByteBuffer.getValue().toBase64String(), categories, itemStack);
    }

    /**
     * @return The wynnItem represented by this SavedItem
     * Note that this can't be done during deserialization because the models might not have finished loading yet
     */
    public WynnItem wynnItem() {
        ErrorOr<WynnItem> errorOrWynnItem = Models.ItemEncoding.decodeItem(EncodedByteBuffer.fromBase64String(base64));

        if (errorOrWynnItem.hasError()) {
            throw new IllegalStateException(
                    "Tried to decode a SavedItem with unencodable WynnItem: " + errorOrWynnItem.getError());
        }

        return errorOrWynnItem.getValue();
    }

    @Override
    public int compareTo(SavedItem other) {
        return this.base64.compareTo(other.base64);
    }

    public static class SavedItemSerializer implements JsonSerializer<SavedItem>, JsonDeserializer<SavedItem> {
        @Override
        public SavedItem deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            // Get base64 from jsonObject
            String base64 = jsonObject.get("base64").getAsString();

            // Get categories from jsonObject
            Set<String> categories = context.deserialize(jsonObject.get("categories"), Set.class);

            // Get itemStackInfo from jsonObject
            ItemStackInfo itemStackInfo = context.deserialize(jsonObject.get("itemStackInfo"), ItemStackInfo.class);

            // Create itemStack from itemStackInfo
            ItemStack itemStack = new ItemStack(Item.byId(itemStackInfo.itemId), 1);
            DataComponentMap.Builder componentsBuilder = DataComponentMap.builder()
                    .set(DataComponents.DAMAGE, itemStackInfo.damage)
                    .set(DataComponents.UNBREAKABLE, new Unbreakable(false))
                    .set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);

            if (itemStackInfo.color != -1) {
                componentsBuilder.set(DataComponents.DYED_COLOR, new DyedItemColor(itemStackInfo.color, false));
            }

            itemStack.applyComponents(componentsBuilder.build());

            // Also hide the attribute modifiers tooltip
            itemStack.set(
                    DataComponents.ATTRIBUTE_MODIFIERS,
                    itemStack
                            .getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                            .withTooltip(false));

            return new SavedItem(base64, categories, itemStack);
        }

        @Override
        public JsonElement serialize(SavedItem src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();

            // Add base64 to jsonObject
            jsonObject.addProperty("base64", src.base64());

            // Add categories to jsonObject
            jsonObject.add("categories", context.serialize(src.categories()));

            ItemStack itemStack = src.itemStack();

            DataComponentMap components = itemStack.getComponents();

            // Leather armor can be dyed, we need to store the color
            int color = components
                    .getOrDefault(DataComponents.DYED_COLOR, new DyedItemColor(-1, false))
                    .rgb();

            int damage = components.getOrDefault(DataComponents.DAMAGE, 0);
            boolean unbreakable = components.has(DataComponents.UNBREAKABLE);

            // Note: HideFlags is kept as a boolean for compatibility with the old system
            ItemStackInfo itemStackInfo =
                    new ItemStackInfo(Item.getId(itemStack.getItem()), damage, unbreakable, color);

            // Add itemStackInfo to jsonObject
            jsonObject.add("itemStackInfo", context.serialize(itemStackInfo));

            return jsonObject;
        }
    }

    private record ItemStackInfo(int itemId, int damage, boolean unbreakable, int color) {}
}
