/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.custom.item;

import com.wynntils.wc.custom.item.properties.ItemProperty;
import com.wynntils.wc.custom.item.properties.type.PropertyType;
import com.wynntils.wc.utils.WynnUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag.Default;

public class WynnItemStack extends ItemStack {
    protected final String itemName;
    private final List<ItemProperty> properties = new ArrayList<>();

    public WynnItemStack(ItemStack stack) {
        super(stack.getItem(), stack.getCount());
        if (stack.getTag() != null) setTag(stack.getTag());

        itemName = WynnUtils.normalizeBadString(
                ChatFormatting.stripFormatting(super.getHoverName().getString()));
    }

    public String getSimpleName() {
        return itemName;
    }

    public List<Component> getOriginalTooltip() {
        return super.getTooltipLines(null, Default.NORMAL);
    }

    public void addProperty(ItemProperty property) {
        if (hasProperty(property.getClass())) return; // don't allow duplicate properties
        this.properties.add(property);
    }

    /**
     * Returns the specified property or the first property of the specified type, if it exists.
     * Otherwise, returns null.
     */
    public <T> T getProperty(Class<T> propertyType) {
        for (ItemProperty property : properties) {
            if (propertyType.isAssignableFrom(property.getClass())) return propertyType.cast(property);
        }
        return null; // no match
    }

    /**
     * Returns true if the specified property, or a property of the specified type, is present
     */
    public boolean hasProperty(Class<?> propertyType) {
        // getProperty returns null if no property of the given type is present
        return (getProperty(propertyType) != null);
    }

    /**
     * Returns all the present properties of the specified type.
     */
    public <T extends PropertyType> List<T> getProperties(Class<T> propertyType) {
        List<T> collected = new ArrayList<>();
        for (ItemProperty property : properties) {
            if (propertyType.isAssignableFrom(property.getClass())) collected.add((T) property);
        }
        return collected;
    }
}
