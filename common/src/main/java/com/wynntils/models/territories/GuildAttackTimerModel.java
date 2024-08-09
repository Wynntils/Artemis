/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.territories;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.scoreboard.ScoreboardPart;
import com.wynntils.handlers.scoreboard.ScoreboardSegment;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.marker.MarkerModel;
import com.wynntils.models.territories.event.GuildWarQueuedEvent;
import com.wynntils.models.territories.markers.GuildAttackMarkerProvider;
import com.wynntils.models.territories.profile.TerritoryProfile;
import com.wynntils.models.territories.type.GuildResourceValues;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.StyledTextUtils;
import com.wynntils.utils.type.TimedSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * A note about territory attack timers:
 * - Chat messages are sent on attack, and on certain time intervals
 * - Scoreboard is always up to date, but can cut off vertically and horizontally
 * (this means it can cut off long territory names and full lines, if there are too many attacks)
 * <p>
 * We use a hybrid approach:
 * - Use chat messages when we can, as they are more reliable
 * - Use scoreboard messages when we don't have a chat message
 * - If a chat message is received for a territory, we stop using the scoreboard for that territory
 * - We also take territory defenses from chat messages
 */
public final class GuildAttackTimerModel extends Model {
    private static final Pattern GUILD_ATTACK_PATTERN = Pattern.compile("§b- (.+):(.+) §3(.+)");
    private static final Pattern GUILD_DEFENSE_CHAT_PATTERN = Pattern.compile("§3.+§b (.+) defense is (.+)");
    // Test in GuildAttackTimerModel_WAR_MESSAGE_PATTERN
    private static final Pattern WAR_MESSAGE_PATTERN = Pattern.compile(
            "§c\uE006\uE002 The war for (?<territory>.+) will start in ((?<minutes>\\d+) minute(?:s)?)?(?: and )?((?<seconds>\\d+) second(?:s)?)?\\.");
    private static final Pattern CAPTURED_PATTERN =
            Pattern.compile("§3\\[WAR\\]§c \\[(?<guild>.+)\\] (?:has )?captured the territory (?<territory>.+)\\.");
    private static final ScoreboardPart GUILD_ATTACK_SCOREBOARD_PART = new GuildAttackScoreboardPart();

    private static final GuildAttackMarkerProvider GUILD_ATTACK_MARKER_PROVIDER = new GuildAttackMarkerProvider();

    private final Map<String, GuildResourceValues> territoryDefenses = new HashMap<>();
    private final Map<String, TerritoryAttackTimer> chatAttackTimers = new HashMap<>();
    private final Map<String, TerritoryAttackTimer> scoreboardAttackTimers = new HashMap<>();
    private final TimedSet<String> capturedTerritories = new TimedSet<>(10, TimeUnit.SECONDS, true);

    public GuildAttackTimerModel(MarkerModel marker) {
        super(List.of(marker));

        Handlers.Scoreboard.addPart(GUILD_ATTACK_SCOREBOARD_PART);
        Models.Marker.registerMarkerProvider(GUILD_ATTACK_MARKER_PROVIDER);
    }

    @SubscribeEvent
    public void onMessage(ChatMessageReceivedEvent event) {
        // TODO: Once RecipientType supports Wynncraft 2.1 messages, we can check for RecipientType.GUILD

        String cleanMessage = StyledTextUtils.joinAllLines(event.getStyledText().stripAlignment())
                .getString()
                .replaceAll("\uE001 ", "");
        Matcher matcher = WAR_MESSAGE_PATTERN.matcher(cleanMessage);
        if (matcher.matches()) {
            long timerEnd = System.currentTimeMillis();

            if (matcher.group("minutes") != null) {
                timerEnd += Long.parseLong(matcher.group("minutes")) * 60 * 1000;
            }
            if (matcher.group("seconds") != null) {
                timerEnd += Long.parseLong(matcher.group("seconds")) * 1000;
            }

            String territory = matcher.group("territory");
            TerritoryAttackTimer scoreboardTimer = scoreboardAttackTimers.remove(territory);

            TerritoryAttackTimer attackTimer = new TerritoryAttackTimer(territory, timerEnd);
            TerritoryAttackTimer oldTimer = chatAttackTimers.put(territory, attackTimer);

            // If we didn't have a timer before, post an event
            if (oldTimer == null && scoreboardTimer == null) {
                WynntilsMod.postEvent(new GuildWarQueuedEvent(attackTimer));
            }

            return;
        }

        matcher = event.getOriginalStyledText().getMatcher(CAPTURED_PATTERN);
        if (matcher.matches()) {
            // Remove the attack timer for the territory, if it exists
            // (the captured message appears for both owned and attacked territories)
            String territory = matcher.group("territory");

            chatAttackTimers.remove(territory);
            scoreboardAttackTimers.remove(territory);
            capturedTerritories.put(territory);
            return;
        }

        matcher = event.getOriginalStyledText().getMatcher(GUILD_DEFENSE_CHAT_PATTERN);
        if (matcher.matches()) {
            String territory = matcher.group(1);
            territoryDefenses.put(territory, GuildResourceValues.fromString(matcher.group(2)));
            return;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        // Every 20 ticks, clean up old timers
        if (McUtils.player().tickCount % 20 != 0) return;

        long currentTime = System.currentTimeMillis();

        List<String> removedTimers = new ArrayList<>();
        for (Map.Entry<String, TerritoryAttackTimer> entry : chatAttackTimers.entrySet()) {
            if (entry.getValue().timerEnd() < currentTime) {
                removedTimers.add(entry.getKey());
            }
        }

        removedTimers.forEach(chatAttackTimers::remove);

        removedTimers.clear();

        for (Map.Entry<String, TerritoryAttackTimer> entry : scoreboardAttackTimers.entrySet()) {
            if (entry.getValue().timerEnd() < currentTime) {
                removedTimers.add(entry.getKey());
            }
        }

        removedTimers.forEach(scoreboardAttackTimers::remove);
    }

    public List<TerritoryAttackTimer> getAttackTimers() {
        return getUpcomingTimers().toList();
    }

    public Optional<TerritoryAttackTimer> getAttackTimerForTerritory(String territory) {
        return getUpcomingTimers()
                .filter(t -> t.territoryName().equals(territory))
                .findFirst();
    }

    public Stream<TerritoryAttackTimer> getUpcomingTimers() {
        return Stream.concat(chatAttackTimers.values().stream(), scoreboardAttackTimers.values().stream())
                .filter(t -> t.timerEnd() > System.currentTimeMillis());
    }

    public Optional<GuildResourceValues> getDefenseForTerritory(String territory) {
        return Optional.ofNullable(territoryDefenses.get(territory));
    }

    void processScoreboardChanges(ScoreboardSegment segment) {
        Set<String> usedTerritories = new HashSet<>();

        for (StyledText line : segment.getContent()) {
            Matcher matcher = line.getMatcher(GUILD_ATTACK_PATTERN);

            if (matcher.matches()) {
                String shortTerritoryName = matcher.group(3);

                // Scoreboard cuts off long territory names, so we need to "guess" the full name
                // There could be multiple matches, so we need to sort by the timer difference, the closest one is the
                // correct one
                Optional<TerritoryAttackTimer> chatTimerOpt = chatAttackTimers.values().stream()
                        .filter(timer -> timer.territoryName().startsWith(shortTerritoryName))
                        .min((a, b) -> (int) (a.timerEnd() - b.timerEnd()));

                // If we found a chat timer, use it
                if (chatTimerOpt.isPresent()) {
                    // Don't put a new timer in for scoreboard if we already have one from chat
                    usedTerritories.add(chatTimerOpt.get().territoryName());
                    continue;
                }

                // Only use the scoreboard timer if we didn't find a chat timer
                // (eg. we joined the server after the war was announced)
                TerritoryProfile territoryProfile =
                        Models.Territory.getTerritoryProfileFromShortName(shortTerritoryName, usedTerritories);

                if (territoryProfile == null) {
                    if (usedTerritories.stream().noneMatch(ex -> ex.equals(shortTerritoryName))) {
                        WynntilsMod.warn(
                                "Received scoreboard attack timer for unknown territory: " + shortTerritoryName);
                    }

                    continue;
                }

                String fullTerritoryName = territoryProfile.getFriendlyName();

                // Don't put a new timer in for scoreboard if it was captured recently
                if (capturedTerritories.stream().anyMatch(t -> Objects.equals(t, fullTerritoryName))) continue;

                usedTerritories.add(fullTerritoryName);

                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));

                long timerEnd = (minutes * 60L + seconds) * 1000 + System.currentTimeMillis();

                TerritoryAttackTimer timer = new TerritoryAttackTimer(fullTerritoryName, timerEnd);
                TerritoryAttackTimer oldTimer = scoreboardAttackTimers.put(fullTerritoryName, timer);

                // If we didn't have a timer before, post an event
                if (oldTimer == null) {
                    WynntilsMod.postEvent(new GuildWarQueuedEvent(timer));
                }
            }
        }
    }
}
