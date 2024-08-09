/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.partymanagement.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SuggestionPlayerWidget extends AbstractWidget {
    private final String playerName;
    private final Button inviteButton;
    private final float gridDivisions;

    public SuggestionPlayerWidget(float x, float y, int width, int height, String playerName, float gridDivisions) {
        super((int) x, (int) y, width, height, Component.literal(playerName));
        this.playerName = playerName;
        this.gridDivisions = gridDivisions;
        this.inviteButton = new Button.Builder(
                        Component.translatable("screens.wynntils.partyManagementGui.invite"),
                        (button) -> Models.Party.partyInvite(playerName))
                .pos((int) (this.getX() + (this.width / this.gridDivisions * 20)) + 1, this.getY())
                .size(
                        (int) ((this.getX() + (this.width / this.gridDivisions * 24))
                                        - (this.getX() + (this.width / this.gridDivisions * 20)))
                                - 2,
                        20)
                .build();
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
        RenderUtils.drawTexturedRect(
                poseStack,
                skin,
                this.getX() + (this.width / gridDivisions) - 8,
                this.getY() + (this.height / 2) - 8,
                8,
                16,
                16,
                40,
                8,
                8,
                8,
                64,
                64);

        // name rendering
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(playerName),
                        this.getX() + (this.width / gridDivisions * 3),
                        this.getY() + this.height / 2,
                        CommonColors.GREEN,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        if (Models.Party.getPartyMembers().contains(playerName)) return;
        inviteButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return inviteButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
