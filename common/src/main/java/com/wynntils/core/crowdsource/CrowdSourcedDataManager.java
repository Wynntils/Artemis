/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.crowdsource;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.crowdsource.type.CrowdSourcedDataGameVersion;
import com.wynntils.core.crowdsource.type.CrowdSourcedDataType;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.crowdsource.LootrunLocationDataCollector;
import com.wynntils.crowdsource.NpcLocationDataCollector;
import com.wynntils.crowdsource.ProfessionNodeLocationDataCollector;
import com.wynntils.crowdsource.ProfessionStationLocationDataCollector;
import com.wynntils.features.wynntils.DataCrowdSourcingFeature;
import com.wynntils.utils.type.ConfirmedBoolean;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CrowdSourcedDataManager extends Manager {
    public static final CrowdSourcedDataGameVersion CURRENT_GAME_VERSION =
            CrowdSourcedDataGameVersion.VERSION_210_BETA_2;

    @Persisted
    private final Storage<CrowdSourcedData> collectedData = new Storage<>(new CrowdSourcedData());

    private final Map<CrowdSourcedDataType, CrowdSourcedDataCollector<?>> collectors = new HashMap<>();

    public CrowdSourcedDataManager() {
        super(List.of());

        registerCollectors();
    }

    public <T> void putData(CrowdSourcedDataType crowdSourcedDataType, T crowdSourcedData) {
        if (getDataCollectionState(crowdSourcedDataType) != ConfirmedBoolean.TRUE) return;

        collectedData.get().putData(CURRENT_GAME_VERSION, crowdSourcedDataType, crowdSourcedData);
        collectedData.touched();
    }

    public <T> Set<T> getData(CrowdSourcedDataType crowdSourcedDataType) {
        return (Set<T>) collectedData
                .get()
                .getData(CURRENT_GAME_VERSION, crowdSourcedDataType, crowdSourcedDataType.getDataClass());
    }

    public ConfirmedBoolean getDataCollectionState(CrowdSourcedDataType crowdSourcedDataType) {
        if (!isDataCollectionEnabled()) return ConfirmedBoolean.FALSE;

        ConfirmedBoolean collectionEnabledForType = Managers.Feature.getFeatureInstance(DataCrowdSourcingFeature.class)
                .crowdSourcedDataTypeEnabledMap
                .get()
                .getOrDefault(crowdSourcedDataType, ConfirmedBoolean.UNCONFIRMED);

        return collectionEnabledForType;
    }

    public boolean isDataCollectionEnabled() {
        return Managers.Feature.getFeatureInstance(DataCrowdSourcingFeature.class)
                .isEnabled();
    }

    private void registerCollectors() {
        registerCollector(CrowdSourcedDataType.LOOTRUN_TASK_LOCATIONS, new LootrunLocationDataCollector());
        registerCollector(CrowdSourcedDataType.NPC_LOCATIONS, new NpcLocationDataCollector());
        registerCollector(CrowdSourcedDataType.PROFESSION_NODE_LOCATIONS, new ProfessionNodeLocationDataCollector());
        registerCollector(
                CrowdSourcedDataType.PROFESSION_CRAFTING_STATION_LOCATIONS,
                new ProfessionStationLocationDataCollector());
    }

    private void registerCollector(CrowdSourcedDataType crowdSourcedDataType, CrowdSourcedDataCollector<?> collector) {
        Class<?> collectorTypeClass = (Class<?>)
                ((ParameterizedType) collector.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        if (!collectorTypeClass.equals(crowdSourcedDataType.getDataClass())) {
            throw new IllegalStateException(
                    "The provided collector does not collect the provided crowd sourced data type.");
        }

        WynntilsMod.registerEventListener(collector);
        collectors.put(crowdSourcedDataType, collector);
    }
}
