/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.players;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.players.event.PartyEvent;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.PLAYERS)
public class AutoJoinPartyFeature extends Feature {
    @Persisted
    public final Config<Boolean> onlyFriends = new Config<>(true);

    @Persisted
    public final Config<Boolean> onlySameWorld = new Config<>(true);

    @SubscribeEvent
    public void onPartyInvite(PartyEvent.Invited event) {
        if (Models.Party.isInParty()) return;
        if (onlyFriends.get() && !Models.Friends.isFriend(event.getPlayerName())) return;
        if (onlySameWorld.get() && !Models.Player.isLocalPlayer(event.getPlayerName())) return;

        Managers.Notification.queueMessage(StyledText.fromString("Auto-joined " + event.getPlayerName() + "'s party"));
        McUtils.playSoundAmbient(SoundEvents.END_PORTAL_FRAME_FILL);

        Models.Party.partyJoin(event.getPlayerName());
    }
}
