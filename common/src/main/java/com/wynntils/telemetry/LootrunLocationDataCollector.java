/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.telemetry;

import com.wynntils.core.components.Models;
import com.wynntils.core.telemetry.CrowdSourceDataCollector;
import com.wynntils.core.telemetry.datatype.LootrunTaskLocation;
import com.wynntils.core.telemetry.type.CrowdSourceDataType;
import com.wynntils.models.lootrun.type.LootrunLocation;
import com.wynntils.models.particle.event.ParticleVerifiedEvent;
import com.wynntils.models.particle.type.ParticleType;
import com.wynntils.utils.mc.type.Location;
import java.util.Optional;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class LootrunLocationDataCollector extends CrowdSourceDataCollector<LootrunTaskLocation> {
    @SubscribeEvent
    public void onLootrunParticle(ParticleVerifiedEvent event) {
        if (event.getParticle().particleType() != ParticleType.LOOTRUN_TASK) return;

        Optional<LootrunLocation> lootrunLocationOpt = Models.Lootrun.getLocation();
        if (lootrunLocationOpt.isEmpty()) return;

        LootrunLocation lootrunLocation = lootrunLocationOpt.get();

        collect(new LootrunTaskLocation(
                lootrunLocation, Location.containing(event.getParticle().position())));
    }

    @Override
    protected CrowdSourceDataType getTelemetryType() {
        return CrowdSourceDataType.LOOTRUN_TASK_LOCATIONS;
    }
}
