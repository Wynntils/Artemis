/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.activities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.ui.WynntilsContentBookFeature;
import com.wynntils.models.activities.discoveries.DiscoveryInfo;
import com.wynntils.models.activities.event.ActivityUpdatedEvent;
import com.wynntils.models.activities.type.ActivitySortOrder;
import com.wynntils.screens.activities.widgets.DiscoveryButton;
import com.wynntils.screens.activities.widgets.DiscoveryProgressButton;
import com.wynntils.screens.base.WynntilsListScreen;
import com.wynntils.screens.base.widgets.BackButton;
import com.wynntils.screens.base.widgets.FilterButton;
import com.wynntils.screens.base.widgets.PageSelectorButton;
import com.wynntils.screens.base.widgets.ReloadButton;
import com.wynntils.screens.base.widgets.SortOrderWidget;
import com.wynntils.screens.base.widgets.SortableActivityScreen;
import com.wynntils.screens.wynntilsmenu.WynntilsMenuScreen;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;

public final class WynntilsDiscoveriesScreen extends WynntilsListScreen<DiscoveryInfo, DiscoveryButton>
        implements SortableActivityScreen {
    private final List<FilterButton> filterButtons = new ArrayList<>();

    private ActivitySortOrder activitySortOrder = ActivitySortOrder.LEVEL;

    private WynntilsDiscoveriesScreen() {
        super(Component.translatable("screens.wynntils.wynntilsDiscoveries.name"));

        // Only register this once
        WynntilsMod.registerEventListener(this);
    }

    public static Screen create() {
        return new WynntilsDiscoveriesScreen();
    }

    @SubscribeEvent
    public void onDiscoveryUpdate(ActivityUpdatedEvent event) {
        if (event.getActivityType().isDiscovery() && McUtils.mc().screen == this) {
            this.reloadElements();
        }
    }

    // Called when the screen is closed
    @Override
    public void onClose() {
        cleanupOnClose();

        super.onClose();
    }

    // Called when the screen is "overwritten" by another screen
    @Override
    public void removed() {
        cleanupOnClose();

        super.removed();
    }

    private void cleanupOnClose() {
        WynntilsMod.unregisterEventListener(this);

        if (Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class)
                .cancelAllQueriesOnScreenClose
                .get()) {
            Handlers.ContainerQuery.endAllQueries();
        }
    }

    @Override
    protected void doInit() {
        Models.Discovery.reloadDiscoveries(shouldQuerySecrets(), shouldQueryWorld(), shouldQueryTerritory());

        super.doInit();

        filterButtons.clear();

        filterButtons.add(new FilterButton(
                35,
                125,
                30,
                30,
                Texture.DISCOVERED_TERRITORY,
                true,
                List.of(Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.showFoundTerritory.name")
                                .withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GREEN))),
                () -> {
                    Storage<Boolean> territorySelected =
                            Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class).territorySelected;
                    territorySelected.store(!territorySelected.get());
                    reloadElements();

                    // Scan territories, if it's the first time we're showing them
                    if (territorySelected.get()) {
                        Models.Discovery.reloadDiscoveries(false, false, true);
                    }
                },
                this::isShowingTerritory));
        filterButtons.add(new FilterButton(
                70,
                125,
                30,
                30,
                Texture.DISCOVERED_WORLD,
                true,
                List.of(Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.showFoundWorld.name")
                                .withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GREEN))),
                () -> {
                    Storage<Boolean> worldSelected =
                            Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class).worldSelected;
                    worldSelected.store(!worldSelected.get());
                    reloadElements();

                    // Scan world discoveries, if it's the first time we're showing them
                    if (worldSelected.get()) {
                        Models.Discovery.reloadDiscoveries(false, true, false);
                    }
                },
                this::isShowingWorld));
        filterButtons.add(new FilterButton(
                105,
                125,
                30,
                30,
                Texture.DISCOVERED_SECRET,
                true,
                List.of(Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.showFoundSecret.name")
                                .withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GREEN))),
                () -> {
                    Storage<Boolean> secretsSelected =
                            Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class).secretsSelected;
                    secretsSelected.store(!secretsSelected.get());
                    reloadElements();

                    // Scan secret discoveries, if it's the first time we're showing them
                    if (secretsSelected.get()) {
                        Models.Discovery.reloadDiscoveries(true, false, false);
                    }
                },
                this::isShowingSecrets));
        filterButtons.add(new FilterButton(
                35,
                160,
                30,
                30,
                Texture.UNDISCOVERED_TERRITORY,
                true,
                List.of(Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.showUnfoundTerritory.name")
                                .withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GREEN))),
                () -> {
                    Storage<Boolean> undiscoveredTerritorySelected = Managers.Feature.getFeatureInstance(
                                    WynntilsContentBookFeature.class)
                            .undiscoveredTerritorySelected;
                    undiscoveredTerritorySelected.store(!undiscoveredTerritorySelected.get());
                    reloadElements();
                },
                this::isShowingUndiscoveredTerritory));
        filterButtons.add(new FilterButton(
                70,
                160,
                30,
                30,
                Texture.UNDISCOVERED_WORLD,
                true,
                List.of(Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.showUnfoundWorld.name")
                                .withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GREEN))),
                () -> {
                    Storage<Boolean> undiscoveredWorldSelected = Managers.Feature.getFeatureInstance(
                                    WynntilsContentBookFeature.class)
                            .undiscoveredWorldSelected;
                    undiscoveredWorldSelected.store(!undiscoveredWorldSelected.get());
                    reloadElements();
                },
                this::isShowingUndiscoveredWorld));
        filterButtons.add(new FilterButton(
                105,
                160,
                30,
                30,
                Texture.UNDISCOVERED_SECRET,
                true,
                List.of(Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.showUnfoundSecret.name")
                                .withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GREEN))),
                () -> {
                    Storage<Boolean> undiscoveredSecretsSelected = Managers.Feature.getFeatureInstance(
                                    WynntilsContentBookFeature.class)
                            .undiscoveredSecretsSelected;
                    undiscoveredSecretsSelected.store(!undiscoveredSecretsSelected.get());
                    reloadElements();
                },
                this::isShowingUndiscoveredSecrets));

        this.addRenderableWidget(new BackButton(
                (int) ((Texture.CONTENT_BOOK_BACKGROUND.width() / 2f - 16) / 2f),
                65,
                Texture.BACK_ARROW_OFFSET.width() / 2,
                Texture.BACK_ARROW_OFFSET.height(),
                WynntilsMenuScreen.create()));

        this.addRenderableWidget(new ReloadButton(
                Texture.CONTENT_BOOK_BACKGROUND.width() - 21,
                11,
                (int) (Texture.RELOAD_ICON_OFFSET.width() / 2 / 1.7f),
                (int) (Texture.RELOAD_ICON_OFFSET.height() / 1.7f),
                "discovery",
                () -> Models.Discovery.reloadDiscoveries(
                        shouldQuerySecrets(), shouldQueryWorld(), shouldQueryTerritory())));

        this.addRenderableWidget(new SortOrderWidget(
                Texture.CONTENT_BOOK_BACKGROUND.width() / 2 + 1,
                11,
                (int) (Texture.SORT_DISTANCE_OFFSET.width() / 1.7f),
                (int) (Texture.SORT_DISTANCE_OFFSET.height() / 2 / 1.7f),
                this));

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

        for (FilterButton filterButton : filterButtons) {
            this.addRenderableWidget(filterButton);
        }

        this.addRenderableWidget(new DiscoveryProgressButton(50, 10, 20, 20, false));
        this.addRenderableWidget(new DiscoveryProgressButton(75, 10, 20, 20, true));
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

        renderTitle(poseStack, I18n.get("screens.wynntils.wynntilsDiscoveries.name"));

        renderVersion(poseStack);

        renderWidgets(guiGraphics, mouseX, mouseY, partialTick);

        if (elements.isEmpty()) {
            renderNoDiscoveries(poseStack);
        }

        renderDescription(
                poseStack,
                I18n.get("screens.wynntils.wynntilsDiscoveries.screenDescription"),
                I18n.get("screens.wynntils.wynntilsActivities.filterHelper"));

        renderPageInfo(poseStack, currentPage + 1, maxPage + 1);

        poseStack.popPose();

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static void renderNoDiscoveries(PoseStack poseStack) {
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.wynntilsDiscoveries.noDiscoveries")),
                        Texture.CONTENT_BOOK_BACKGROUND.width() / 2f + 15f,
                        Texture.CONTENT_BOOK_BACKGROUND.width() - 15f,
                        0,
                        Texture.CONTENT_BOOK_BACKGROUND.height(),
                        Texture.CONTENT_BOOK_BACKGROUND.width() / 2f - 30f,
                        CommonColors.BLACK,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NONE);
    }

    @Override
    protected DiscoveryButton getButtonFromElement(int i) {
        int offset = i % getElementsPerPage();
        return new DiscoveryButton(
                Texture.CONTENT_BOOK_BACKGROUND.width() / 2 + 15,
                offset * 13 + 25,
                Texture.CONTENT_BOOK_BACKGROUND.width() / 2 - 37,
                9,
                elements.get(i),
                this);
    }

    @Override
    protected void reloadElementsList(String searchTerm) {
        // We need to filter duplicates
        elements.addAll(Stream.concat(
                        Models.Discovery.getAllDiscoveries(activitySortOrder)
                                .filter(discoveryInfo -> !discoveryInfo.discovered())
                                .filter(discoveryInfo -> switch (discoveryInfo.type()) {
                                    case TERRITORY -> isShowingUndiscoveredTerritory();
                                    case WORLD -> isShowingUndiscoveredWorld();
                                    case SECRET -> isShowingUndiscoveredSecrets();
                                }),
                        Models.Discovery.getAllCompletedDiscoveries(activitySortOrder)
                                .filter(discoveryInfo -> switch (discoveryInfo.type()) {
                                    case TERRITORY -> isShowingTerritory();
                                    case WORLD -> isShowingWorld();
                                    case SECRET -> isShowingSecrets();
                                }))
                .filter(info -> StringUtils.partialMatch(info.name(), searchTerm))
                .toList());
    }

    @Override
    public ActivitySortOrder getActivitySortOrder() {
        return activitySortOrder;
    }

    @Override
    public void setActivitySortOrder(ActivitySortOrder newSortOrder) {
        if (newSortOrder == null) {
            throw new IllegalStateException("Tried to set null activity sort order");
        }

        // Disable DISTANCE sorting for discoveries
        if (newSortOrder == ActivitySortOrder.DISTANCE) {
            newSortOrder = ActivitySortOrder.ALPHABETIC;
        }

        this.activitySortOrder = newSortOrder;
        this.setCurrentPage(0);
    }

    private boolean isShowingSecrets() {
        return Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class)
                .secretsSelected
                .get();
    }

    private boolean isShowingUndiscoveredSecrets() {
        return Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class)
                .undiscoveredSecretsSelected
                .get();
    }

    private boolean shouldQuerySecrets() {
        return isShowingSecrets() || isShowingUndiscoveredSecrets();
    }

    private boolean isShowingWorld() {
        return Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class)
                .worldSelected
                .get();
    }

    private boolean isShowingUndiscoveredWorld() {
        return Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class)
                .undiscoveredWorldSelected
                .get();
    }

    private boolean shouldQueryWorld() {
        return isShowingWorld() || isShowingUndiscoveredWorld();
    }

    private boolean isShowingTerritory() {
        return Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class)
                .territorySelected
                .get();
    }

    private boolean isShowingUndiscoveredTerritory() {
        return Managers.Feature.getFeatureInstance(WynntilsContentBookFeature.class)
                .undiscoveredTerritorySelected
                .get();
    }

    private boolean shouldQueryTerritory() {
        return isShowingTerritory() || isShowingUndiscoveredTerritory();
    }
}
