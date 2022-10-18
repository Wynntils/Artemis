/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.item;

import com.wynntils.core.webapi.WebManager;
import com.wynntils.core.webapi.profiles.ingredient.IngredientIdentificationContainer;
import com.wynntils.core.webapi.profiles.ingredient.IngredientItemModifiers;
import com.wynntils.core.webapi.profiles.ingredient.IngredientModifiers;
import com.wynntils.core.webapi.profiles.ingredient.IngredientProfile;
import com.wynntils.core.webapi.profiles.ingredient.ProfessionType;
import com.wynntils.core.webapi.profiles.item.IdentificationProfile;
import com.wynntils.wynn.item.parsers.WynnItemMatchers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class IngredientItemStack extends WynnItemStack {
    private final boolean isGuideStack;

    private final List<Component> guideTooltip;

    private final IngredientProfile ingredientProfile;

    public IngredientItemStack(ItemStack stack) {
        super(stack);

        Matcher matcher = WynnItemMatchers.ingredientOrMaterialMatcher(stack.getHoverName());
        if (!matcher.matches()) {
            throw new IllegalStateException("Matcher did not match for IngredientItemStack");
        }

        ingredientProfile = WebManager.getIngredients().get(matcher.group(1));

        isGuideStack = false;
        guideTooltip = List.of();
    }

    public IngredientItemStack(IngredientProfile ingredientProfile) {
        super(ingredientProfile.asItemStack());

        CompoundTag tag = this.getOrCreateTag();
        tag.putBoolean("Unbreakable", true);

        this.ingredientProfile = ingredientProfile;

        isGuideStack = true;
        guideTooltip = generateGuideTooltip();
    }

    @Override
    public List<Component> getTooltipLines(Player player, TooltipFlag isAdvanced) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(getHoverName());

        if (isGuideStack) {
            tooltip.addAll(guideTooltip);
            return tooltip;
        }

        return super.getTooltipLines(player, isAdvanced);
    }

    @Override
    public Component getHoverName() {
        if (isGuideStack) {
            return new TextComponent(ingredientProfile.getDisplayName())
                    .withStyle(ChatFormatting.GRAY)
                    .append(new TextComponent(" " + ingredientProfile.getTier().getTierString()));
        }

        return super.getHoverName();
    }

    private List<Component> generateGuideTooltip() {
        List<Component> itemLore = new ArrayList<>();

        itemLore.add(new TextComponent("Crafting Ingredient").withStyle(ChatFormatting.DARK_GRAY));
        itemLore.add(TextComponent.EMPTY);

        Map<String, IngredientIdentificationContainer> statuses = ingredientProfile.getStatuses();

        for (String status : statuses.keySet()) {
            IngredientIdentificationContainer identificationContainer = statuses.get(status);
            if (identificationContainer.hasConstantValue()) {
                if (identificationContainer.getMin() >= 0) {
                    itemLore.add(new TextComponent("+" + identificationContainer.getMin()
                                    + identificationContainer.getType().getInGame(status))
                            .withStyle(ChatFormatting.GREEN)
                            .append(new TextComponent(" " + IdentificationProfile.getAsLongName(status))
                                    .withStyle(ChatFormatting.GRAY)));
                } else {
                    itemLore.add(new TextComponent(identificationContainer.getMin()
                                    + identificationContainer.getType().getInGame(status))
                            .withStyle(ChatFormatting.RED)
                            .append(new TextComponent(" " + IdentificationProfile.getAsLongName(status))
                                    .withStyle(ChatFormatting.GRAY)));
                }
            } else {
                if (identificationContainer.getMin() >= 0) {
                    itemLore.add(new TextComponent("+" + identificationContainer.getMin())
                            .withStyle(ChatFormatting.GREEN)
                            .append(new TextComponent(" to ").withStyle(ChatFormatting.DARK_GREEN))
                            .append(new TextComponent(identificationContainer.getMax()
                                            + identificationContainer.getType().getInGame(status))
                                    .withStyle(ChatFormatting.GREEN))
                            .append(new TextComponent(" " + IdentificationProfile.getAsLongName(status))
                                    .withStyle(ChatFormatting.GRAY)));
                } else {
                    itemLore.add(new TextComponent(String.valueOf(identificationContainer.getMin()))
                            .withStyle(ChatFormatting.RED)
                            .append(new TextComponent(" to ").withStyle(ChatFormatting.DARK_RED))
                            .append(new TextComponent(identificationContainer.getMax()
                                            + identificationContainer.getType().getInGame(status))
                                    .withStyle(ChatFormatting.RED))
                            .append(new TextComponent(" " + IdentificationProfile.getAsLongName(status))
                                    .withStyle(ChatFormatting.GRAY)));
                }
            }
        }

        if (statuses.size() > 0) {
            itemLore.add(TextComponent.EMPTY);
        }

        IngredientModifiers ingredientModifiers = ingredientProfile.getIngredientModifiers();
        itemLore.addAll(ingredientModifiers.getModifierLoreLines());

        if (ingredientModifiers.anyExists()) {
            itemLore.add(TextComponent.EMPTY);
        }

        IngredientItemModifiers itemModifiers = ingredientProfile.getItemModifiers();
        itemLore.addAll(itemModifiers.getItemModifierLoreLines());

        if (itemModifiers.anyExists()) {
            itemLore.add(TextComponent.EMPTY);
        }

        if (ingredientProfile.isUntradeable())
            itemLore.add(new TextComponent("Untradable Item").withStyle(ChatFormatting.RED));

        itemLore.add(
                new TextComponent("Crafting Lv. Min: " + ingredientProfile.getLevel()).withStyle(ChatFormatting.GRAY));

        for (ProfessionType profession : ingredientProfile.getProfessions()) {
            itemLore.add(new TextComponent("  " + profession.getProfessionIconChar() + " ")
                    .append(new TextComponent(profession.getDisplayName()).withStyle(ChatFormatting.GRAY)));
        }

        return itemLore;
    }

    public IngredientProfile getIngredientProfile() {
        return ingredientProfile;
    }
}
