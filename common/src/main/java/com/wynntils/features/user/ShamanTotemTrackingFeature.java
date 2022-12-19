/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.wynntils.core.features.UserFeature;
import com.wynntils.wynn.event.SpellCastedEvent;
import com.wynntils.wynn.objects.SpellType;
import com.wynntils.wynn.utils.WynnUtils;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ShamanTotemTrackingFeature extends UserFeature {

    @SubscribeEvent
    public void onTotemCasted(SpellCastedEvent e) {
        if (!WynnUtils.onWorld() || e.getSpell() != SpellType.TOTEM) return;
    }
}
