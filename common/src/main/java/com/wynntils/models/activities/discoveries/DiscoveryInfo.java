/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.activities.discoveries;

import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.type.ActivityInfo;
import com.wynntils.models.activities.type.ActivityStatus;
import com.wynntils.models.activities.type.DiscoveryType;
import com.wynntils.utils.mc.ComponentUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public record DiscoveryInfo(
        String name,
        DiscoveryType type,
        String description,
        int minLevel,
        boolean discovered,
        List<String> requirements,
        List<Component> displayLore) {
    public DiscoveryInfo(String name, DiscoveryType type, String description, int minLevel, boolean discovered) {
        this(
                name,
                type,
                description,
                minLevel,
                discovered,
                List.of(),
                generateLore(name, type, description, minLevel, discovered));
    }

    public static DiscoveryInfo fromActivityInfo(ActivityInfo activityInfo) {
        return new DiscoveryInfo(
                activityInfo.name(),
                DiscoveryType.fromActivityType(activityInfo.type()),
                activityInfo.description().orElse(StyledText.EMPTY).getString(),
                activityInfo.requirements().level().key(),
                activityInfo.status() == ActivityStatus.COMPLETED);
    }

    public static List<Component> generateLore(
            String name, DiscoveryType type, String description, int minLevel, boolean discovered) {
        List<Component> lore = new ArrayList<>();

        lore.add(Component.literal(name).withStyle(type.getColor()).withStyle(ChatFormatting.BOLD));

        if (Models.CombatXp.getCombatLevel().current() >= minLevel) {
            lore.add(Component.literal("✔")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(" Combat Lv. Min: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(String.valueOf(minLevel)).withStyle(ChatFormatting.WHITE))));
        } else {
            lore.add(Component.literal("✘")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(" Combat Lv. Min: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(String.valueOf(minLevel)).withStyle(ChatFormatting.WHITE))));
        }

        lore.add(Component.empty());

        if (discovered) {
            lore.add(Component.literal("Discovered").withStyle(ChatFormatting.GREEN));
        } else {
            lore.add(Component.literal("Not Discovered").withStyle(ChatFormatting.RED));
        }

        if (!description.isEmpty()) {
            lore.add(Component.empty());
            lore.addAll(ComponentUtils.wrapTooltips(
                    List.of(Component.literal(description).withStyle(ChatFormatting.GRAY)), 300));
        }

        return lore;
    }
}
