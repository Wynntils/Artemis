/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.embellishments;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.models.territories.event.GuildWarQueuedEvent;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.EMBELLISHMENTS)
public class WarHornFeature extends Feature {
    private static final ResourceLocation WAR_HORN_ID = ResourceLocation.fromNamespaceAndPath("wynntils", "war.horn");
    private static final SoundEvent WAR_HORN_SOUND = SoundEvent.createVariableRangeEvent(WAR_HORN_ID);

    @Persisted
    private final Config<Float> soundVolume = new Config<>(1.0f);

    @Persisted
    private final Config<Float> soundPitch = new Config<>(1.0f);

    @SubscribeEvent
    public void onWarQueued(GuildWarQueuedEvent event) {
        McUtils.playSoundAmbient(WAR_HORN_SOUND, soundVolume.get(), soundPitch.get());
    }
}
