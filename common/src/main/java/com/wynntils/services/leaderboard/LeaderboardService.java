/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.leaderboard;

import com.google.gson.JsonElement;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Service;
import com.wynntils.core.net.Download;
import com.wynntils.core.net.UrlId;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.services.leaderboard.type.LeaderboardBadge;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.neoforged.bus.api.SubscribeEvent;

public class LeaderboardService extends Service {
    private Map<UUID, List<LeaderboardBadge>> leaderboard = new HashMap<>();

    public LeaderboardService() {
        super(List.of());

        reloadData();
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() != WorldState.HUB && event.getNewState() != WorldState.CONNECTING) return;

        updateLeaderboard();
    }

    @Override
    public void reloadData() {
        updateLeaderboard();
    }

    public List<LeaderboardBadge> getBadges(UUID id) {
        return leaderboard.getOrDefault(id, List.of());
    }

    private void updateLeaderboard() {
        Download dl = Managers.Net.download(UrlId.DATA_ATHENA_LEADERBOARD);
        dl.handleJsonObject(json -> {
            Map<UUID, List<LeaderboardBadge>> map = new HashMap<>();

            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                UUID id = UUID.fromString(entry.getKey());
                List<LeaderboardBadge> list = new ArrayList<>();

                Set<Map.Entry<String, JsonElement>> ranks = entry.getValue()
                        .getAsJsonObject()
                        .get("ranks")
                        .getAsJsonObject()
                        .entrySet();

                for (Map.Entry<String, JsonElement> rank : ranks) {
                    list.add(
                            LeaderboardBadge.from(rank.getKey(), rank.getValue().getAsInt()));
                }

                map.put(id, list);
            }

            leaderboard = map;
        });
    }
}
