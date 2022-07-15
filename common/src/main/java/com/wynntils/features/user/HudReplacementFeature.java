/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import com.wynntils.mc.event.ChatReceivedEvent;
import com.wynntils.wc.utils.WynnUtils;
import net.minecraft.network.chat.ChatType;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(stability = Stability.STABLE, category = "HUD")
public class HudReplacementFeature extends UserFeature {
    @Config
    public static boolean hideInfoBar = false;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChatReceived(ChatReceivedEvent e) {
        if (!WynnUtils.onWorld()) return;
        if (e.getType() != ChatType.GAME_INFO) return;

        if (hideInfoBar) {
            e.setCanceled(true);
        }
    }
}
