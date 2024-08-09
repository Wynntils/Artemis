/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.war;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Model;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.bossbar.TrackedBar;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.models.war.bossbar.WarTowerBar;
import com.wynntils.models.war.event.GuildWarEvent;
import com.wynntils.models.war.event.GuildWarTowerEffectEvent;
import com.wynntils.models.war.type.WarBattleInfo;
import com.wynntils.models.war.type.WarTowerState;
import com.wynntils.models.worlds.event.WorldStateEvent;
import java.util.List;
import java.util.Optional;
import net.neoforged.bus.api.SubscribeEvent;

public class GuildWarTowerModel extends Model {
    private static final int EFFECT_PROC_MS = 3200;
    private static final StyledText AURA_TITLE = StyledText.fromString("§4§n/!\\§7 Tower §6Aura");
    private static final StyledText VOLLEY_TITLE = StyledText.fromString("§4§n/!\\§7 Tower §dVolley");

    private final TrackedBar WarTowerBar = new WarTowerBar();

    public GuildWarTowerModel() {
        super(List.of());

        Handlers.BossBar.registerBar(WarTowerBar);
    }

    private WarBattleInfo warBattleInfo = null;

    private long lastAuraProc = 0;
    private long lastVolleyProc = 0;

    @SubscribeEvent
    public void onSubtitle(SubtitleSetTextEvent event) {
        StyledText styledText = StyledText.fromComponent(event.getComponent());

        if (styledText.equals(AURA_TITLE)) {
            lastAuraProc = System.currentTimeMillis();
            WynntilsMod.postEvent(new GuildWarTowerEffectEvent.AuraSpawned());
            return;
        }

        if (styledText.equals(VOLLEY_TITLE)) {
            lastVolleyProc = System.currentTimeMillis();
            WynntilsMod.postEvent(new GuildWarTowerEffectEvent.VolleySpawned());
        }
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent evente) {
        resetTowerState();
    }

    public long getLastAuraProc() {
        return lastAuraProc;
    }

    public long getLastVolleyProc() {
        return lastVolleyProc;
    }

    public int getEffectLength() {
        return EFFECT_PROC_MS;
    }

    public long getRemainingTimeUntilAura() {
        return Math.max(-1, getEffectLength() - (System.currentTimeMillis() - getLastAuraProc()));
    }

    public long getRemainingTimeUntilVolley() {
        return Math.max(-1, getEffectLength() - (System.currentTimeMillis() - getLastVolleyProc()));
    }

    public Optional<WarBattleInfo> getWarBattleInfo() {
        return Optional.ofNullable(warBattleInfo);
    }

    public void onTowerDamaged(String guild, String territory, WarTowerState towerState) {
        // If the previous territory is different from the current territory, reset the tower state
        // This should not happen, but it's a safety measure
        if (warBattleInfo != null && !warBattleInfo.getTerritory().equals(territory)) {
            WynntilsMod.warn("War tower territory was not reset correctly, warBattleInfo has %s, war bar has %s"
                    .formatted(warBattleInfo.getTerritory(), territory));
            resetTowerState();
        }

        if (warBattleInfo == null) {
            warBattleInfo = new WarBattleInfo(territory, guild, towerState);
            WynntilsMod.postEvent(new GuildWarEvent.Started(warBattleInfo));
        } else {
            warBattleInfo.addNewState(towerState);
        }
    }

    public void resetTowerState() {
        if (warBattleInfo == null) return;

        WarBattleInfo oldBattleInfo = warBattleInfo;

        warBattleInfo = null;

        WynntilsMod.postEvent(new GuildWarEvent.Ended(oldBattleInfo));
    }
}
