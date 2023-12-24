/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.tooltip.type;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Represents a tooltip component generator that can be used in {@link com.wynntils.handlers.tooltip.TooltipBuilder}
 * @param <T> The type of the gear info
 * @param <U> The type of the gear instance
 */
public abstract class TooltipComponent<T, U> {
    public abstract List<Component> buildHeaderTooltip(T itemInfo, U itemInstance, boolean hideUnidentified);

    public abstract List<Component> buildFooterTooltip(T itemInfo, U itemInstance);

    protected MutableComponent buildRequirementLine(String requirementName, boolean fulfilled) {
        MutableComponent requirement;

        requirement = fulfilled
                ? Component.literal("✔ ").withStyle(ChatFormatting.GREEN)
                : Component.literal("✖ ").withStyle(ChatFormatting.RED);
        requirement.append(Component.literal(requirementName).withStyle(ChatFormatting.GRAY));
        return requirement;
    }
}
