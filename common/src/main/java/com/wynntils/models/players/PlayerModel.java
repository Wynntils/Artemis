/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.players;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Services;
import com.wynntils.core.net.ApiResponse;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.PlayerJoinedWorldEvent;
import com.wynntils.mc.event.PlayerTeamEvent;
import com.wynntils.models.players.type.WynnPlayerInfo;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.TimedSet;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;

public final class PlayerModel extends Model {
    private static final Gson PLAYER_GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(WynnPlayerInfo.class, new WynnPlayerInfo.WynnPlayerInfoDeserializer())
            .create();
    private static final String ATHENA_USER_NOT_FOUND = "User not found";
    private static final Pattern GHOST_WORLD_PATTERN = Pattern.compile("^_(\\d+)$");

    // If there is a failure with the API, give it time to recover
    private static final int ERROR_TIMEOUT_MINUTE = 5;

    // Max amount of overall lookup errors
    private static final int MAX_ERRORS = 5;

    // Max amount of errors for a single user,
    // before disabling lookups for them
    private static final int MAX_USER_ERRORS = 3;

    private final Map<UUID, WynntilsUser> users = new ConcurrentHashMap<>();
    private final Set<UUID> usersWithoutWynntilsAccount = ConcurrentHashMap.newKeySet();
    private final Set<UUID> fetching = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> ghosts = new ConcurrentHashMap<>();
    private final Map<UUID, String> nameMap = new ConcurrentHashMap<>();

    // The size of this set is the count of errors in the last ERROR_TIMEOUT_MINUTE minutes.
    // This is used to avoid spamming the API.
    private final TimedSet<Object> errors =
            new TimedSet<>(ERROR_TIMEOUT_MINUTE, TimeUnit.MINUTES, true, ConcurrentHashMap::newKeySet);
    private final Map<UUID, Integer> userFailures = new ConcurrentHashMap<>();

    public PlayerModel() {
        super(List.of());
        errors.clear();
        userFailures.clear();
    }

    // Returns true if the player is on the same server and is not a npc
    public boolean isLocalPlayer(Player player) {
        return !isNpc(player) && !isPlayerGhost(player);
    }

    public boolean isLocalPlayer(String name) {
        // Wynn uses TeamNames for player names that are online
        return !isNpcName(StyledText.fromString(name))
                && McUtils.mc().level.getScoreboard().getTeamNames().contains(name);
    }

    public boolean isNpc(Player player) {
        StyledText scoreboardName = StyledText.fromString(player.getScoreboardName());
        return isNpcName(scoreboardName) || isNpcUuid(player.getUUID());
    }

    public boolean isPlayerGhost(Player player) {
        return ghosts.containsKey(player.getUUID());
    }

    public WynntilsUser getUser(UUID uuid) {
        return users.getOrDefault(uuid, null);
    }

    public Stream<String> getAllPlayerNames() {
        return nameMap.values().stream();
    }

    public void reset() {
        fetching.clear();
        errors.clear();
        userFailures.clear();
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldState.NOT_CONNECTED) {
            clearNameMap();
            reset();
        }
        if (event.getNewState() == WorldState.WORLD) {
            clearGhostCache();
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerJoinedWorldEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.getUUID() == null) return;
        StyledText name = StyledText.fromString(player.getGameProfile().getName());
        if (!isLocalPlayer(player)) return; // avoid player npcs

        loadUser(player.getUUID(), name.getString());
    }

    @SubscribeEvent
    public void onAddPlayerToTeam(PlayerTeamEvent.Added event) {
        PlayerInfo playerInfo = McUtils.mc().getConnection().getPlayerInfo(event.getUsername());
        if (playerInfo == null) return;

        UUID uuid = playerInfo.getProfile().getId();
        if (uuid == null) return;

        PlayerTeam playerTeam = event.getPlayerTeam();

        Matcher matcher = GHOST_WORLD_PATTERN.matcher(playerTeam.getName());
        if (!matcher.matches()) {
            // Maybe it should not be a ghost anymore
            ghosts.remove(uuid);
            return;
        }

        int world = Integer.parseInt(matcher.group(1));
        ghosts.put(uuid, world);
    }

    private void loadUser(UUID uuid, String userName) {
        // Avoid fetching the same user multiple times
        if (fetching.contains(uuid)) return;
        if (users.containsKey(uuid) || usersWithoutWynntilsAccount.contains(uuid)) return;

        // Call getEntries to clear old entries
        if (errors.getEntries().size() >= MAX_ERRORS) {
            // Athena is having problems, skip this
            return;
        }

        if (userFailures.getOrDefault(uuid, 0) >= MAX_USER_ERRORS) {
            // User has had too many failures, skip this
            return;
        }

        fetching.add(uuid); // temporary, avoid extra loads
        nameMap.put(uuid, userName);

        ApiResponse apiResponse =
                Services.WynntilsAccount.callApi(UrlId.API_ATHENA_USER_INFO, Map.of("uuid", uuid.toString()));
        apiResponse.handleJsonObject(
                json -> {
                    if (json.has("message") && json.get("message").getAsString().equals(ATHENA_USER_NOT_FOUND)) {
                        // This user does not exist in our database, stop requesting it
                        usersWithoutWynntilsAccount.add(uuid);
                        fetching.remove(uuid);
                        return;
                    }

                    if (!json.has("user")) {
                        fetching.remove(uuid);
                        saveUserFailures(uuid, userName);
                        return;
                    }

                    WynntilsUser user = WynntilsMod.GSON.fromJson(json.getAsJsonObject("user"), WynntilsUser.class);

                    users.put(uuid, user);
                    fetching.remove(uuid);

                    // Schedule cape loading for next render tick
                    RenderSystem.recordRenderCall(() -> Services.Cosmetics.loadCosmeticTextures(uuid, user));
                },
                onError -> {
                    errors.put(System.currentTimeMillis());

                    saveUserFailures(uuid, userName);
                });
    }

    public CompletableFuture<WynnPlayerInfo> getPlayer(String username) {
        CompletableFuture<WynnPlayerInfo> future = new CompletableFuture<>();

        ApiResponse apiResponse = Managers.Net.callApi(UrlId.DATA_WYNNCRAFT_PLAYER, Map.of("username", username));
        apiResponse.handleJsonObject(
                json -> {
                    Type type = new TypeToken<WynnPlayerInfo>() {}.getType();

                    future.complete(PLAYER_GSON.fromJson(json, type));
                },
                onError -> future.complete(null));

        return future;
    }

    private void saveUserFailures(UUID uuid, String userName) {
        userFailures.putIfAbsent(uuid, 0);
        userFailures.compute(uuid, (k, v) -> v + 1);

        // Only log the error once
        // Call getEntries to clear old entries
        if (errors.getEntries().size() == MAX_ERRORS) {
            WynntilsMod.error("Athena user lookup has repeating failures. Disabling future lookups temporarily.");
        }

        // Only log the error once
        if (userFailures.get(uuid) == MAX_USER_ERRORS) {
            WynntilsMod.error("Athena user lookup has repeating failures for user " + userName
                    + ". Disabling future lookups for the user, until a reset.");
        }
    }

    private void clearNameMap() {
        nameMap.clear();
    }

    private void clearGhostCache() {
        ghosts.clear();
    }

    private boolean isNpcName(StyledText name) {
        // FIXME: Maybe make a better check using more native StyledText operations?
        return name.contains("\u0001") || name.contains("§");
    }

    private boolean isNpcUuid(UUID uuid) {
        // All players have UUID version 4,
        // while most NPCs have UUID version 2
        // Starting Wynncraft 2.1, all NPCs will have UUID version 2
        return uuid.version() == 2;
    }
}
