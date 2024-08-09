/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.chat;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.KeyInputEvent;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.CHAT)
public class DialogueOptionOverrideFeature extends Feature {
    @SubscribeEvent
    public void onDialogueKeyPress(KeyInputEvent e) {
        if (!Models.WorldState.onWorld() || e.getAction() != 1) return; // Only send packet on presses, not releases
        LocalPlayer player = McUtils.player();
        if (player == null) return;

        if (e.getKey() == player.getInventory().selected + 49) { // keys 1-9 are +49 offset from hotbar
            McUtils.sendPacket(new ServerboundSetCarriedItemPacket(e.getKey() - 49));
        }
    }
}
