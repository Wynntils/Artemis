/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.guides;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.WynntilsListScreen;
import com.wynntils.screens.base.widgets.BackButton;
import com.wynntils.screens.base.widgets.PageSelectorButton;
import com.wynntils.screens.guides.charm.WynntilsCharmGuideScreen;
import com.wynntils.screens.guides.emeraldpouch.WynntilsEmeraldPouchGuideScreen;
import com.wynntils.screens.guides.gear.WynntilsItemGuideScreen;
import com.wynntils.screens.guides.ingredient.WynntilsIngredientGuideScreen;
import com.wynntils.screens.guides.powder.WynntilsPowderGuideScreen;
import com.wynntils.screens.guides.tome.WynntilsTomeGuideScreen;
import com.wynntils.screens.guides.widgets.ExportButton;
import com.wynntils.screens.guides.widgets.GuidesButton;
import com.wynntils.screens.guides.widgets.ImportButton;
import com.wynntils.screens.wynntilsmenu.WynntilsMenuScreen;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.Texture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public final class WynntilsGuidesListScreen extends WynntilsListScreen<Screen, GuidesButton> {
    private static final List<Screen> GUIDES = List.of(
            WynntilsItemGuideScreen.create(),
            WynntilsIngredientGuideScreen.create(),
            WynntilsTomeGuideScreen.create(),
            WynntilsCharmGuideScreen.create(),
            WynntilsEmeraldPouchGuideScreen.create(),
            WynntilsPowderGuideScreen.create());

    private WynntilsGuidesListScreen() {
        super(Component.translatable("screens.wynntils.wynntilsGuides.name"));
    }

    public static Screen create() {
        return new WynntilsGuidesListScreen();
    }

    @Override
    protected void doInit() {
        super.doInit();

        this.addRenderableWidget(new BackButton(
                (int) ((Texture.CONTENT_BOOK_BACKGROUND.width() / 2f - 16) / 2f),
                65,
                Texture.BACK_ARROW_OFFSET.width() / 2,
                Texture.BACK_ARROW_OFFSET.height(),
                WynntilsMenuScreen.create()));

        this.addRenderableWidget(new ImportButton(
                Texture.CONTENT_BOOK_BACKGROUND.width() - 21,
                11,
                (int) (Texture.ADD_ICON.width() / 1.5f),
                (int) (Texture.ADD_ICON.height() / 1.5f),
                this::importFavorites));
        this.addRenderableWidget(new ExportButton(
                Texture.CONTENT_BOOK_BACKGROUND.width() - 21,
                11 + (int) (Texture.ADD_ICON.height() / 1.5f),
                (int) (Texture.SHARE_ICON.width() / 1.5f),
                (int) (Texture.SHARE_ICON.height() / 1.5f),
                this::exportFavorites));

        this.addRenderableWidget(new PageSelectorButton(
                Texture.CONTENT_BOOK_BACKGROUND.width() / 2 + 50 - Texture.FORWARD_ARROW_OFFSET.width() / 2,
                Texture.CONTENT_BOOK_BACKGROUND.height() - 25,
                Texture.FORWARD_ARROW_OFFSET.width() / 2,
                Texture.FORWARD_ARROW_OFFSET.height(),
                false,
                this));
        this.addRenderableWidget(new PageSelectorButton(
                Texture.CONTENT_BOOK_BACKGROUND.width() - 50,
                Texture.CONTENT_BOOK_BACKGROUND.height() - 25,
                Texture.FORWARD_ARROW_OFFSET.width() / 2,
                Texture.FORWARD_ARROW_OFFSET.height(),
                true,
                this));
    }

    private void importFavorites() {
        String clipboard = McUtils.mc().keyboardHandler.getClipboard();

        if (clipboard == null || !clipboard.startsWith("wynntilsFavorites,")) {
            McUtils.sendErrorToClient(I18n.get("screens.wynntils.wynntilsGuides.invalidClipboard"));
        }

        ArrayList<String> names = new ArrayList<>(Arrays.asList(clipboard.split(",")));
        names.removeFirst(); // Remove the "wynntilsFavorites," part
        names.forEach(name -> {
            if (name.isBlank() || name.isEmpty()) return;
            Services.Favorites.addFavorite(name);
        });
        McUtils.sendMessageToClient(
                Component.translatable("screens.wynntils.wynntilsGuides.importedFavorites", names.size())
                        .withStyle(ChatFormatting.GREEN));
    }

    private void exportFavorites() {
        McUtils.mc()
                .keyboardHandler
                .setClipboard("wynntilsFavorites," + String.join(",", Services.Favorites.getFavoriteItems()));
        McUtils.sendMessageToClient(Component.translatable(
                        "screens.wynntils.wynntilsGuides.exportedFavorites",
                        Services.Favorites.getFavoriteItems().size())
                .withStyle(ChatFormatting.GREEN));
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        renderBackgroundTexture(poseStack);

        // Make 0, 0 the top left corner of the rendered quest book background
        poseStack.pushPose();
        final float translationX = getTranslationX();
        final float translationY = getTranslationY();
        poseStack.translate(translationX, translationY, 1f);

        renderTitle(poseStack, I18n.get("screens.wynntils.wynntilsGuides.name"));

        renderVersion(poseStack);

        renderWidgets(guiGraphics, mouseX, mouseY, partialTick);

        renderDescription(poseStack, I18n.get("screens.wynntils.wynntilsGuides.screenDescription"), "");

        renderPageInfo(poseStack, currentPage + 1, maxPage + 1);

        poseStack.popPose();

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected GuidesButton getButtonFromElement(int i) {
        int offset = i % getElementsPerPage();
        return new GuidesButton(
                Texture.CONTENT_BOOK_BACKGROUND.width() / 2 + 15,
                offset * 13 + 25,
                Texture.CONTENT_BOOK_BACKGROUND.width() / 2 - 37,
                9,
                elements.get(i),
                getTranslationX(),
                getTranslationY());
    }

    @Override
    protected void reloadElementsList(String searchTerm) {
        elements.addAll(GUIDES.stream()
                .filter(screen -> StringUtils.partialMatch(
                        StyledText.fromComponent(screen.getTitle()).getStringWithoutFormatting(), searchTerm))
                .toList());
    }
}
