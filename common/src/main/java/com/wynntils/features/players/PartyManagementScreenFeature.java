/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.players;

import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.ScreenClosedEvent;
import com.wynntils.models.players.event.FriendsEvent;
import com.wynntils.models.players.event.PartyEvent;
import com.wynntils.screens.partymanagement.PartyManagementScreen;
import com.wynntils.utils.mc.McUtils;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.PLAYERS)
public class PartyManagementScreenFeature extends Feature {
    private PartyManagementScreen partyManagementScreen;

    @RegisterKeyBind
    private final KeyBind openPartyManagementScreen =
            new KeyBind("Open Party Management Screen", GLFW.GLFW_KEY_O, true, () -> {
                partyManagementScreen = (PartyManagementScreen) PartyManagementScreen.create();
                McUtils.mc().setScreen(partyManagementScreen);
            });

    @SubscribeEvent
    public void onScreenClose(ScreenClosedEvent e) {
        partyManagementScreen = null;
    }

    // region Party events
    @SubscribeEvent
    public void onPartyList(PartyEvent.Listed e) {
        reloadScreenWidgets();
    }

    @SubscribeEvent
    public void onPartyMemberJoin(PartyEvent.OtherJoined e) {
        reloadScreenWidgets();
    }

    @SubscribeEvent
    public void onPartyMemberLeave(PartyEvent.OtherLeft e) {
        reloadScreenWidgets();
    }

    @SubscribeEvent
    public void onPartyMemberDisconnect(PartyEvent.OtherDisconnected e) {
        reloadScreenWidgets();
    }

    @SubscribeEvent
    public void onPartyMemberReconnect(PartyEvent.OtherReconnected e) {
        reloadScreenWidgets();
    }

    @SubscribeEvent
    public void onPartyPriorityChanged(PartyEvent.PriorityChanged e) {
        reloadScreenWidgets();
    }

    // endregion

    // region Friend events
    // SubscribeEvents for Friends being Added and Removed are not required because it is impossible to do any
    // of those actions when the Party Management Screen is open.

    @SubscribeEvent
    public void onFriendJoin(FriendsEvent.Joined e) {
        reloadScreenWidgets();
    }

    @SubscribeEvent
    public void onFriendLeave(FriendsEvent.Left e) {
        Managers.TickScheduler.scheduleLater(this::reloadScreenWidgets, 3);
    }

    // The events for the lists are required as it is possible that there is a delay from Wynn which causes the screen
    // to init before the new lists are received.
    @SubscribeEvent
    public void onFriendsList(FriendsEvent.Listed e) {
        Managers.TickScheduler.scheduleLater(this::reloadScreenWidgets, 2);
    }

    @SubscribeEvent
    public void onOnlineFriendsList(FriendsEvent.OnlineListed e) {
        Managers.TickScheduler.scheduleLater(this::reloadScreenWidgets, 2);
    }
    // endregion

    private void reloadScreenWidgets() {
        if (partyManagementScreen == null) return;

        partyManagementScreen.reloadCreateLeaveButton();
        partyManagementScreen.reloadMembersWidgets();
        partyManagementScreen.reloadSuggestedPlayersWidgets();
    }
}
