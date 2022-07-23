/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.players;

import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import com.wynntils.mc.event.LivingEntityRenderTranslucentCheckEvent;
import com.wynntils.wc.utils.WynnPlayerUtils;
import com.wynntils.wc.utils.WynnUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(stability = Stability.STABLE, category = "Players")
public class PlayerGhostTransparencyFeature extends UserFeature {

    @Config
    public static float playerGhostTranslucenceLevel = 0.75f;

    @SubscribeEvent
    public void onTranslucentCheck(LivingEntityRenderTranslucentCheckEvent e) {
        if (!WynnUtils.onWorld()) return;

        if (!(e.getEntity() instanceof Player player)) return;

        if (WynnPlayerUtils.isPlayerGhost(player)) {
            e.setTranslucence(playerGhostTranslucenceLevel);
        }
    }
}
