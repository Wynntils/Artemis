/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.wynntilsmenu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.map.MainMapFeature;
import com.wynntils.features.ui.WynntilsContentBookFeature;
import com.wynntils.screens.activities.WynntilsCaveScreen;
import com.wynntils.screens.activities.WynntilsDialogueHistoryScreen;
import com.wynntils.screens.activities.WynntilsDiscoveriesScreen;
import com.wynntils.screens.activities.WynntilsQuestBookScreen;
import com.wynntils.screens.base.WynntilsMenuScreenBase;
import com.wynntils.screens.crowdsourcing.WynntilsCrowdSourcingSettingsScreen;
import com.wynntils.screens.guides.WynntilsGuidesListScreen;
import com.wynntils.screens.itemsharing.SavedItemsScreen;
import com.wynntils.screens.lootrunpaths.WynntilsLootrunPathsScreen;
import com.wynntils.screens.maps.MainMapScreen;
import com.wynntils.screens.maps.PoiManagementScreen;
import com.wynntils.screens.overlays.selection.OverlaySelectionScreen;
import com.wynntils.screens.settings.WynntilsBookSettingsScreen;
import com.wynntils.screens.statistics.WynntilsStatisticsScreen;
import com.wynntils.screens.wynntilsmenu.widgets.WynntilsMenuButton;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.RenderedStringUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.wynn.ContainerUtils;
import com.wynntils.utils.wynn.InventoryUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class WynntilsMenuScreen extends WynntilsMenuScreenBase {
    private static final int BUTTON_SIZE = 30;
    private static final CustomColor BUTTON_COLOR = new CustomColor(181, 174, 151);
    private static final CustomColor BUTTON_COLOR_HOVERED = new CustomColor(121, 116, 101);

    private final List<List<WynntilsMenuButton>> buttons = new ArrayList<>();
    private WynntilsMenuButton hovered = null;

    private boolean firstInit = true;

    // This makes sure we "save" our status on the settings & overlay screen, and we reopen it in the same state
    private static final Screen overlayScreenInstance = OverlaySelectionScreen.create();
    private static final Screen settingsScreenInstance = WynntilsBookSettingsScreen.create(null);

    private WynntilsMenuScreen() {
        super(Component.translatable("screens.wynntils.wynntilsMenu.name"));
        setup();
    }

    public static Screen create() {
        return new WynntilsMenuScreen();
    }

    @Override
    protected void doInit() {
        if (firstInit) {
            if (Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class)
                    .displayOverallProgress
                    .get()) {
                Models.Activity.scanOverallProgress();
            }
        }

        firstInit = false;
    }

    private void setup() {
        // Add 4 rows of buttons
        for (int i = 0; i < 4; i++) {
            buttons.add(new ArrayList<>());
        }

        // region Row 1: Content / Activities
        buttons.getFirst()
                .add(new WynntilsMenuButton(
                        Texture.QUEST_BOOK_ICON,
                        true,
                        WynntilsQuestBookScreen.create(),
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable("screens.wynntils.wynntilsMenu.questBook.name")
                                                .withStyle(ChatFormatting.BOLD)
                                                .withStyle(ChatFormatting.GOLD)),
                                Component.translatable("screens.wynntils.wynntilsMenu.questBook.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));
        buttons.getFirst()
                .add(new WynntilsMenuButton(
                        Texture.DISCOVERIES_ICON,
                        true,
                        WynntilsDiscoveriesScreen.create(),
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.name")
                                                .withStyle(ChatFormatting.BOLD)
                                                .withStyle(ChatFormatting.GOLD)),
                                Component.translatable("screens.wynntils.wynntilsDiscoveries.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));
        buttons.get(0)
                .add(new WynntilsMenuButton(
                        Texture.CAVE,
                        false,
                        WynntilsCaveScreen.create(),
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable("screens.wynntils.wynntilsCaves.name")
                                                .withStyle(ChatFormatting.BOLD)
                                                .withStyle(ChatFormatting.GOLD)),
                                Component.translatable("screens.wynntils.wynntilsCaves.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));
        buttons.get(0)
                .add(new WynntilsMenuButton(
                        Texture.ADD_ICON,
                        false,
                        () -> ContainerUtils.openInventory(InventoryUtils.CONTENT_BOOK_SLOT_NUM),
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable("screens.wynntils.additionalContent.name")
                                                .withStyle(ChatFormatting.BOLD)
                                                .withStyle(ChatFormatting.GOLD)),
                                Component.translatable("screens.wynntils.additionalContent.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));

        // endregion

        // region Row 2: Map

        if (Managers.Feature.getFeatureInstance(MainMapFeature.class).isEnabled()) {
            buttons.get(1)
                    .add(new WynntilsMenuButton(
                            Texture.MAP_ICON,
                            true,
                            MainMapScreen.create(),
                            List.of(
                                    Component.literal("[>] ")
                                            .withStyle(ChatFormatting.GOLD)
                                            .append(Component.translatable(
                                                            "screens.wynntils.wynntilsQuestBook.mainMap.name")
                                                    .withStyle(ChatFormatting.BOLD)
                                                    .withStyle(ChatFormatting.GOLD)),
                                    Component.translatable("screens.wynntils.wynntilsQuestBook.mainMap.description")
                                            .withStyle(ChatFormatting.GRAY),
                                    Component.literal(""),
                                    Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                            .withStyle(ChatFormatting.GREEN))));
        }
        buttons.get(1)
                .add(new WynntilsMenuButton(
                        Texture.LOOTRUN_ICON,
                        true,
                        WynntilsLootrunPathsScreen.create(),
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable(
                                                        "screens.wynntils.wynntilsQuestBook.lootruns.name")
                                                .withStyle(ChatFormatting.BOLD)
                                                .withStyle(ChatFormatting.GOLD)),
                                Component.translatable("screens.wynntils.wynntilsQuestBook.lootruns.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));
        buttons.get(1)
                .add(new WynntilsMenuButton(
                        Texture.WAYPOINT_MANAGER_ICON,
                        false,
                        PoiManagementScreen.create(),
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .withStyle(ChatFormatting.BOLD)
                                        .append(Component.translatable("screens.wynntils.map.manager.name")),
                                Component.translatable("screens.wynntils.map.manager.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));

        // endregion

        // region Row 3: Guides
        buttons.get(2)
                .add(new WynntilsMenuButton(
                        Texture.GUIDES_ICON,
                        true,
                        WynntilsGuidesListScreen.create(),
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable("screens.wynntils.wynntilsGuides.name")
                                                .withStyle(ChatFormatting.BOLD)
                                                .withStyle(ChatFormatting.GOLD)),
                                Component.translatable("screens.wynntils.wynntilsGuides.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));

        buttons.get(2)
                .add(new WynntilsMenuButton(
                        Texture.DIALOGUE_ICON,
                        false,
                        WynntilsDialogueHistoryScreen.create(),
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable(
                                                        "screens.wynntils.wynntilsQuestBook.dialogueHistory.name")
                                                .withStyle(ChatFormatting.BOLD)
                                                .withStyle(ChatFormatting.GOLD)),
                                Component.translatable("screens.wynntils.wynntilsQuestBook.dialogueHistory.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));

        buttons.get(2)
                .add(new WynntilsMenuButton(
                        Texture.FAVORITE_ICON,
                        false,
                        WynntilsStatisticsScreen.create(),
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable("screens.wynntils.statistics.name")
                                                .withStyle(ChatFormatting.BOLD)
                                                .withStyle(ChatFormatting.GOLD)),
                                Component.translatable("screens.wynntils.statistics.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));

        buttons.get(2)
                .add(new WynntilsMenuButton(
                        Texture.ITEM_LOCK,
                        false,
                        SavedItemsScreen.create(),
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable("screens.wynntils.savedItems.name")
                                                .withStyle(ChatFormatting.BOLD)
                                                .withStyle(ChatFormatting.GOLD)),
                                Component.translatable("screens.wynntils.savedItems.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));

        // endregion

        // region Row 4: Wynntils
        buttons.get(3)
                .add(new WynntilsMenuButton(
                        Texture.SETTINGS_ICON,
                        true,
                        settingsScreenInstance,
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable("screens.wynntils.wynntilsMenu.configs.name")
                                                .withStyle(ChatFormatting.BOLD)
                                                .withStyle(ChatFormatting.GOLD)),
                                Component.translatable("screens.wynntils.wynntilsMenu.configs.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));
        buttons.get(3)
                .add(new WynntilsMenuButton(
                        Texture.OVERLAYS_ICON,
                        true,
                        overlayScreenInstance,
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable(
                                                        "screens.wynntils.wynntilsMenu.overlayConfig.name")
                                                .withStyle(ChatFormatting.BOLD)
                                                .withStyle(ChatFormatting.GOLD)),
                                Component.translatable("screens.wynntils.wynntilsMenu.overlayConfig.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));

        buttons.get(3)
                .add(new WynntilsMenuButton(
                        Texture.SHARE_ICON,
                        false,
                        WynntilsCrowdSourcingSettingsScreen.create(),
                        List.of(
                                Component.literal("[>] ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.translatable(
                                                        "screens.wynntils.wynntilsMenu.crowdSourcing.name")
                                                .withStyle(ChatFormatting.BOLD)
                                                .withStyle(ChatFormatting.GOLD)),
                                Component.translatable("screens.wynntils.wynntilsMenu.crowdSourcing.description")
                                        .withStyle(ChatFormatting.GRAY),
                                Component.literal(""),
                                Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                                        .withStyle(ChatFormatting.GREEN))));

        // endregion

        assert buttons.size() == 4 && buttons.stream().allMatch(row -> row.size() <= 4);
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();
        renderBackgroundTexture(poseStack);

        // Make 0, 0 the top left corner of the rendered quest book background
        poseStack.pushPose();
        final float translationX = (this.width - Texture.CONTENT_BOOK_BACKGROUND.width()) / 2f;
        final float translationY = (this.height - Texture.CONTENT_BOOK_BACKGROUND.height()) / 2f;
        poseStack.translate(translationX, translationY, 1f);

        poseStack.pushPose();
        poseStack.translate(0, -15, 0);

        renderTitle(poseStack, I18n.get("screens.wynntils.wynntilsMenu.userProfile"));

        poseStack.popPose();

        renderVersion(poseStack);

        renderWidgets(poseStack, mouseX, mouseY);

        renderPlayerInfo(guiGraphics, mouseX, mouseY);

        poseStack.popPose();

        renderPlayer(guiGraphics, mouseX, mouseY);

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderPlayerInfo(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PoseStack poseStack = guiGraphics.pose();

        if (!Models.Guild.getGuildName().isEmpty()) {
            String rank = Models.Guild.getGuildRank().getGuildDescription();

            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            StyledText.fromString(rank + " of"),
                            Texture.CONTENT_BOOK_BACKGROUND.width() / 2f,
                            Texture.CONTENT_BOOK_BACKGROUND.width(),
                            40,
                            0,
                            CommonColors.CYAN,
                            HorizontalAlignment.CENTER,
                            TextShadow.NONE);

            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            StyledText.fromString(Models.Guild.getGuildName()),
                            Texture.CONTENT_BOOK_BACKGROUND.width() / 2f,
                            Texture.CONTENT_BOOK_BACKGROUND.width(),
                            50,
                            0,
                            CommonColors.CYAN,
                            HorizontalAlignment.CENTER,
                            TextShadow.NONE);
        }

        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromComponent(McUtils.player().getDisplayName())
                                .withoutFormatting(),
                        Texture.CONTENT_BOOK_BACKGROUND.width() / 2f,
                        Texture.CONTENT_BOOK_BACKGROUND.width(),
                        60,
                        0,
                        CommonColors.BLACK,
                        HorizontalAlignment.CENTER,
                        TextShadow.NONE);
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromString(
                                Models.Character.getClassType().getName().toUpperCase(Locale.ROOT) + " Level "
                                        + Models.CombatXp.getCombatLevel().current()),
                        Texture.CONTENT_BOOK_BACKGROUND.width() / 2f,
                        Texture.CONTENT_BOOK_BACKGROUND.width(),
                        145,
                        0,
                        CommonColors.PURPLE,
                        HorizontalAlignment.CENTER,
                        TextShadow.NONE);
        if (Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class)
                .displayOverallProgress
                .get()) {
            CappedValue progress = Models.Activity.getOverallProgress();
            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            StyledText.fromString(ChatFormatting.BLACK + "Progress: " + ChatFormatting.DARK_AQUA
                                    + progress.getPercentageInt() + "%" + ChatFormatting.BLACK + " ["
                                    + ChatFormatting.DARK_AQUA + progress + ChatFormatting.BLACK + "]"),
                            Texture.CONTENT_BOOK_BACKGROUND.width() / 2f,
                            Texture.CONTENT_BOOK_BACKGROUND.width(),
                            160,
                            0,
                            CommonColors.BLACK,
                            HorizontalAlignment.CENTER,
                            TextShadow.NONE);
        }

        String currentSplash = Services.Splash.getCurrentSplash() == null ? "" : Services.Splash.getCurrentSplash();
        StyledText[] wrappedSplash = RenderedStringUtils.wrapTextBySize(
                StyledText.fromString(currentSplash), Texture.CONTENT_BOOK_BACKGROUND.width() / 2 - 20);

        for (int i = 0; i < wrappedSplash.length; i++) {
            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            wrappedSplash[i],
                            Texture.CONTENT_BOOK_BACKGROUND.width() / 2f,
                            Texture.CONTENT_BOOK_BACKGROUND.width(),
                            Texture.CONTENT_BOOK_BACKGROUND.height() - 45 + i * (McUtils.mc().font.lineHeight + 1),
                            0,
                            CommonColors.MAGENTA,
                            HorizontalAlignment.CENTER,
                            TextShadow.NONE);
        }
    }

    private void renderPlayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        final float translationX = (this.width - Texture.CONTENT_BOOK_BACKGROUND.width()) / 2f;
        final float translationY = (this.height - Texture.CONTENT_BOOK_BACKGROUND.height()) / 2f;

        int posX = (int) (Texture.CONTENT_BOOK_BACKGROUND.width() / 2f + 50 + translationX);
        int posY = (int) (Texture.CONTENT_BOOK_BACKGROUND.height() / 2f - 40 + translationY);

        final int renderWidth = (int) (Texture.CONTENT_BOOK_BACKGROUND.width() / 2f - 100);
        final int renderHeight = 70;

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                posX,
                posY,
                posX + renderWidth,
                posY + renderHeight,
                25,
                0.4f,
                mouseX,
                mouseY,
                McUtils.player());
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        if (this.hovered == null) return false;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.hovered.clickAction().run();
        }

        return true;
    }

    private void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.hovered != null) {
            guiGraphics.renderComponentTooltip(
                    FontRenderer.getInstance().getFont(),
                    ComponentUtils.wrapTooltips(this.hovered.tooltipList(), 250),
                    mouseX,
                    mouseY);
        }
    }

    private void renderWidgets(PoseStack poseStack, int mouseX, int mouseY) {
        int rowCount = buttons.size();

        poseStack.pushPose();
        poseStack.translate(20, 50, 0);

        final int translationX = (this.width - Texture.CONTENT_BOOK_BACKGROUND.width()) / 2 + 20;
        final int translationY = (this.height - Texture.CONTENT_BOOK_BACKGROUND.height()) / 2 + 50;

        int adjustedMouseX = mouseX - translationX;
        int adjustedMouseY = mouseY - translationY;

        this.hovered = null;

        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < buttons.get(row).size(); col++) {
                final int x = col * (BUTTON_SIZE + 5);
                final int y = row * (BUTTON_SIZE + 5);

                boolean hovered = x <= adjustedMouseX
                        && x + BUTTON_SIZE >= adjustedMouseX
                        && y <= adjustedMouseY
                        && y + BUTTON_SIZE >= adjustedMouseY;

                RenderUtils.drawRect(
                        poseStack, hovered ? BUTTON_COLOR_HOVERED : BUTTON_COLOR, x, y, 0, BUTTON_SIZE, BUTTON_SIZE);
                WynntilsMenuButton button = buttons.get(row).get(col);
                Texture texture = button.buttonTexture();

                if (hovered) {
                    this.hovered = button;
                }

                if (!button.dynamicTexture()) {
                    RenderUtils.drawTexturedRect(
                            poseStack,
                            texture.resource(),
                            x + (BUTTON_SIZE - texture.width()) / 2f,
                            y + (BUTTON_SIZE - texture.height()) / 2f,
                            1,
                            texture.width(),
                            texture.height(),
                            0,
                            0,
                            texture.width(),
                            texture.height(),
                            texture.width(),
                            texture.height());
                    continue;
                }

                if (hovered) {
                    RenderUtils.drawTexturedRect(
                            poseStack,
                            texture.resource(),
                            x + (BUTTON_SIZE - texture.width()) / 2f,
                            y + (BUTTON_SIZE - texture.height() / 2f) / 2f,
                            1,
                            texture.width(),
                            texture.height() / 2f,
                            0,
                            texture.height() / 2,
                            texture.width(),
                            texture.height() / 2,
                            texture.width(),
                            texture.height());
                } else {
                    RenderUtils.drawTexturedRect(
                            poseStack,
                            texture.resource(),
                            x + (BUTTON_SIZE - texture.width()) / 2f,
                            y + (BUTTON_SIZE - texture.height() / 2f) / 2f,
                            1,
                            texture.width(),
                            texture.height() / 2f,
                            0,
                            0,
                            texture.width(),
                            texture.height() / 2,
                            texture.width(),
                            texture.height());
                }
            }
        }

        poseStack.popPose();
    }
}
