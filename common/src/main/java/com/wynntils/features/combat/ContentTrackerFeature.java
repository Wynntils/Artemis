/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.models.activities.event.ActivityTrackerUpdatedEvent;
import com.wynntils.models.activities.type.ActivityType;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.COMBAT)
public class ContentTrackerFeature extends Feature {
    private static final ResourceLocation TRACKER_UPDATE_ID =
            ResourceLocation.fromNamespaceAndPath("wynntils", "ui.tracker.update");
    private static final SoundEvent TRACKER_UPDATE_SOUND = SoundEvent.createVariableRangeEvent(TRACKER_UPDATE_ID);

    @Persisted
    public final Config<Boolean> autoTrackCoordinates = new Config<>(true);

    @Persisted
    public final Config<Boolean> playSoundOnUpdate = new Config<>(true);

    @SubscribeEvent
    public void onTrackerUpdate(ActivityTrackerUpdatedEvent event) {
        // Don't play sounds for world events as the tracker needs to be updated to match the countdown
        // but each time it changes this is called, causing the sound to be repeated every minute until the final
        // 60 seconds when it is repeated every second until the event begins.
        if (event.getType() == ActivityType.WORLD_EVENT) {
            return;
        }

        if (playSoundOnUpdate.get()) {
            McUtils.playSoundUI(TRACKER_UPDATE_SOUND);
        }
    }
}
