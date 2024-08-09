/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.container.widgets;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class PersonalStorageEditNameButton extends WynntilsButton {
    private static final List<Component> CANCEL_TOOLTIP = List.of(
            Component.translatable("screens.wynntils.containers.cancel.name").withStyle(ChatFormatting.RED),
            Component.translatable("screens.wynntils.containers.cancel.description")
                    .withStyle(ChatFormatting.GRAY));

    private static final List<Component> EDIT_TOOLTIP = List.of(
            Component.translatable("screens.wynntils.containers.edit.name").withStyle(ChatFormatting.YELLOW),
            Component.translatable("screens.wynntils.containers.edit.description")
                    .withStyle(ChatFormatting.GRAY));

    private final PersonalStorageUtilitiesWidget parent;

    public PersonalStorageEditNameButton(int x, int y, int width, int height, PersonalStorageUtilitiesWidget parent) {
        super(x, y, width, height, Component.literal("Personal Storage Edit Name Button"));

        this.parent = parent;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.EDIT_NAME_ICON.resource(),
                this.getX(),
                this.getY(),
                0,
                this.width,
                this.height,
                Texture.EDIT_NAME_ICON.width(),
                Texture.EDIT_NAME_ICON.height());

        if (isHovered) {
            List<Component> tooltipToUse = Models.Bank.isEditingName() ? CANCEL_TOOLTIP : EDIT_TOOLTIP;

            McUtils.mc()
                    .screen
                    .setTooltipForNextRenderPass(Lists.transform(tooltipToUse, Component::getVisualOrderText));
        }
    }

    // unused
    @Override
    public void onPress() {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (Models.Bank.isEditingName()) {
                parent.removeEditInput();
            } else {
                parent.addEditInput();
            }

            Models.Bank.toggleEditingName(!Models.Bank.isEditingName());
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !Models.Bank.isEditingName()) {
            Models.Bank.resetCurrentPageName();
        }

        return true;
    }
}
