/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.war.type;

import com.wynntils.utils.type.RangedValue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WarBattleInfo {
    private final String territory;
    private final String ownerGuild;

    private final List<WarTowerState> states = new ArrayList<>();

    public WarBattleInfo(String territory, String ownerGuild, WarTowerState initialState) {
        this.territory = territory;
        this.ownerGuild = ownerGuild;
        this.states.add(initialState);
    }

    public String getTerritory() {
        return territory;
    }

    public String getOwnerGuild() {
        return ownerGuild;
    }

    public WarTowerState getInitialState() {
        return states.getFirst();
    }

    public WarTowerState getCurrentState() {
        return states.getLast();
    }

    public long getTotalLengthSeconds() {
        return (long)
                Math.ceil((states.getLast().timestamp() - states.getFirst().timestamp()) / 1000d);
    }

    public long getDps(long seconds) {
        // Get the dps over the entire war
        if (seconds == Long.MAX_VALUE) {
            WarTowerState initialState = getInitialState();
            WarTowerState currentState = getCurrentState();

            return (long) Math.floor((initialState.effectiveHealth() - currentState.effectiveHealth())
                    / ((currentState.timestamp() - initialState.timestamp()) / 1000d));
        }

        // Get the dps over the last x seconds
        long now = System.currentTimeMillis();
        long start = now - TimeUnit.SECONDS.toMillis(seconds);

        WarTowerState firstRelevantState = null;
        WarTowerState lastRelevantState = null;

        for (WarTowerState state : states) {
            if (state.timestamp() >= start) {
                if (firstRelevantState == null) {
                    firstRelevantState = state;
                }

                lastRelevantState = state;
            }
        }

        return firstRelevantState == null || lastRelevantState == null
                ? 0
                : (long) Math.floor(
                        (firstRelevantState.effectiveHealth() - lastRelevantState.effectiveHealth()) / seconds);
    }

    public long getTowerEffectiveHp() {
        WarTowerState currentState = getCurrentState();
        return currentState.effectiveHealth();
    }

    public RangedValue getTowerDps() {
        WarTowerState currentState = getCurrentState();
        // Tower max DPS needs to be doubled to calculate correctly
        return RangedValue.of((int) (currentState.damage().low() * currentState.attackSpeed()), (int)
                (currentState.damage().high() * currentState.attackSpeed() * 2));
    }

    public long getEstimatedTimeRemaining() {
        WarTowerState currentState = getCurrentState();
        long effectiveHp = getTowerEffectiveHp();
        long dps = getDps(Long.MAX_VALUE);
        return dps == 0 ? -1L : effectiveHp / dps;
    }

    public void addNewState(WarTowerState towerState) {
        states.add(towerState);
    }
}
