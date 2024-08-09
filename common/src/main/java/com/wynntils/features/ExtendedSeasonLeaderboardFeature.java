/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.handlers.labels.event.LabelIdentifiedEvent;
import com.wynntils.handlers.labels.event.LabelsRemovedEvent;
import com.wynntils.handlers.labels.event.TextDisplayChangedEvent;
import com.wynntils.handlers.labels.type.LabelInfo;
import com.wynntils.models.players.label.GuildSeasonLeaderboardHeaderLabelInfo;
import com.wynntils.models.players.label.GuildSeasonLeaderboardLabelInfo;
import com.wynntils.models.players.profile.GuildProfile;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.SubscribeEvent;

public class ExtendedSeasonLeaderboardFeature extends Feature {
    private final Map<Integer, GuildSeasonLeaderboardLabelInfo> labelInfos = new HashMap<>();

    @Persisted
    private final Config<Boolean> useShortSeasonRankingStrings = new Config<>(false);

    @SubscribeEvent
    public void onLabelIdentified(LabelIdentifiedEvent event) {
        if (event.getLabelInfo() instanceof GuildSeasonLeaderboardLabelInfo guildSeasonLeaderboardLabelInfo) {
            labelInfos.put(guildSeasonLeaderboardLabelInfo.getPlace(), guildSeasonLeaderboardLabelInfo);
        }
    }

    @SubscribeEvent
    public void onLabelsRemoved(LabelsRemovedEvent event) {
        event.getRemovedLabels().stream()
                .filter(labelInfo -> labelInfo instanceof GuildSeasonLeaderboardLabelInfo)
                .forEach(labelInfo -> labelInfos.remove(((GuildSeasonLeaderboardLabelInfo) labelInfo).getPlace()));
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        labelInfos.clear();
    }

    @SubscribeEvent
    public void onLabelChanged(TextDisplayChangedEvent.Text event) {
        if (event.getLabelInfo().isEmpty()) return;

        LabelInfo labelInfo = event.getLabelInfo().get();

        if (labelInfo instanceof GuildSeasonLeaderboardHeaderLabelInfo) {
            // Reset every label that is for a place higher than 10
            // A page only has 10 entries, the header only changes when we switch seasons,
            // so we do this to make sure the 10th place is not calculated from the old 11th place data
            List<GuildSeasonLeaderboardLabelInfo> labelsToRemove = labelInfos.values().stream()
                    .filter(info -> info.getPlace() > 10)
                    .toList();

            labelsToRemove.forEach(labelInfoToRemove -> labelInfos.remove(labelInfoToRemove.getPlace()));

            // We also need to update the "last" displayed label, as it is the one that is displayed on the 10th place
            labelInfos.values().stream()
                    .filter(info -> info.getPlace() % 10 == 0)
                    .forEach(this::updateLeaderboardEntityName);

            return;
        }

        if (!(labelInfo instanceof GuildSeasonLeaderboardLabelInfo guildSeasonLeaderboardLabelInfo)) {
            return;
        }

        // Update the changed label data
        labelInfos.put(guildSeasonLeaderboardLabelInfo.getPlace(), guildSeasonLeaderboardLabelInfo);

        // Cancel the event to prevent the label from being changed
        event.setCanceled(true);

        // Update this entity's name
        updateLeaderboardEntityName(guildSeasonLeaderboardLabelInfo);

        // Also update the name of the entity that is "before" this entity in ranking
        // But only, if this is not the "first" entity in ranking (the first element that is displayed on the page)
        if (guildSeasonLeaderboardLabelInfo.getPlace() % 10 != 1) {
            labelInfos.values().stream()
                    .filter(info -> info.getPlace() == guildSeasonLeaderboardLabelInfo.getPlace() - 1)
                    .findFirst()
                    .ifPresent(this::updateLeaderboardEntityName);
        }
    }

    private void updateLeaderboardEntityName(GuildSeasonLeaderboardLabelInfo guildSeasonLeaderboardLabelInfo) {
        // Find the label info that is "after" the current label info, in ranking
        GuildSeasonLeaderboardLabelInfo nextLabelInfo = labelInfos.values().stream()
                .filter(info -> info.getPlace() == guildSeasonLeaderboardLabelInfo.getPlace() + 1)
                .findFirst()
                .orElse(null);

        Optional<GuildProfile> guildProfile = Models.Guild.getGuildProfile(guildSeasonLeaderboardLabelInfo.getGuild());

        // Update the current label info with the additional information

        // Place color:
        // 1-3 - Gold
        // 4-6 - Yellow
        // 7-9 - White
        // 10+ - Gray
        ChatFormatting placeColor = ChatFormatting.GRAY;
        if (guildSeasonLeaderboardLabelInfo.getPlace() <= 3) {
            placeColor = ChatFormatting.GOLD;
        } else if (guildSeasonLeaderboardLabelInfo.getPlace() <= 6) {
            placeColor = ChatFormatting.YELLOW;
        } else if (guildSeasonLeaderboardLabelInfo.getPlace() <= 9) {
            placeColor = ChatFormatting.WHITE;
        }

        String scoreString;
        String scoreDiffString;
        if (useShortSeasonRankingStrings.get()) {
            scoreString = StringUtils.integerToShortString(guildSeasonLeaderboardLabelInfo.getScore());
            scoreDiffString = StringUtils.integerToShortString(
                    nextLabelInfo == null ? 0 : guildSeasonLeaderboardLabelInfo.getScore() - nextLabelInfo.getScore());
        } else {
            scoreString = String.format("%,d", guildSeasonLeaderboardLabelInfo.getScore());
            scoreDiffString = String.format(
                    "%,d",
                    nextLabelInfo == null ? 0 : guildSeasonLeaderboardLabelInfo.getScore() - nextLabelInfo.getScore());
        }

        ChatFormatting guildStyle = guildSeasonLeaderboardLabelInfo.getGuild().equals(Models.Guild.getGuildName())
                ? ChatFormatting.GREEN
                : ChatFormatting.AQUA;
        // §<color><place>§7 - §b<guild name> [<guild tag>]§d (<formatted score> SR) §a(+<score diff to next>)
        MutableComponent newLabel = Component.empty()
                .append(Component.literal(String.valueOf(guildSeasonLeaderboardLabelInfo.getPlace()))
                        .withStyle(
                                guildSeasonLeaderboardLabelInfo.getPlace() == 1
                                        ? ChatFormatting.BOLD
                                        : ChatFormatting.RESET)
                        .withStyle(placeColor))
                .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(guildSeasonLeaderboardLabelInfo.getGuild())
                        .withStyle(guildStyle))
                .append(Component.literal(
                                " [" + guildProfile.map(GuildProfile::prefix).orElse("???") + "]")
                        .withStyle(guildStyle))
                .append(Component.literal(" (" + scoreString + " SR)").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" (+" + (nextLabelInfo == null ? "???" : scoreDiffString) + ")")
                        .withStyle(ChatFormatting.GREEN));

        guildSeasonLeaderboardLabelInfo.getEntity().setCustomName(newLabel);
    }
}
