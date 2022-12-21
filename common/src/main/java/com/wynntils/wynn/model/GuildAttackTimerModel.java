/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model;

import com.wynntils.core.managers.Model;
import com.wynntils.handlers.chat.RecipientType;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.scoreboard.ScoreboardListener;
import com.wynntils.handlers.scoreboard.Segment;
import com.wynntils.utils.Pair;
import com.wynntils.utils.objects.TimedSet;
import com.wynntils.wynn.model.scoreboard.guild.GuildAttackListener;
import com.wynntils.wynn.model.scoreboard.guild.TerritoryAttackTimer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class GuildAttackTimerModel extends Model {
    private static final Pattern GUILD_ATTACK_PATTERN = Pattern.compile("§b- (.+):(.+) §3(.+)");
    private static final Pattern GUILD_DEFENSE_CHAT_PATTERN = Pattern.compile("§r§3.+§b (.+) defense is (.+)");

    public static final ScoreboardListener SCOREBOARD_HANDLER = new GuildAttackListener();
    private final TimedSet<Pair<String, String>> territoryDefenseSet = new TimedSet<>(5, TimeUnit.SECONDS, true);

    private List<TerritoryAttackTimer> attackTimers = List.of();

    @SubscribeEvent
    public void onMessage(ChatMessageReceivedEvent event) {
        if (event.getRecipientType() != RecipientType.GUILD) return;

        Matcher matcher = GUILD_DEFENSE_CHAT_PATTERN.matcher(event.getOriginalCodedMessage());
        if (!matcher.matches()) return;

        Optional<TerritoryAttackTimer> territory = attackTimers.stream()
                .filter(territoryAttackTimer -> territoryAttackTimer.territory().equals(matcher.group(1))
                        && !territoryAttackTimer.isDefenseKnown())
                .findFirst();

        if (territory.isPresent()) {
            territory.get().setDefense(matcher.group(2));
        } else {
            for (Pair<String, String> defensePair : territoryDefenseSet) {
                if (defensePair.a().equals(matcher.group(1))) {
                    return; // do not put it in the set twice
                }
            }

            territoryDefenseSet.put(new Pair<>(matcher.group(1), matcher.group(2)));
        }
    }

    public void processChanges(Segment segment) {
        List<TerritoryAttackTimer> newList = new ArrayList<>();

        for (String line : segment.getContent()) {
            Matcher matcher = GUILD_ATTACK_PATTERN.matcher(line);

            if (matcher.matches()) {
                TerritoryAttackTimer timer = new TerritoryAttackTimer(
                        matcher.group(3), Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
                newList.add(timer);

                boolean foundDefense = false;
                Optional<TerritoryAttackTimer> oldTimer = attackTimers.stream()
                        .filter(territoryAttackTimer ->
                                territoryAttackTimer.territory().equals(timer.territory()))
                        .findFirst();

                if (oldTimer.isPresent()) {
                    if (oldTimer.get().isDefenseKnown()) {
                        timer.setDefense(oldTimer.get().defense());
                        foundDefense = true;
                    }
                }

                if (!foundDefense) {
                    for (Pair<String, String> defensePair : territoryDefenseSet) {
                        if (defensePair.a().equals(timer.territory())) {
                            timer.setDefense(defensePair.b());
                            break;
                        }
                    }
                }
            }
        }

        attackTimers = newList;
    }

    public void resetTimers() {
        attackTimers = List.of();
    }

    public List<TerritoryAttackTimer> getAttackTimers() {
        return attackTimers;
    }

    public Optional<TerritoryAttackTimer> getAttackTimerForTerritory(String territory) {
        return attackTimers.stream()
                .filter(t -> t.territory().equals(territory))
                .findFirst();
    }
}
