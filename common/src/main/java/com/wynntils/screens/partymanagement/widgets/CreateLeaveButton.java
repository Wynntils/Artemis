package com.wynntils.screens.partymanagement.widgets;

import com.wynntils.core.components.Models;
import com.wynntils.screens.base.widgets.WynntilsButton;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class CreateLeaveButton extends WynntilsButton {

    public CreateLeaveButton(int x, int y, int width, int height) {
        super(x, y, width, height, Models.Party.isInParty() ?
                Component.translatable("screens.wynntils.partyManagementGui.leavePartyButton").withStyle(ChatFormatting.RED) :
                Component.translatable("screens.wynntils.partyManagementGui.createPartyButton"));
    }

    @Override
    public void onPress() {
        if (Models.Party.isInParty()) {
            Models.Party.partyLeave();
        } else {
            Models.Party.partyCreate();
        }
    }
}
