/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.crowdsourcing.widgets;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.crowdsource.type.CrowdSourcedDataType;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.wynntils.DataCrowdSourcingFeature;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.ConfirmedBoolean;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class CrowdSourcedDataWidget extends WynntilsButton implements TooltipProvider {
    private static final Pair<CustomColor, CustomColor> BUTTON_COLOR =
            Pair.of(new CustomColor(181, 174, 151), new CustomColor(121, 116, 101));

    private final CrowdSourcedDataType crowdSourcedDataType;
    private final float translationX;
    private final float translationY;

    public CrowdSourcedDataWidget(
            int x, int y, CrowdSourcedDataType crowdSourcedDataType, float translationX, float translationY) {
        super(x, y, 140, 26, Component.literal(crowdSourcedDataType.name()));

        this.crowdSourcedDataType = crowdSourcedDataType;
        this.translationX = translationX;
        this.translationY = translationY;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        CustomColor backgroundColor = this.isHovered ? BUTTON_COLOR.b() : BUTTON_COLOR.a();
        RenderUtils.drawRect(poseStack, backgroundColor, this.getX(), this.getY(), 0, this.width, this.height);

        FontRenderer.getInstance()
                .renderScrollingString(
                        poseStack,
                        StyledText.fromString(crowdSourcedDataType.getTranslatedName()),
                        this.getX() + 2,
                        this.getY() + 6,
                        this.width - 4,
                        translationX,
                        translationY,
                        CommonColors.BLACK,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NONE);

        Component componentState;
        Texture textureState;
        CustomColor color;

        // Get various details based on collection state
        switch (Managers.CrowdSourcedData.getDataCollectionState(crowdSourcedDataType)) {
            case FALSE -> {
                componentState = Component.literal("Not collecting");
                textureState = Texture.ACTIVITY_CANNOT_START;
                color = CommonColors.RED;
            }
            case TRUE -> {
                componentState = Component.literal("Collecting");
                textureState = Texture.ACTIVITY_FINISHED;
                color = CommonColors.LIGHT_GREEN;
            }
            default -> { // Unconfirmed
                componentState = Component.literal("Unconfirmed");
                textureState = Texture.QUESTION_MARK;
                color = CommonColors.YELLOW;
            }
        }

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromComponent(componentState),
                        this.getX() + 15,
                        this.getY() + 21,
                        this.width - 4,
                        color,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NONE);

        RenderUtils.drawTexturedRect(poseStack, textureState, this.getX() + 2, this.getY() + 16);

        if (isHovered) {
            McUtils.mc()
                    .screen
                    .setTooltipForNextRenderPass(Lists.transform(getTooltipLines(), Component::getVisualOrderText));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Toggle collection state
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (Managers.CrowdSourcedData.getDataCollectionState(crowdSourcedDataType) == ConfirmedBoolean.TRUE) {
                Managers.Feature.getFeatureInstance(DataCrowdSourcingFeature.class)
                        .crowdSourcedDataTypeEnabledMap
                        .get()
                        .put(crowdSourcedDataType, ConfirmedBoolean.FALSE);

                Managers.Config.saveConfig();
                return true;
            }

            if (!Managers.CrowdSourcedData.isDataCollectionEnabled()) {
                Managers.Feature.getFeatureInstance(DataCrowdSourcingFeature.class)
                        .userEnabled
                        .setValue(true);
            }

            Managers.Feature.getFeatureInstance(DataCrowdSourcingFeature.class)
                    .crowdSourcedDataTypeEnabledMap
                    .get()
                    .put(crowdSourcedDataType, ConfirmedBoolean.TRUE);
            Managers.Config.saveConfig();

            return true;
        }

        // Copy collected data to clipboard
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            ConfirmedBoolean dataCollectionState =
                    Managers.CrowdSourcedData.getDataCollectionState(crowdSourcedDataType);
            if (dataCollectionState == ConfirmedBoolean.UNCONFIRMED) {
                Managers.Feature.getFeatureInstance(DataCrowdSourcingFeature.class)
                        .crowdSourcedDataTypeEnabledMap
                        .get()
                        .put(crowdSourcedDataType, ConfirmedBoolean.FALSE);

                Managers.Config.saveConfig();
                return true;
            }

            Set<Object> data = Managers.CrowdSourcedData.getData(crowdSourcedDataType);

            String jsonString = Managers.Json.GSON.toJson(Map.of(Managers.CrowdSourcedData.CURRENT_GAME_VERSION, data));

            McUtils.mc().keyboardHandler.setClipboard(jsonString);

            // Tell the user how much they collected
            McUtils.sendMessageToClient(Component.translatable(
                            "screens.wynntils.wynntilsCrowdSourcing.copiedToClipboard",
                            data.size(),
                            crowdSourcedDataType.getTranslatedName())
                    .withStyle(ChatFormatting.GREEN));

            return true;
        }

        return false;
    }

    @Override
    public void onPress() {}

    @Override
    public List<Component> getTooltipLines() {
        List<Component> lines = new ArrayList<>();

        lines.add(Component.literal(crowdSourcedDataType.getTranslatedName())
                .withStyle(ChatFormatting.BOLD)
                .withStyle(ChatFormatting.GOLD));

        lines.add(Component.empty());

        lines.add(Component.literal(crowdSourcedDataType.getTranslatedDescription())
                .withStyle(ChatFormatting.GRAY));

        lines.add(Component.empty());

        ConfirmedBoolean dataCollectionState = Managers.CrowdSourcedData.getDataCollectionState(crowdSourcedDataType);
        if (!Managers.CrowdSourcedData.isDataCollectionEnabled()) {
            lines.add(Component.translatable("feature.wynntils.dataCrowdSourcing.button.enableWithFeature")
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(ChatFormatting.DARK_GREEN));
        } else if (dataCollectionState != ConfirmedBoolean.TRUE) {
            lines.add(Component.translatable("feature.wynntils.dataCrowdSourcing.button.enable")
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(ChatFormatting.GREEN));
        } else {
            lines.add(Component.translatable("feature.wynntils.dataCrowdSourcing.button.disable")
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(ChatFormatting.RED));
        }

        if (dataCollectionState == ConfirmedBoolean.UNCONFIRMED) {
            lines.add(Component.translatable("feature.wynntils.dataCrowdSourcing.button.disableUnconfirmed")
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(ChatFormatting.RED));
        } else {
            lines.add(Component.translatable("feature.wynntils.dataCrowdSourcing.button.copy")
                    .withStyle(ChatFormatting.BOLD)
                    .withStyle(ChatFormatting.YELLOW));
        }

        return ComponentUtils.wrapTooltips(lines, 200);
    }
}
