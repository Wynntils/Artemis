/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.crowdsourcing.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.crowdsource.type.CrowdSourcedDataType;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.wynntils.DataCrowdSourcingFeature;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.crowdsourcing.WynntilsCrowdSourcingSettingsScreen;
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
    private final WynntilsCrowdSourcingSettingsScreen crowdSourcingSettingsScreen;

    public CrowdSourcedDataWidget(
            int x,
            int y,
            int width,
            int height,
            CrowdSourcedDataType crowdSourcedDataType,
            WynntilsCrowdSourcingSettingsScreen screen) {
        super(x, y, width, height, Component.literal(crowdSourcedDataType.name()));

        this.crowdSourcedDataType = crowdSourcedDataType;
        this.crowdSourcingSettingsScreen = screen;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        CustomColor backgroundColor = this.isHovered ? BUTTON_COLOR.b() : BUTTON_COLOR.a();
        RenderUtils.drawRect(poseStack, backgroundColor, this.getX(), this.getY(), 0, this.width, this.height);

        FontRenderer.getInstance()
                .renderScrollingText(
                        poseStack,
                        StyledText.fromString(crowdSourcedDataType.getTranslatedName()),
                        this.getX() + 14,
                        this.getY() + 1,
                        this.width - 15,
                        CommonColors.BLACK,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NONE,
                        1f);

        Texture stateTexture =
                switch (Managers.CrowdSourcedData.getDataCollectionState(crowdSourcedDataType)) {
                    case FALSE -> Texture.ACTIVITY_CANNOT_START;
                    case TRUE -> Texture.ACTIVITY_FINISHED;
                    case UNCONFIRMED -> Texture.QUESTION_MARK;
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
