/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.RenderedStringUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.AnimationPercentage;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.ThrottledSupplier;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.SubscribeEvent;

public class CustomPlayerListOverlay extends Overlay {
    private static final Comparator<PlayerInfo> PLAYER_INFO_COMPARATOR =
            Comparator.comparing(playerInfo -> playerInfo.getProfile().getName(), String::compareToIgnoreCase);
    private static final int DISTANCE_BETWEEN_CATEGORIES = 87;
    private static final int ROLL_WIDTH = 27;
    private static final int HALF_WIDTH = 178;
    private static final int WIDTH = HALF_WIDTH * 2;
    private static final int TOTAL_WIDTH = WIDTH + ROLL_WIDTH * 2;
    private static final int MAX_WIDTH = 73;

    @Persisted
    public final Config<Integer> openingDuration = new Config<>(125);

    @Persisted
    public final Config<Boolean> showWorldInStream = new Config<>(false);

    private final AnimationPercentage animationPercentage = new AnimationPercentage(
            McUtils.options().keyPlayerList::isDown, Duration.of(openingDuration.get(), ChronoUnit.MILLIS));
    private final ThrottledSupplier<List<StyledText>> availablePlayers =
            new ThrottledSupplier<>(CustomPlayerListOverlay::getAvailablePlayers, Duration.ofMillis(250));

    public CustomPlayerListOverlay() {
        super(
                new OverlayPosition(
                        0,
                        0,
                        VerticalAlignment.TOP,
                        HorizontalAlignment.CENTER,
                        OverlayPosition.AnchorSection.TOP_MIDDLE),
                TOTAL_WIDTH,
                Texture.PLAYER_LIST_OVERLAY.height());
    }

    @SubscribeEvent
    public void onRender(RenderEvent.Pre event) {
        if (event.getType() == RenderEvent.ElementType.PLAYER_TAB_LIST) {
            event.setCanceled(true);
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        if (!McUtils.options().keyPlayerList.isDown() && animationPercentage.finishedClosingAnimation()) return;
        renderPlayerList(poseStack, animationPercentage.getAnimation());
    }

    @Override
    public void renderPreview(
            PoseStack poseStack, MultiBufferSource bufferSource, DeltaTracker deltaTracker, Window window) {
        renderPlayerList(poseStack, 1);
    }

    private static List<StyledText> getAvailablePlayers() {
        PlayerTabOverlay defaultTabList = McUtils.mc().gui.getTabList();

        return McUtils.player().connection.getListedOnlinePlayers().stream()
                .sorted(PLAYER_INFO_COMPARATOR)
                .limit(80)
                .map(defaultTabList::getNameForDisplay)
                .map(StyledText::fromComponent)
                .filter(styledText -> !styledText.contains(ChatFormatting.BOLD.toString()))
                .map(StyledText::getString)
                .map(styledText -> RenderedStringUtils.substringMaxWidth(styledText, MAX_WIDTH))
                .map(styledText -> styledText.replace(ChatFormatting.GRAY.toString(), ChatFormatting.BLACK.toString()))
                .map(StyledText::fromString)
                .toList();
    }

    private void renderPlayerList(PoseStack poseStack, double animation) {
        RenderSystem.disableDepthTest();

        if (animation < 1) {
            RenderUtils.enableScissor(
                    (int) (getRenderX() + ROLL_WIDTH + HALF_WIDTH - HALF_WIDTH * animation),
                    0,
                    (int) (WIDTH * animation),
                    McUtils.mc().getWindow().getScreenHeight());
        }

        renderBackground(poseStack);

        float currentDist = getRenderX() + ROLL_WIDTH + 55;
        float categoryStart = getRenderY() + 18;
        renderCategoryTitle(poseStack, "Friends", currentDist, categoryStart);
        currentDist += DISTANCE_BETWEEN_CATEGORIES;
        String worldCategory = Models.WorldState.onHousing()
                ? Models.WorldState.getCurrentHousingName()
                : Models.WorldState.getCurrentWorldName();

        if (!showWorldInStream.get() && Models.WorldState.isInStream()) {
            worldCategory = "-";
        }

        renderCategoryTitle(poseStack, worldCategory, currentDist, categoryStart);
        currentDist += DISTANCE_BETWEEN_CATEGORIES;
        renderCategoryTitle(poseStack, "Party", currentDist, categoryStart);
        currentDist += DISTANCE_BETWEEN_CATEGORIES;
        renderCategoryTitle(poseStack, "Guild", currentDist, categoryStart);

        renderPlayerNames(poseStack, categoryStart, availablePlayers.get());

        if (animation < 1) {
            RenderUtils.disableScissor();
        }

        float middle = getRenderX() + HALF_WIDTH + ROLL_WIDTH;
        renderRoll(poseStack, (float) (middle - ROLL_WIDTH + 2 - HALF_WIDTH * animation));
        renderRoll(poseStack, (float) (middle - 2 + HALF_WIDTH * animation));

        RenderSystem.enableDepthTest();
    }

    private void renderRoll(PoseStack poseStack, float xPos) {
        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.PLAYER_LIST_OVERLAY.resource(),
                xPos,
                getRenderY(),
                0,
                ROLL_WIDTH,
                Texture.PLAYER_LIST_OVERLAY.height(),
                0,
                0,
                ROLL_WIDTH,
                Texture.PLAYER_LIST_OVERLAY.height(),
                Texture.PLAYER_LIST_OVERLAY.width(),
                Texture.PLAYER_LIST_OVERLAY.height());
    }

    private void renderPlayerNames(PoseStack poseStack, float categoryStart, List<StyledText> players) {
        for (int i = 0; i < players.size(); i++) {
            int x = i / 19;
            int y = i % 19;

            float xPos = getRenderX() + ROLL_WIDTH + 12 + (DISTANCE_BETWEEN_CATEGORIES * x);
            float yPos = categoryStart + 14 + (10 * y);
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            players.get(i),
                            xPos,
                            yPos,
                            CustomColor.fromChatFormatting(ChatFormatting.BLACK),
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.MIDDLE,
                            TextShadow.NONE);
        }
    }

    private void renderCategoryTitle(PoseStack poseStack, String name, float currentDist, float categoryStart) {
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(name),
                        currentDist,
                        categoryStart,
                        CustomColor.fromChatFormatting(ChatFormatting.BLACK),
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NONE);
    }

    private void renderBackground(PoseStack poseStack) {
        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.PLAYER_LIST_OVERLAY.resource(),
                getRenderX() + ROLL_WIDTH,
                getRenderY(),
                0,
                Texture.PLAYER_LIST_OVERLAY.width() - ROLL_WIDTH,
                Texture.PLAYER_LIST_OVERLAY.height(),
                ROLL_WIDTH,
                0,
                Texture.PLAYER_LIST_OVERLAY.width() - ROLL_WIDTH,
                Texture.PLAYER_LIST_OVERLAY.height(),
                Texture.PLAYER_LIST_OVERLAY.width(),
                Texture.PLAYER_LIST_OVERLAY.height());
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {
        animationPercentage.setOpeningDuration(Duration.of(openingDuration.get(), ChronoUnit.MILLIS));
    }
}
