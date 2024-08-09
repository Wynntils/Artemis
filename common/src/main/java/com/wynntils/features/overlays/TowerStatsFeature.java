/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.overlays;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.annotations.OverlayInfo;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.models.war.event.GuildWarEvent;
import com.wynntils.models.war.type.WarBattleInfo;
import com.wynntils.models.war.type.WarTowerState;
import com.wynntils.overlays.TowerStatsOverlay;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.OVERLAYS)
public class TowerStatsFeature extends Feature {
    private static final int SEPARATOR_LENGTH = 40;

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    public final Overlay towerStatsOverlay = new TowerStatsOverlay();

    @Persisted
    private final Config<Boolean> printTowerStatsOnEnd = new Config<>(true);

    @SubscribeEvent
    public void onWarEnd(GuildWarEvent.Ended event) {
        if (!printTowerStatsOnEnd.get()) return;

        WarBattleInfo warBattleInfo = event.getWarBattleInfo();
        WarTowerState initialTowerState = warBattleInfo.getInitialState();
        WarTowerState endTowerState = warBattleInfo.getCurrentState();

        MutableComponent message =
                Component.empty().append(getSeparatorComponent("Tower Stats - Initial", SEPARATOR_LENGTH));

        message = message.append(
                getTowerStatsComponent(warBattleInfo.getTerritory(), warBattleInfo.getOwnerGuild(), initialTowerState));

        if (!endTowerState.equals(initialTowerState)) {
            message = message.append(getSeparatorComponent("Tower Stats - End", SEPARATOR_LENGTH))
                    .append(getTowerStatsComponent(
                            warBattleInfo.getTerritory(), warBattleInfo.getOwnerGuild(), endTowerState));
        }

        message = message.append(Component.literal("\uD83D\uDD51 Time in War: ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("%s"
                                        .formatted(StringUtils.formatDuration(warBattleInfo.getTotalLengthSeconds())))
                                .withStyle(ChatFormatting.WHITE)))
                .append(Component.literal("\n"))
                .append(Component.literal("⚔ DPS: ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("%s"
                                        .formatted(
                                                StringUtils.integerToShortString(warBattleInfo.getDps(Long.MAX_VALUE))))
                                .withStyle(ChatFormatting.WHITE)))
                .append(Component.literal("\n"));

        message = message.append(Component.literal("%s".formatted("=".repeat(SEPARATOR_LENGTH)))
                .withStyle(ChatFormatting.BLUE)
                .withStyle(ChatFormatting.STRIKETHROUGH));

        String messageString = StyledText.fromComponent(message).getStringWithoutFormatting();
        message = message.withStyle(style -> style.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy to clipboard.")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, messageString)));

        McUtils.sendMessageToClient(message);
    }

    private static MutableComponent getSeparatorComponent(String text, int separatorLength) {
        String separator = "=".repeat(separatorLength);
        int textLength = text.length();
        int totalPadding = separatorLength - textLength;
        int paddingSide = totalPadding / 2;
        String padding = " ".repeat(paddingSide);

        return Component.empty()
                .append(Component.literal(separator + "\n")
                        .withStyle(ChatFormatting.BLUE, ChatFormatting.STRIKETHROUGH))
                .append(Component.literal(padding + text + padding + "\n")
                        .withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD))
                .append(Component.literal(separator + "\n")
                        .withStyle(ChatFormatting.BLUE, ChatFormatting.STRIKETHROUGH));
    }

    private static MutableComponent getTowerStatsComponent(
            String territory, String ownerGuild, WarTowerState initialTowerState) {
        return Component.empty()
                .append(Component.literal(territory)
                        .withStyle(ChatFormatting.GOLD)
                        .withStyle(ChatFormatting.BOLD)
                        .append(Component.literal(" [")
                                .withStyle(ChatFormatting.DARK_AQUA)
                                .append(Component.literal("%s".formatted(ownerGuild))
                                        .withStyle(ChatFormatting.AQUA)
                                        .append(Component.literal("]").withStyle(ChatFormatting.DARK_AQUA)))))
                .append(Component.literal("\n"))
                .append(Component.literal("✣ Damage: ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("%s"
                                        .formatted(initialTowerState.damage().low()))
                                .withStyle(ChatFormatting.WHITE)
                                .append(Component.literal(" - ")
                                        .withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal("%s"
                                                        .formatted(initialTowerState
                                                                .damage()
                                                                .high()))
                                                .withStyle(ChatFormatting.WHITE))))
                        .append(Component.literal("\n")))
                .append(Component.literal("➡ Attack Speed: ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("%.2fx".formatted(initialTowerState.attackSpeed()))
                                .withStyle(ChatFormatting.WHITE)))
                .append(Component.literal("\n"))
                .append(Component.literal("❤ Health: ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal("%d".formatted(initialTowerState.health()))
                                .withStyle(ChatFormatting.WHITE)))
                .append(Component.literal("\n"))
                .append(Component.literal("⛨ Defense: ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(Component.literal("%.2f".formatted(initialTowerState.defense()) + "%")
                                .withStyle(ChatFormatting.WHITE)))
                .append(Component.literal("\n"));
    }
}
