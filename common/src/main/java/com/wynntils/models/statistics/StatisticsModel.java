/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.statistics;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.storage.Storage;
import com.wynntils.models.character.event.CharacterUpdateEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// This should really be an "ExternalModel"...
public final class StatisticsModel extends Model {
    private final StatisticsCollectors collectors = new StatisticsCollectors();

    // All statistics, per character
    private final Storage<Map<String, Map<StatisticKind, StatisticEntry>>> statistics = new Storage<>(new TreeMap<>());

    // The currently active statistics
    private Map<StatisticKind, StatisticEntry> currentStatistics = new EnumMap<>(StatisticKind.class);

    public StatisticsModel() {
        super(List.of());
        WynntilsMod.registerEventListener(collectors);
    }

    @SubscribeEvent
    public void onWorldChange(WorldStateEvent event) {
        if (!Models.Character.hasCharacter()) {
            // If we do not have a proper character, set up a fake statistics map so we always
            // have a valid map, otherwise we will crash when trying to set statistics.
            // These values will not be persisted.
            currentStatistics = new EnumMap<>(StatisticKind.class);
            return;
        }

        setCurrentStatistics(Models.Character.getId());
    }

    @SubscribeEvent
    public void onCharacterUpdated(CharacterUpdateEvent event) {
        setCurrentStatistics(Models.Character.getId());
    }

    public void increaseStatistics(StatisticKind kind) {
        addToStatistics(kind, 1);
    }

    public void addToStatistics(StatisticKind kind, int amount) {
        StatisticEntry newValue = currentStatistics.containsKey(kind)
                ? currentStatistics.get(kind).getUpdatedEntry(amount)
                : new StatisticEntry(amount, 1, amount, amount);
        currentStatistics.put(kind, newValue);
        statistics.touched();
    }

    public StatisticEntry getStatistic(StatisticKind statistic) {
        return currentStatistics.getOrDefault(statistic, StatisticEntry.EMPTY);
    }

    public void resetStatistics() {
        currentStatistics.clear();
        statistics.touched();
    }

    private void setCurrentStatistics(String id) {
        // Make sure our statistics is based on the correct character, and that it is persisted
        statistics.get().putIfAbsent(id, new EnumMap<>(StatisticKind.class));
        currentStatistics = statistics.get().get(id);
        statistics.touched();
    }
}
