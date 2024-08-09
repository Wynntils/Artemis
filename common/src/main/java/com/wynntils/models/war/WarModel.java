/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.war;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Services;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.handlers.scoreboard.ScoreboardPart;
import com.wynntils.models.war.event.GuildWarEvent;
import com.wynntils.models.war.scoreboard.WarScoreboardPart;
import com.wynntils.models.war.type.HistoricWarInfo;
import com.wynntils.models.war.type.WarBattleInfo;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.services.hades.HadesUser;
import com.wynntils.utils.mc.McUtils;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;

public class WarModel extends Model {
    private static final int WAR_RADIUS = 100;
    private static final ScoreboardPart WAR_SCOREBOARD_PART =
            new WarScoreboardPart(); // This is basically a party scoreboard part, but for war members

    @Persisted
    public final Storage<List<HistoricWarInfo>> historicWars = new Storage<>(new ArrayList<>());

    private List<HadesUser> hadesUsers = new ArrayList<>();
    private boolean warActive = false;

    public WarModel() {
        super(List.of());

        Handlers.Scoreboard.addPart(WAR_SCOREBOARD_PART);
    }

    @SubscribeEvent
    public void onWarEnd(GuildWarEvent.Ended event) {
        WarBattleInfo warBattleInfo = event.getWarBattleInfo();

        historicWars
                .get()
                .add(new HistoricWarInfo(
                        warBattleInfo.getTerritory(),
                        warBattleInfo.getOwnerGuild(),
                        warBattleInfo.getInitialState(),
                        warBattleInfo.getCurrentState(),
                        System.currentTimeMillis()));
        historicWars.touched();
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() != WorldState.WORLD) {
            onWarEnd();
        }
    }

    public void onWarStart() {
        hadesUsers = Services.Hades.getHadesUsers()
                .filter(hadesUser -> hadesUser
                        .getMapLocation()
                        .asLocation()
                        .toVec3()
                        .closerThan(McUtils.player().position(), WAR_RADIUS))
                .toList();
        warActive = true;
    }

    public void onWarEnd() {
        hadesUsers = new ArrayList<>();
        warActive = false;
    }

    public List<HadesUser> getHadesUsers() {
        return hadesUsers;
    }

    public boolean isWarActive() {
        return warActive;
    }
}
