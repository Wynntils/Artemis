/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.guides.ingredient;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.ingredients.type.IngredientInfo;
import com.wynntils.models.ingredients.type.IngredientPosition;
import com.wynntils.models.ingredients.type.IngredientTierFormatting;
import com.wynntils.models.items.items.game.IngredientItem;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.screens.guides.GuideItemStack;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public final class GuideIngredientItemStack extends GuideItemStack {
    private final IngredientInfo ingredientInfo;
    private final List<MutableComponent> guideTooltip;

    public GuideIngredientItemStack(IngredientInfo ingredientInfo) {
        super(ingredientInfo.material().itemStack(), new IngredientItem(ingredientInfo), ingredientInfo.name());

        this.ingredientInfo = ingredientInfo;
        this.guideTooltip = generateGuideTooltip();
    }

    @Override
    public List<Component> getTooltipLines(Item.TooltipContext context, Player player, TooltipFlag isAdvanced) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(getHoverName());

        tooltip.addAll(guideTooltip);

        appendObtainInfo(tooltip, Models.Ingredient.getObtainInfo(ingredientInfo));

        tooltip.add(Component.empty());
        if (Services.Favorites.isFavorite(ingredientInfo.name())) {
            tooltip.add(Component.translatable("screens.wynntils.wynntilsGuides.itemGuide.unfavorite")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("screens.wynntils.wynntilsGuides.itemGuide.favorite")
                    .withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.translatable("screens.wynntils.wynntilsGuides.itemGuide.open")
                .withStyle(ChatFormatting.RED));

        return tooltip;
    }

    @Override
    public Component getHoverName() {
        return Component.literal(ingredientInfo.name())
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" " + getTierString(ingredientInfo.tier())));
    }

    private String getTierString(int tier) {
        String tierString = IngredientTierFormatting.fromTierNum(tier).getTierString();

        if (tierString == null) {
            WynntilsMod.warn("Invalid ingredient tier for: " + this.ingredientInfo.name() + ": " + tier);
            return "";
        }

        return tierString;
    }

    private List<MutableComponent> generateGuideTooltip() {
        List<MutableComponent> itemLore = new ArrayList<>();

        itemLore.add(Component.literal("Crafting Ingredient").withStyle(ChatFormatting.DARK_GRAY));
        itemLore.add(Component.empty());

        itemLore.addAll(getStatsLore(ingredientInfo));
        itemLore.addAll(getPositionModifierLore(ingredientInfo));
        itemLore.addAll(getEffectsAndRequirementsLore(ingredientInfo));

        itemLore.add(
                Component.literal("Crafting Lv. Min: " + ingredientInfo.level()).withStyle(ChatFormatting.GRAY));

        for (ProfessionType profession : ingredientInfo.professions()) {
            itemLore.add(Component.literal("  " + profession.getProfessionIconChar() + " ")
                    .append(Component.literal(profession.getDisplayName()).withStyle(ChatFormatting.GRAY)));
        }

        return itemLore;
    }

    private List<MutableComponent> getStatsLore(IngredientInfo ingredientInfo) {
        List<MutableComponent> itemLore = new ArrayList<>();

        for (Pair<StatType, RangedValue> valuedStat : ingredientInfo.variableStats()) {
            if (valuedStat.value().isFixed()) {
                if (valuedStat.value().low() >= 0) {
                    itemLore.add(Component.literal("+" + valuedStat.value().low()
                                    + valuedStat.key().getUnit().getDisplayName())
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(" " + valuedStat.key().getDisplayName())
                                    .withStyle(ChatFormatting.GRAY)));
                } else {
                    itemLore.add(Component.literal(valuedStat.value().low()
                                    + valuedStat.key().getUnit().getDisplayName())
                            .withStyle(ChatFormatting.RED)
                            .append(Component.literal(" " + valuedStat.key().getDisplayName())
                                    .withStyle(ChatFormatting.GRAY)));
                }
            } else {
                if (valuedStat.value().low() >= 0) {
                    itemLore.add(Component.literal("+" + valuedStat.value().low())
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(" to ").withStyle(ChatFormatting.DARK_GREEN))
                            .append(Component.literal(valuedStat.value().high()
                                            + valuedStat.key().getUnit().getDisplayName())
                                    .withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(" " + valuedStat.key().getDisplayName())
                                    .withStyle(ChatFormatting.GRAY)));
                } else {
                    itemLore.add(Component.literal(
                                    String.valueOf(valuedStat.value().low()))
                            .withStyle(ChatFormatting.RED)
                            .append(Component.literal(" to ").withStyle(ChatFormatting.DARK_RED))
                            .append(Component.literal(valuedStat.value().high()
                                            + valuedStat.key().getUnit().getDisplayName())
                                    .withStyle(ChatFormatting.RED))
                            .append(Component.literal(" " + valuedStat.key().getDisplayName())
                                    .withStyle(ChatFormatting.GRAY)));
                }
            }
        }

        if (!itemLore.isEmpty()) {
            itemLore.add(Component.empty());
        }

        return itemLore;
    }

    private List<MutableComponent> getPositionModifierLore(IngredientInfo ingredientInfo) {
        List<MutableComponent> itemLore = new ArrayList<>();

        for (Map.Entry<IngredientPosition, Integer> modifier :
                ingredientInfo.positionModifiers().entrySet()) {
            int value = modifier.getValue();

            String colorCode = value > 0 ? ChatFormatting.GREEN + "+" : ChatFormatting.RED.toString();
            itemLore.add(
                    Component.literal(colorCode + value + "%" + ChatFormatting.GRAY + " Ingredient Effectiveness"));
            itemLore.add(Component.literal(
                    ChatFormatting.GRAY + "(To ingredients " + modifier.getKey().getDisplayName() + " this one)"));
        }

        if (!itemLore.isEmpty()) {
            itemLore.add(Component.empty());
        }

        return itemLore;
    }

    private List<MutableComponent> getEffectsAndRequirementsLore(IngredientInfo ingredientInfo) {
        List<MutableComponent> itemLore = new ArrayList<>();

        if (ingredientInfo.durabilityModifier() != 0 && ingredientInfo.duration() != 0) {
            int duration = ingredientInfo.duration();
            int durability = ingredientInfo.durabilityModifier();
            itemLore.add(getEffectsAndRequirementsLine("Durability", durability, durability > 0)
                    .append(Component.literal(" or ").withStyle(ChatFormatting.GRAY))
                    .append(getEffectsAndRequirementsLine("Duration", duration, duration > 0)));
        } else if (ingredientInfo.durabilityModifier() != 0) {
            int durability = ingredientInfo.durabilityModifier();
            itemLore.add(getEffectsAndRequirementsLine("Durability", durability, durability > 0));
        } else if (ingredientInfo.duration() != 0) {
            int duration = ingredientInfo.duration();
            itemLore.add(getEffectsAndRequirementsLine("Duration", duration, duration > 0));
        }

        if (ingredientInfo.charges() != 0) {
            int charges = ingredientInfo.charges();
            itemLore.add(getEffectsAndRequirementsLine("Charges", charges, charges > 0));
        }

        for (Pair<Skill, Integer> skillReq : ingredientInfo.skillRequirements()) {
            int minLevel = skillReq.value();
            itemLore.add(
                    getEffectsAndRequirementsLine(skillReq.key().getDisplayName() + " Min.", minLevel, minLevel < 0));
        }

        if (!itemLore.isEmpty()) {
            itemLore.add(Component.empty());
        }
        return itemLore;
    }

    private static MutableComponent getEffectsAndRequirementsLine(String effectName, int value, boolean isGood) {
        return Component.literal(StringUtils.toSignedString(value) + " " + effectName)
                .withStyle(isGood ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    public IngredientInfo getIngredientInfo() {
        return ingredientInfo;
    }
}
