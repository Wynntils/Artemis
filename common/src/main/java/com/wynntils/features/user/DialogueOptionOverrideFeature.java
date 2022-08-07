/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import com.wynntils.mc.event.KeyInputEvent;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wc.utils.WynnUtils;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(stability = Stability.STABLE)
public class DialogueOptionOverrideFeature extends UserFeature {
    @SubscribeEvent
    public void onDialogueKeyPress(KeyInputEvent e) {
        if (!WynnUtils.onWorld() || e.getAction() != 1) return; // Only send packet on presses, not releases

        if (e.getKey() - 49 == McUtils.player().getInventory().selected) { // keys 1-9 are +49 offset from hotbar
            McUtils.sendPacket(new ServerboundSetCarriedItemPacket(e.getKey() - 49));
        }
    }
}
