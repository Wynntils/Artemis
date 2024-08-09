/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.statistics;

import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.models.containers.containers.reward.RewardContainer;
import com.wynntils.models.containers.event.MythicFoundEvent;
import com.wynntils.models.damage.type.DamageDealtEvent;
import com.wynntils.models.lootrun.event.LootrunFinishedEvent;
import com.wynntils.models.raid.event.RaidEndedEvent;
import com.wynntils.models.spells.event.SpellEvent;
import com.wynntils.models.war.event.GuildWarEvent;
import com.wynntils.services.statistics.type.StatisticKind;
import net.neoforged.bus.api.SubscribeEvent;

public final class StatisticsCollectors {
    @SubscribeEvent
    public void onDamageDealtEvent(DamageDealtEvent event) {
        long damageSum = event.getDamages().values().stream().mapToLong(d -> d).sum();
        Services.Statistics.addToStatistics(StatisticKind.DAMAGE_DEALT, damageSum);
    }

    @SubscribeEvent
    public void onSpellEvent(SpellEvent.Completed event) {
        Services.Statistics.increaseStatistics(StatisticKind.SPELLS_CAST);
    }

    @SubscribeEvent
    public void onLootrunCompleted(LootrunFinishedEvent.Completed event) {
        Services.Statistics.increaseStatistics(StatisticKind.LOOTRUNS_COMPLETED);

        Services.Statistics.addToStatistics(
                StatisticKind.LOOTRUNS_CHALLENGES_COMPLETED, event.getChallengesCompleted());
        Services.Statistics.addToStatistics(StatisticKind.LOOTRUNS_TIME_ELAPSED, event.getTimeElapsed());
        Services.Statistics.addToStatistics(StatisticKind.LOOTRUNS_REWARD_PULLS, event.getRewardPulls());
        Services.Statistics.addToStatistics(StatisticKind.LOOTRUNS_REWARD_REROLLS, event.getRewardRerolls());
        Services.Statistics.addToStatistics(StatisticKind.LOOTRUNS_EXPERIENCE_GAINED, event.getExperienceGained());
        Services.Statistics.addToStatistics(StatisticKind.LOOTRUNS_MOBS_KILLED, event.getMobsKilled());
    }

    @SubscribeEvent
    public void onLootrunFailed(LootrunFinishedEvent.Failed event) {
        Services.Statistics.increaseStatistics(StatisticKind.LOOTRUNS_FAILED);

        Services.Statistics.addToStatistics(
                StatisticKind.LOOTRUNS_CHALLENGES_COMPLETED, event.getChallengesCompleted());
        Services.Statistics.addToStatistics(StatisticKind.LOOTRUNS_TIME_ELAPSED, event.getTimeElapsed());
    }

    @SubscribeEvent
    public void onMythicFoundEvent(MythicFoundEvent event) {
        Services.Statistics.increaseStatistics(StatisticKind.MYTHICS_FOUND);

        if (event.isLootrunEndReward()) {
            Services.Statistics.addToStatistics(
                    StatisticKind.LOOTRUNS_PULLS_WITHOUT_MYTHIC, Models.Lootrun.dryPulls.get());
        }
    }

    @SubscribeEvent
    public void onRaidCompleted(RaidEndedEvent.Completed event) {
        switch (event.getRaid()) {
            case NEST_OF_THE_GROOTSLANGS -> {
                Services.Statistics.increaseStatistics(StatisticKind.NEST_OF_THE_GROOTSLANGS_SUCCEEDED);
                Services.Statistics.addToStatistics(
                        StatisticKind.NEST_OF_THE_GROOTSLANGS_TIME_ELAPSED, event.getRaidTime() / 1000);
            }
            case ORPHIONS_NEXUS_OF_LIGHT -> {
                Services.Statistics.increaseStatistics(StatisticKind.ORPHIONS_NEXUS_OF_LIGHT_SUCCEEDED);
                Services.Statistics.addToStatistics(
                        StatisticKind.ORPHIONS_NEXUS_OF_LIGHT_TIME_ELAPSED, event.getRaidTime() / 1000);
            }
            case THE_CANYON_COLOSSUS -> {
                Services.Statistics.increaseStatistics(StatisticKind.THE_CANYON_COLOSSUS_SUCCEEDED);
                Services.Statistics.addToStatistics(
                        StatisticKind.THE_CANYON_COLOSSUS_TIME_ELAPSED, event.getRaidTime() / 1000);
            }
            case THE_NAMELESS_ANOMALY -> {
                Services.Statistics.increaseStatistics(StatisticKind.THE_NAMELESS_ANOMALY_SUCCEEDED);
                Services.Statistics.addToStatistics(
                        StatisticKind.THE_NAMELESS_ANOMALY_TIME_ELAPSED, event.getRaidTime() / 1000);
            }
        }
    }

    @SubscribeEvent
    public void onRaidFailed(RaidEndedEvent.Failed event) {
        switch (event.getRaid()) {
            case NEST_OF_THE_GROOTSLANGS -> {
                Services.Statistics.increaseStatistics(StatisticKind.NEST_OF_THE_GROOTSLANGS_FAILED);
                Services.Statistics.addToStatistics(
                        StatisticKind.NEST_OF_THE_GROOTSLANGS_TIME_ELAPSED, event.getRaidTime() / 1000);
            }
            case ORPHIONS_NEXUS_OF_LIGHT -> {
                Services.Statistics.increaseStatistics(StatisticKind.ORPHIONS_NEXUS_OF_LIGHT_FAILED);
                Services.Statistics.addToStatistics(
                        StatisticKind.ORPHIONS_NEXUS_OF_LIGHT_TIME_ELAPSED, event.getRaidTime() / 1000);
            }
            case THE_CANYON_COLOSSUS -> {
                Services.Statistics.increaseStatistics(StatisticKind.THE_CANYON_COLOSSUS_FAILED);
                Services.Statistics.addToStatistics(
                        StatisticKind.THE_CANYON_COLOSSUS_TIME_ELAPSED, event.getRaidTime() / 1000);
            }
            case THE_NAMELESS_ANOMALY -> {
                Services.Statistics.increaseStatistics(StatisticKind.THE_NAMELESS_ANOMALY_FAILED);
                Services.Statistics.addToStatistics(
                        StatisticKind.THE_NAMELESS_ANOMALY_TIME_ELAPSED, event.getRaidTime() / 1000);
            }
        }
    }

    @SubscribeEvent
    public void onRewardContainerOpened(ScreenInitEvent e) {
        if (Models.Container.getCurrentContainer() instanceof RewardContainer rewardContainer) {
            Services.Statistics.increaseStatistics(StatisticKind.LOOTRUNS_CHESTS_OPENED);
        }
    }

    @SubscribeEvent
    public void onWarJoinedEvent(GuildWarEvent.Started event) {
        Services.Statistics.increaseStatistics(StatisticKind.WARS_JOINED);
    }
}
