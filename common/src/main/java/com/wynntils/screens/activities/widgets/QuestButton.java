/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.activities.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.quests.QuestInfo;
import com.wynntils.screens.activities.WynntilsQuestBookScreen;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.maps.MainMapScreen;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Pair;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class QuestButton extends WynntilsButton {
    private static final Pair<CustomColor, CustomColor> BUTTON_COLOR =
            Pair.of(new CustomColor(181, 174, 151), new CustomColor(121, 116, 101));
    private static final Pair<CustomColor, CustomColor> TRACKED_BUTTON_COLOR =
            Pair.of(new CustomColor(176, 197, 148), new CustomColor(126, 211, 106));
    private static final Pair<CustomColor, CustomColor> TRACKING_REQUESTED_BUTTON_COLOR =
            Pair.of(new CustomColor(255, 206, 127), new CustomColor(255, 196, 50));
    private final QuestInfo questInfo;
    private final WynntilsQuestBookScreen questBookScreen;

    public QuestButton(int x, int y, int width, int height, QuestInfo questInfo, WynntilsQuestBookScreen screen) {
        super(x, y, width, height, Component.literal("Quest Button"));
        this.questInfo = questInfo;
        this.questBookScreen = screen;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        CustomColor backgroundColor = getBackgroundColor();
        RenderUtils.drawRect(poseStack, backgroundColor, this.getX(), this.getY(), 0, this.width, this.height);

        FontRenderer.getInstance()
                .renderScrollingText(
                        poseStack,
                        StyledText.fromString(questInfo.name()),
                        this.getX() + 14,
                        this.getY() + 1,
                        this.width - 15,
                        CommonColors.BLACK,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NONE,
                        1f);

        Texture stateTexture =
                switch (questInfo.status()) {
                    case STARTED -> Texture.ACTIVITY_STARTED;
                    case COMPLETED -> Texture.ACTIVITY_FINISHED;
                    case AVAILABLE -> Texture.ACTIVITY_CAN_START;
                    case UNAVAILABLE -> Texture.ACTIVITY_CANNOT_START;
                };

        RenderUtils.drawTexturedRect(
                poseStack,
                stateTexture.resource(),
                this.getX() + 1,
                this.getY() + 1,
                stateTexture.width(),
                stateTexture.height(),
                stateTexture.width(),
                stateTexture.height());
    }

    private CustomColor getBackgroundColor() {
        Pair<CustomColor, CustomColor> colors;

        if (this.questInfo.equals(Models.Activity.getTrackedQuestInfo())) {
            colors = TRACKED_BUTTON_COLOR;
        } else if (this.questInfo.equals(questBookScreen.getTrackingRequested())) {
            colors = TRACKING_REQUESTED_BUTTON_COLOR;
        } else {
            colors = BUTTON_COLOR;
        }

        return (this.isHovered ? colors.b() : colors.a());
    }

    // Not called
    @Override
    public void onPress() {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            trackQuest();
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            Optional<Location> nextLocation = this.questInfo.nextLocation();

            nextLocation.ifPresent(location -> McUtils.mc().setScreen(MainMapScreen.create(location.x, location.z)));
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            openQuestWiki();
        }

        return true;
    }

    private void trackQuest() {
        if (this.questInfo.trackable()) {
            McUtils.playSoundUI(SoundEvents.ANVIL_LAND);
            if (this.questInfo.equals(Models.Activity.getTrackedQuestInfo())) {
                Models.Quest.stopTracking();
                questBookScreen.setTrackingRequested(null);
            } else {
                Models.Quest.startTracking(this.questInfo);
                questBookScreen.setTrackingRequested(this.questInfo);
            }
        }
    }

    private void openQuestWiki() {
        McUtils.playSoundUI(SoundEvents.EXPERIENCE_ORB_PICKUP);
        Models.Quest.openQuestOnWiki(questInfo);
    }

    public QuestInfo getQuestInfo() {
        return questInfo;
    }
}
