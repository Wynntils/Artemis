/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.game;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemAnnotator;
import com.wynntils.models.gear.ingredients.IngredientInfo;
import com.wynntils.models.items.items.game.IngredientItem;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;

public final class IngredientAnnotator implements ItemAnnotator {
    private static final Pattern INGREDIENT_PATTERN =
            Pattern.compile("^§7(.*)§[3567] \\[§([8bde])✫(§8)?✫(§8)?✫§[3567]\\]$");

    @Override
    public ItemAnnotation getAnnotation(ItemStack itemStack, String name) {
        Matcher matcher = INGREDIENT_PATTERN.matcher(name);
        if (!matcher.matches()) return null;

        String ingredientName = matcher.group(1);
        String tierColor = matcher.group(2);

        int tier = Models.Ingredient.getTierFromColorCode(tierColor);
        IngredientInfo ingredientInfo = Models.Ingredient.fromName(ingredientName);
        if (ingredientInfo == null) return null;

        if (ingredientInfo.tier() != tier) {
            WynntilsMod.warn("Incorrect tier in ingredient database: " + ingredientName + " is " + tier);
            return null;
        }

        return new IngredientItem(ingredientInfo);
    }
}
