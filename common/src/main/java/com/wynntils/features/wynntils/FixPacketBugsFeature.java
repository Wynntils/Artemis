/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.wynntils;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.AddEntityLookupEvent;
import com.wynntils.mc.event.PlayerTeamEvent;
import com.wynntils.mc.event.SetEntityPassengersEvent;
import com.wynntils.mc.event.SetPlayerTeamEvent;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.WYNNTILS)
public class FixPacketBugsFeature extends Feature {
    private static final int METHOD_ADD = 0;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSetPlayerTeamPacket(SetPlayerTeamEvent event) {
        if (McUtils.mc().level == null) return;

        // Work around bug in Wynncraft that causes a lot of NPEs in Vanilla
        if (event.getMethod() != METHOD_ADD
                && McUtils.mc().level.getScoreboard().getPlayerTeam(event.getTeamName()) == null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRemovePlayerFromTeam(PlayerTeamEvent.Removed event) {
        if (McUtils.mc().level == null) return;

        // Work around bug in Wynncraft that causes NPEs in Vanilla
        PlayerTeam playerTeamFromUserName = McUtils.mc().level.getScoreboard().getPlayersTeam(event.getUsername());
        if (playerTeamFromUserName != event.getPlayerTeam()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSetEntityPassengersPacket(SetEntityPassengersEvent event) {
        if (McUtils.mc().level == null) return;

        // Work around bug in Wynncraft that causes a lot of warnings in Vanilla
        Entity entity = McUtils.mc().level.getEntity(event.getVehicle());
        if (entity == null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAddEntityLookup(AddEntityLookupEvent event) {
        // Work around bug in Wynncraft that causes a lot of warnings in Vanilla
        if (event.getEntityMap().containsKey(event.getUUID())) {
            event.setCanceled(true);
        }
    }
}
