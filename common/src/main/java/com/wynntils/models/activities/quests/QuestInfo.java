/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.activities.quests;

import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.type.ActivityDifficulty;
import com.wynntils.models.activities.type.ActivityLength;
import com.wynntils.models.activities.type.ActivityRequirements;
import com.wynntils.models.activities.type.ActivityStatus;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.mc.RenderedStringUtils;
import com.wynntils.utils.mc.StyledTextUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public record QuestInfo(
        String name,
        String specialInfo,
        ActivityDifficulty difficulty,
        ActivityStatus status,
        ActivityLength length,
        int level,
        StyledText nextTask,
        ActivityRequirements additionalRequirements,
        boolean isMiniQuest,
        List<String> rewards) {
    private static final int NEXT_TASK_MAX_WIDTH = 200;

    public boolean trackable() {
        return status == ActivityStatus.AVAILABLE || status == ActivityStatus.STARTED;
    }

    public Optional<Location> nextLocation() {
        return StyledTextUtils.extractLocation(nextTask);
    }

    public int sortLevel() {
        return !isMiniQuest || additionalRequirements.level().a() != 0
                ? level
                : additionalRequirements.professionLevels().getFirst().a().b();
    }

    public static List<Component> generateTooltipForQuest(QuestInfo questInfo) {
        List<Component> tooltipLines = new ArrayList<>();

        tooltipLines.add(Component.literal(questInfo.name())
                .withStyle(ChatFormatting.BOLD)
                .withStyle(ChatFormatting.WHITE));

        tooltipLines.add(questInfo.status().getQuestStateComponent());
        if (questInfo.specialInfo() != null) {
            tooltipLines.add(Component.literal(questInfo.specialInfo()).withStyle(ChatFormatting.GREEN));
        }

        tooltipLines.add(Component.literal(""));
        if (!questInfo.isMiniQuest()
                || questInfo.additionalRequirements().level().a() != 0) {
            tooltipLines.add((Models.CombatXp.getCombatLevel().current() >= questInfo.level()
                            ? Component.literal("✔").withStyle(ChatFormatting.GREEN)
                            : Component.literal("✖").withStyle(ChatFormatting.RED))
                    .append(Component.literal(" Combat Lv. Min: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(questInfo.level())).withStyle(ChatFormatting.WHITE)));
        }

        for (Pair<Pair<ProfessionType, Integer>, Boolean> professionRequirement :
                questInfo.additionalRequirements().professionLevels()) {
            MutableComponent base = professionRequirement.b()
                    ? Component.literal("✔ ").withStyle(ChatFormatting.GREEN)
                    : Component.literal("✖ ").withStyle(ChatFormatting.RED);

            tooltipLines.add(
                    base.append(Component.literal(professionRequirement.a().a().getDisplayName() + " Lv. Min: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(String.valueOf(
                                            professionRequirement.a().b()))
                                    .withStyle(ChatFormatting.WHITE))));
        }

        for (Pair<String, Boolean> questRequirement :
                questInfo.additionalRequirements().quests()) {
            MutableComponent base = questRequirement.b()
                    ? Component.literal("✔ ").withStyle(ChatFormatting.GREEN)
                    : Component.literal("✖ ").withStyle(ChatFormatting.RED);

            tooltipLines.add(base.append(Component.literal("Quest Req: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(questRequirement.a()))
                            .withStyle(ChatFormatting.WHITE))));
        }

        tooltipLines.add(Component.literal(""));

        tooltipLines.add(Component.literal("-")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" Length: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(StringUtils.capitalizeFirst(
                                questInfo.length().toString().toLowerCase(Locale.ROOT)))
                        .withStyle(ChatFormatting.WHITE)));
        tooltipLines.add(Component.literal("-")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" Difficulty: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(StringUtils.capitalizeFirst(
                                questInfo.difficulty().toString().toLowerCase(Locale.ROOT)))
                        .withStyle(ChatFormatting.WHITE)));

        if (questInfo.status() != ActivityStatus.COMPLETED
                && !questInfo.nextTask().isEmpty()) {
            tooltipLines.add(Component.literal(""));
            StyledText nextTask = questInfo.nextTask();
            StyledText[] lines = RenderedStringUtils.wrapTextBySize(nextTask, NEXT_TASK_MAX_WIDTH);

            for (StyledText line : lines) {
                tooltipLines.add(
                        Component.empty().withStyle(ChatFormatting.GRAY).append(line.getComponent()));
            }
        }

        tooltipLines.add(Component.literal(""));
        tooltipLines.add(Component.literal("Rewards:").withStyle(ChatFormatting.LIGHT_PURPLE));
        for (String reward : questInfo.rewards()) {
            tooltipLines.add(Component.literal("- ")
                    .withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal(reward).withStyle(ChatFormatting.GRAY)));
        }

        return tooltipLines;
    }
}
