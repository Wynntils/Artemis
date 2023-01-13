/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.screens.guides;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.gui.render.FontRenderer;
import com.wynntils.gui.render.HorizontalAlignment;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.gui.render.VerticalAlignment;
import com.wynntils.gui.screens.WynntilsGuidesListScreen;
import com.wynntils.gui.screens.WynntilsMenuListScreen;
import com.wynntils.gui.screens.guides.widgets.GuideEmeraldPouchItemStackButton;
import com.wynntils.gui.widgets.BackButton;
import com.wynntils.gui.widgets.PageSelectorButton;
import com.wynntils.mc.objects.CommonColors;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

public final class WynntilsEmeraldPouchGuideScreen
        extends WynntilsMenuListScreen<GuideEmeraldPouchItemStack, GuideEmeraldPouchItemStackButton> {
    private static final int ELEMENTS_COLUMNS = 7;
    private static final int ELEMENT_ROWS = 7;

    private List<GuideEmeraldPouchItemStack> parsedItemCache;

    private WynntilsEmeraldPouchGuideScreen() {
        super(Component.translatable("screens.wynntils.wynntilsGuides.emeraldPouch.name"));
    }

    public static Screen create() {
        return new WynntilsEmeraldPouchGuideScreen();
    }

    @Override
    protected void doInit() {
        if (parsedItemCache == null) {
            parsedItemCache = new ArrayList<>();

            for (int i = 1; i <= 10; i++) {
                parsedItemCache.add(new GuideEmeraldPouchItemStack(i));
            }
        }

        super.doInit();

        this.addRenderableWidget(new BackButton(
                (int) ((Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 16) / 2f),
                65,
                Texture.BACK_ARROW.width() / 2,
                Texture.BACK_ARROW.height(),
                WynntilsGuidesListScreen.create()));

        this.addRenderableWidget(new PageSelectorButton(
                Texture.QUEST_BOOK_BACKGROUND.width() / 2 + 50 - Texture.FORWARD_ARROW.width() / 2,
                Texture.QUEST_BOOK_BACKGROUND.height() - 25,
                Texture.FORWARD_ARROW.width() / 2,
                Texture.FORWARD_ARROW.height(),
                false,
                this));
        this.addRenderableWidget(new PageSelectorButton(
                Texture.QUEST_BOOK_BACKGROUND.width() - 50,
                Texture.QUEST_BOOK_BACKGROUND.height() - 25,
                Texture.FORWARD_ARROW.width() / 2,
                Texture.FORWARD_ARROW.height(),
                true,
                this));
    }

    @Override
    public void doRender(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackgroundTexture(poseStack);

        // Make 0, 0 the top left corner of the rendered quest book background
        poseStack.pushPose();
        final float translationX = getTranslationX();
        final float translationY = getTranslationY();
        poseStack.translate(translationX, translationY, 1f);

        renderTitle(poseStack, I18n.get("screens.wynntils.wynntilsGuides.emeraldPouch.name"));

        renderVersion(poseStack);

        renderItemsHeader(poseStack);

        renderButtons(poseStack, mouseX, mouseY, partialTick);

        renderPageInfo(poseStack, currentPage + 1, maxPage + 1);

        poseStack.popPose();

        renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    protected void renderTitle(PoseStack poseStack, String titleString) {
        int txWidth = Texture.QUEST_BOOK_TITLE.width();
        int txHeight = Texture.QUEST_BOOK_TITLE.height();
        RenderUtils.drawScalingTexturedRect(
                poseStack, Texture.QUEST_BOOK_TITLE.resource(), 0, 30, 0, txWidth, txHeight, txWidth, txHeight);

        poseStack.pushPose();
        poseStack.translate(10, 36, 0);
        poseStack.scale(1.5f, 1.5f, 0f);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        titleString,
                        0,
                        0,
                        CommonColors.YELLOW,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NORMAL);
        poseStack.popPose();
    }

    private void renderTooltip(PoseStack poseStack, int mouseX, int mouseY) {
        if (hovered instanceof GuideEmeraldPouchItemStackButton guideEmeraldPouchItemStack) {
            GuideEmeraldPouchItemStack itemStack = guideEmeraldPouchItemStack.getItemStack();

            List<Component> tooltipLines = itemStack.getTooltipLines(McUtils.player(), TooltipFlag.NORMAL);
            tooltipLines.add(Component.empty());
            if (Managers.Favorites.isFavorite(itemStack)) {
                tooltipLines.add(Component.translatable("screens.wynntils.wynntilsGuides.itemGuide.unfavorite")
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                tooltipLines.add(Component.translatable("screens.wynntils.wynntilsGuides.itemGuide.favorite")
                        .withStyle(ChatFormatting.GREEN));
            }

            RenderUtils.drawTooltipAt(
                    poseStack,
                    mouseX,
                    mouseY,
                    0,
                    ComponentUtils.wrapTooltips(tooltipLines, 200),
                    FontRenderer.getInstance().getFont(),
                    true);
        }
    }

    private void renderItemsHeader(PoseStack poseStack) {
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        I18n.get("screens.wynntils.wynntilsGuides.itemGuide.available"),
                        Texture.QUEST_BOOK_BACKGROUND.width() * 0.75f,
                        30,
                        CommonColors.BLACK,
                        HorizontalAlignment.Center,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NONE);
    }

    @Override
    protected GuideEmeraldPouchItemStackButton getButtonFromElement(int i) {
        int xOffset = (i % ELEMENTS_COLUMNS) * 20;
        int yOffset = ((i % getElementsPerPage()) / ELEMENTS_COLUMNS) * 20;

        return new GuideEmeraldPouchItemStackButton(
                xOffset + Texture.QUEST_BOOK_BACKGROUND.width() / 2 + 13, yOffset + 43, 18, 18, elements.get(i), this);
    }

    @Override
    protected void reloadElementsList(String searchTerm) {
        elements.addAll(parsedItemCache.stream()
                .filter(itemStack ->
                        StringUtils.partialMatch(ComponentUtils.getUnformatted(itemStack.getHoverName()), searchTerm))
                .toList());
    }

    @Override
    protected int getElementsPerPage() {
        return ELEMENT_ROWS * ELEMENTS_COLUMNS;
    }
}
