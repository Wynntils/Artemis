/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ContainerRenderEvent;
import com.wynntils.models.containers.containers.CharacterInfoContainer;
import com.wynntils.models.containers.containers.personal.PersonalStorageContainer;
import com.wynntils.models.emeralds.type.EmeraldUnits;
import com.wynntils.screens.gearviewer.GearViewerScreen;
import com.wynntils.screens.itemsharing.SavedItemsScreen;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.Arrays;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.INVENTORY)
public class InventoryEmeraldCountFeature extends Feature {
    private static final int TEXTURE_SIZE = 28;

    @Persisted
    public final Config<EmeraldCountType> emeraldCountType = new Config<>(EmeraldCountType.TEXTURE);

    @Persisted
    public final Config<TextDisplaySide> textDisplaySide = new Config<>(TextDisplaySide.LEFT);

    @Persisted
    public final Config<Boolean> showInventoryEmeraldCount = new Config<>(true);

    @Persisted
    public final Config<Boolean> showContainerEmeraldCount = new Config<>(true);

    @Persisted
    public final Config<Boolean> showZerosInEmeraldCount = new Config<>(true);

    @Persisted
    public final Config<Boolean> combineInventoryAndContainer = new Config<>(false);

    @SubscribeEvent
    public void onContainerRender(ContainerRenderEvent event) {
        Screen screen = McUtils.mc().screen;
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;
        if (isExcludedContainer(screen)) return;

        // Always draw top part, which is all there is if it is inventory,
        // and all there is if we combine them, otherwise it is just the
        // container
        boolean isInventory = (event.getScreen().getMenu().containerId == 0);
        int topEmeralds;
        if (isInventory) {
            if (!showInventoryEmeraldCount.get()) return;
            topEmeralds = Models.Emerald.getAmountInInventory();
        } else {
            topEmeralds = 0;
            if (showContainerEmeraldCount.get()) topEmeralds += Models.Emerald.getAmountInContainer();
            if (combineInventoryAndContainer.get() && showInventoryEmeraldCount.get()) {
                topEmeralds += Models.Emerald.getAmountInInventory();
            }
        }

        int textureX = containerScreen.leftPos;
        int textX = (textDisplaySide.get() == TextDisplaySide.LEFT)
                ? containerScreen.leftPos + 2
                : screen.width - containerScreen.leftPos - 2;

        if (Models.Container.getCurrentContainer() instanceof PersonalStorageContainer
                && Managers.Feature.getFeatureInstance(PersonalStorageUtilitiesFeature.class)
                        .isEnabled()) {
            textureX -= 10 + Texture.BANK_PANEL.width();
        }

        int bottomEmeralds = Models.Emerald.getAmountInInventory();
        boolean displayBottom = !isInventory
                && !combineInventoryAndContainer.get()
                && showInventoryEmeraldCount.get()
                && bottomEmeralds != 0;
        if (topEmeralds != 0) {
            int y = containerScreen.topPos;
            switch (emeraldCountType.get()) {
                case TEXT -> renderTextCount(event.getPoseStack(), textX, y, topEmeralds);
                case TEXTURE -> {
                    if (displayBottom) { // ensure we don't overlap with bottom textures
                        int topDisplayedTextureCount = (int) Arrays.stream(getRenderableEmeraldAmounts(topEmeralds))
                                .filter(s -> showZerosInEmeraldCount.get() || !s.equals("0"))
                                .count();
                        int textureVerticalSize = topDisplayedTextureCount * TEXTURE_SIZE + 2;
                        int bottomStartY = containerScreen.topPos + containerScreen.imageHeight - TEXTURE_SIZE * 3 - 2;
                        y = Math.min(bottomStartY - textureVerticalSize, y);
                    }
                    renderTexturedCount(event.getGuiGraphics(), textureX, y, topEmeralds);
                }
            }
        }

        if (displayBottom) {
            int y = containerScreen.topPos + containerScreen.imageHeight;
            switch (emeraldCountType.get()) {
                case TEXT -> renderTextCount(event.getPoseStack(), textX, y + 11, bottomEmeralds);
                case TEXTURE -> renderTexturedCount(
                        event.getGuiGraphics(), textureX, y - TEXTURE_SIZE * 3 - 2, bottomEmeralds);
            }
        }
    }

    private void renderTextCount(PoseStack poseStack, int x, int y, int emeralds) {
        final HorizontalAlignment emeraldTextAlignment =
                textDisplaySide.get() == TextDisplaySide.LEFT ? HorizontalAlignment.LEFT : HorizontalAlignment.RIGHT;
        final int emeraldTextOffsetX = textDisplaySide.get() == TextDisplaySide.LEFT ? 1 : -1;

        poseStack.pushPose();
        poseStack.translate(0, 0, 200);

        String emeraldText;
        if (KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            emeraldText = emeralds + EmeraldUnits.EMERALD.getSymbol();
        } else {
            emeraldText = Models.Emerald.getFormattedString(emeralds, showZerosInEmeraldCount.get());
        }

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(emeraldText),
                        x + emeraldTextOffsetX,
                        y - 10,
                        0,
                        CommonColors.WHITE,
                        emeraldTextAlignment,
                        VerticalAlignment.TOP,
                        TextShadow.NORMAL);

        poseStack.popPose();
    }

    private void renderTexturedCount(GuiGraphics guiGraphics, int x, int y, int emeralds) {
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();
        poseStack.translate(x, y, 0);

        String[] emeraldAmounts = getRenderableEmeraldAmounts(emeralds);

        int renderedCount = 0;

        for (int i = emeraldAmounts.length - 1; i >= 0; i--) {
            String emeraldAmount = emeraldAmounts[i];

            if (!showZerosInEmeraldCount.get() && emeraldAmount.equals("0")) continue;

            final int renderX = -TEXTURE_SIZE;
            final int renderY = renderedCount * TEXTURE_SIZE;
            RenderUtils.drawTexturedRect(
                    poseStack,
                    Texture.EMERALD_COUNT_BACKGROUND.resource(),
                    renderX,
                    renderY,
                    0,
                    TEXTURE_SIZE,
                    TEXTURE_SIZE,
                    0,
                    0,
                    Texture.EMERALD_COUNT_BACKGROUND.width(),
                    Texture.EMERALD_COUNT_BACKGROUND.height(),
                    Texture.EMERALD_COUNT_BACKGROUND.width(),
                    Texture.EMERALD_COUNT_BACKGROUND.height());

            guiGraphics.renderItem(EmeraldUnits.values()[i].getItemStack(), renderX + 6, renderY + 6);

            if (EmeraldUnits.values()[i].getSymbol().equals("stx")) { // Make stx not look like normal LE
                guiGraphics.renderItem(EmeraldUnits.values()[i].getItemStack(), renderX + 3, renderY + 4);
                guiGraphics.renderItem(EmeraldUnits.values()[i].getItemStack(), renderX + 6, renderY + 6);
                guiGraphics.renderItem(EmeraldUnits.values()[i].getItemStack(), renderX + 9, renderY + 8);
            } else {
                // This needs to be separate since Z levels are determined by order here
                guiGraphics.renderItem(EmeraldUnits.values()[i].getItemStack(), renderX + 6, renderY + 6);
            }

            poseStack.pushPose();
            poseStack.translate(0, 0, 200);
            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            StyledText.fromString(emeraldAmount),
                            renderX,
                            renderX + TEXTURE_SIZE - 2,
                            renderY,
                            renderY + TEXTURE_SIZE - 2,
                            0,
                            CommonColors.WHITE,
                            HorizontalAlignment.RIGHT,
                            VerticalAlignment.BOTTOM,
                            TextShadow.OUTLINE);
            poseStack.popPose();

            renderedCount++;
        }

        poseStack.popPose();
    }

    private String[] getRenderableEmeraldAmounts(int emeralds) {
        String[] emeraldAmounts = new String[4];
        if (KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            emeraldAmounts[0] = StringUtils.formatAmount(emeralds);
            emeraldAmounts[1] = StringUtils.formatAmount(emeralds / 64d);
            emeraldAmounts[2] = StringUtils.formatAmount(emeralds / 4096d);
            emeraldAmounts[3] = StringUtils.formatAmount(emeralds / 262144d);
        } else {
            emeraldAmounts = Arrays.stream(Models.Emerald.emeraldsPerUnit(emeralds))
                    .mapToObj(String::valueOf)
                    .toArray(String[]::new);
        }

        return emeraldAmounts;
    }

    private boolean isExcludedContainer(Screen screen) {
        return Models.Container.getCurrentContainer() instanceof CharacterInfoContainer
                || screen instanceof GearViewerScreen
                || screen instanceof SavedItemsScreen;
    }

    public enum EmeraldCountType {
        TEXT,
        TEXTURE
    }

    private enum TextDisplaySide {
        RIGHT,
        LEFT
    }
}
