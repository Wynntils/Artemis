/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.worlds;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.net.Download;
import com.wynntils.core.net.UrlId;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.profile.ServerProfile;
import com.wynntils.models.worlds.type.WorldState;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.neoforged.bus.api.SubscribeEvent;

public final class ServerListModel extends Model {
    private static final int SERVER_UPDATE_MS = 15000;

    private static final List<String> SERVER_TYPES = List.of("WC", "lobby", "GM", "DEV", "WAR", "HB", "YT");

    private final ScheduledExecutorService timerExecutor = new ScheduledThreadPoolExecutor(1);

    private Map<String, ServerProfile> availableServers = new HashMap<>();

    public ServerListModel() {
        super(List.of());

        timerExecutor.scheduleWithFixedDelay(this::updateServerList, 0, SERVER_UPDATE_MS, TimeUnit.MILLISECONDS);
    }

    public List<String> getWynnServerTypes() {
        return SERVER_TYPES;
    }

    public Set<String> getServers() {
        return availableServers.keySet();
    }

    public String getNewestServer() {
        return availableServers.entrySet().stream()
                .max(Comparator.comparingLong(entry -> entry.getValue().getFirstSeen()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public List<String> getServersSortedOnUptime() {
        return availableServers.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> -entry.getValue().getFirstSeen()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public List<String> getServersSortedOnNameOfType(String serverType) {
        return getServers().stream()
                .filter(server -> server.startsWith(serverType))
                .sorted((o1, o2) -> {
                    int number1 = Integer.parseInt(o1.substring(serverType.length()));
                    int number2 = Integer.parseInt(o2.substring(serverType.length()));

                    return number1 - number2;
                })
                .toList();
    }

    public ServerProfile getServer(String worldId) {
        return availableServers.get(worldId);
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() != WorldState.HUB && event.getNewState() != WorldState.CONNECTING) return;

        updateServerList();
    }

    private CompletableFuture<Boolean> updateServerList() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // dataAthenaServerList is based on
        // https://api.wynncraft.com/public_api.php?action=onlinePlayers
        // but injects a firstSeen timestamp when the server was first noticed by Athena

        Download dl = Managers.Net.download(UrlId.DATA_ATHENA_SERVER_LIST);
        dl.handleJsonObject(json -> {
            JsonObject servers = json.getAsJsonObject("servers");
            Map<String, ServerProfile> newMap = new HashMap<>();

            long serverTime = dl.getResponseTimestamp();
            for (Map.Entry<String, JsonElement> entry : servers.entrySet()) {
                JsonElement serverElement = entry.getValue();

                if (!serverElement.isJsonObject()) {
                    WynntilsMod.warn("Server element is not a JsonObject: " + serverElement);
                    continue;
                }

                // Inject the server name into the server profile
                serverElement.getAsJsonObject().addProperty("serverName", entry.getKey());

                ServerProfile profile = WynntilsMod.GSON.fromJson(entry.getValue(), ServerProfile.class);
                profile.matchTime(serverTime);

                newMap.put(entry.getKey(), profile);
            }

            availableServers = newMap;
            future.complete(true);
        });
        return future;
    }
}
