/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.partymanagement.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PartyMemberWidget extends AbstractWidget {
    private final String playerName;
    private final boolean isOffline;
    private final Button promoteButton;
    private final Button kickButton;
    private final Button disbandButton;
    private final Button moveUpButton;
    private final Button moveDownButton;
    private final float gridDivisions;

    public PartyMemberWidget(
            float x, float y, int width, int height, String playerName, boolean isOffline, float gridDivisions) {
        super((int) x, (int) y, width, height, Component.literal(playerName));
        this.playerName = playerName;
        this.isOffline = isOffline;
        this.gridDivisions = gridDivisions;
        this.promoteButton = new Button.Builder(
                        Component.translatable("screens.wynntils.partyManagementGui.promote"),
                        (button) -> Models.Party.partyPromote(playerName))
                .pos((int) (this.getX() + (this.width / this.gridDivisions * 16)) + 1, this.getY())
                .size(
                        (int) ((this.getX() + (this.width / this.gridDivisions * 20))
                                        - (this.getX() + (this.width / this.gridDivisions * 16)))
                                - 2,
                        20)
                .build();
        this.kickButton = new Button.Builder(
                        Component.translatable("screens.wynntils.partyManagementGui.kick"),
                        (button) -> Models.Party.partyKick(playerName))
                .pos((int) (this.getX() + (this.width / this.gridDivisions * 20)) + 1, this.getY())
                .size(
                        (int) ((this.getX() + (this.width / this.gridDivisions * 24))
                                        - (this.getX() + (this.width / this.gridDivisions * 20)))
                                - 2,
                        20)
                .build();
        this.disbandButton = new Button.Builder(
                        Component.translatable("screens.wynntils.partyManagementGui.disband"),
                        (button) -> Models.Party.partyDisband())
                .pos((int) (this.getX() + (this.width / this.gridDivisions * 20)) + 1, this.getY())
                .size(
                        (int) ((this.getX() + (this.width / this.gridDivisions * 24))
                                        - (this.getX() + (this.width / this.gridDivisions * 20)))
                                - 2,
                        20)
                .build();
        this.moveUpButton = new Button.Builder(
                        Component.literal("🠝"), (button) -> Models.Party.increasePlayerPriority(playerName))
                .pos((int) (this.getX() + (this.width / this.gridDivisions * 24)) + 1, this.getY())
                .size(
                        (int) ((this.getX() + (this.width / this.gridDivisions * 25))
                                        - (this.getX() + (this.width / this.gridDivisions * 24)))
                                - 2,
                        20)
                .build();
        this.moveDownButton = new Button.Builder(
                        Component.literal("🠟"), (button) -> Models.Party.decreasePlayerPriority(playerName))
                .pos((int) (this.getX() + (this.width / this.gridDivisions * 25)) + 1, this.getY())
                .size(
                        (int) ((this.getX() + (this.width / this.gridDivisions * 26))
                                        - (this.getX() + (this.width / this.gridDivisions * 25)))
                                - 2,
                        20)
                .build();
        if (Models.Party.isPartyLeader(playerName)) {
            this.promoteButton.active = false;
            this.kickButton.active = false;
        } else {
            this.disbandButton.active = false;
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        PlayerInfo playerInfo =
                McUtils.mc().getConnection().getPlayerInfo(playerName); // Disconnected players will just be steves
        ResourceLocation skin = (playerInfo == null)
                ? ResourceLocation.withDefaultNamespace("textures/entity/steve.png")
                : playerInfo.getSkin().texture();
        // head rendering
        RenderUtils.drawTexturedRect(
                poseStack,
                skin,
                this.getX() + (this.width / gridDivisions) - 8,
                this.getY() + (this.height / 2) - 8,
                8,
                16,
                16,
                8,
                8,
                8,
                8,
                64,
                64);
        // hat rendering
        RenderUtils.drawTexturedRect(
                poseStack,
                skin,
                this.getX() + (this.width / gridDivisions) - 8,
                this.getY() + (this.height / 2) - 8,
                1,
                16,
                16,
                40,
                8,
                8,
                8,
                64,
                64);

        // name rendering
        CustomColor color = CommonColors.WHITE;
        if (Models.Party.isPartyLeader(playerName)) {
            color = CommonColors.YELLOW;
        } else if (Models.Friends.getFriends().contains(playerName)) {
            color = CommonColors.GREEN;
        }
        String prefix = "";
        if (isOffline) {
            prefix = ChatFormatting.STRIKETHROUGH.toString();
        } else if (playerName.equals(McUtils.playerName())) {
            prefix = ChatFormatting.BOLD.toString();
        }

        StyledText formattedPlayerName = StyledText.fromString(prefix + playerName);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        formattedPlayerName,
                        this.getX() + (this.width / gridDivisions * 3),
                        this.getY() + this.height / 2,
                        color,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        moveUpButton.render(guiGraphics, mouseX, mouseY, partialTick);
        moveDownButton.render(guiGraphics, mouseX, mouseY, partialTick);

        // only leader can promote/kick
        if (!Models.Party.isPartyLeader(McUtils.playerName())) return;

        if (Models.Party.isPartyLeader(playerName)) {
            disbandButton.render(guiGraphics, mouseX, mouseY, partialTick);
        } else {
            promoteButton.render(guiGraphics, mouseX, mouseY, partialTick);
            kickButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (moveUpButton.mouseClicked(mouseX, mouseY, button) || moveDownButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (!Models.Party.isPartyLeader(McUtils.playerName())) return false;

        return promoteButton.mouseClicked(mouseX, mouseY, button)
                || kickButton.mouseClicked(mouseX, mouseY, button)
                || disbandButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
