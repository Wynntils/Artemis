/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gear;

import com.google.gson.reflect.TypeToken;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.net.Download;
import com.wynntils.core.net.UrlId;
import com.wynntils.models.gear.type.SetInfo;
import com.wynntils.models.stats.type.StatType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetModel extends Model {
    // Stored as a map for quick lookup
    private final Map<String, SetInfo> setData = new HashMap<>();

    public SetModel() {
        super(List.of());
        loadSetData();
    }

    public SetInfo getSetInfoForId(String setId) {
        return setData.getOrDefault(setId, null);
    }

    public SetInfo getSetInfoForItem(String itemName) {
        return getSetInfoForId(getSetName(itemName));
    }

    /**
     * @param itemName The name of the item to check
     * @return The set name if the item is part of a set, null otherwise
     */
    public String getSetName(String itemName) {
        for (Map.Entry<String, SetInfo> entry : setData.entrySet()) {
            if (entry.getValue().items().contains(itemName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void loadSetData() {
        Download dl = Managers.Net.download(UrlId.DATA_STATIC_ITEM_SETS);
        dl.handleReader(reader -> {
            Map<String, Map<String, List<?>>> tempData =
                    WynntilsMod.GSON.fromJson(reader, new TypeToken<Map<String, Map<String, List<?>>>>() {}.getType());
            tempData.forEach((key, value) -> {
                List<Map<String, Double>> rawBonuses = (List<Map<String, Double>>) value.get("bonuses");
                List<String> items = (List<String>) value.get("items");

                List<Map<StatType, Integer>> bonuses = rawBonuses.stream()
                        .map(bonus -> {
                            Map<StatType, Integer> bonusMap = new HashMap<>();
                            bonus.forEach((statName, statValue) -> {
                                StatType statType = Models.Stat.fromApiName(statName);
                                if (statType == null) {
                                    WynntilsMod.warn("Unknown stat type: " + statName);
                                }
                                bonusMap.put(statType, statValue.intValue());
                            });
                            return bonusMap;
                        })
                        .toList();

                setData.put(key, new SetInfo(key, bonuses, items));
            });
        });
    }
}
