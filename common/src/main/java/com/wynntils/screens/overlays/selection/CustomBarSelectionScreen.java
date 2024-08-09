/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.overlays.selection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.screens.WynntilsScreen;
import com.wynntils.core.persisted.config.OverlayGroupHolder;
import com.wynntils.features.overlays.CustomBarsOverlayFeature;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.BarTexture;
import com.wynntils.utils.render.type.HealthTexture;
import com.wynntils.utils.render.type.ManaTexture;
import com.wynntils.utils.render.type.ObjectivesTextures;
import com.wynntils.utils.render.type.UniversalTexture;
import com.wynntils.utils.type.Pair;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public final class CustomBarSelectionScreen extends WynntilsScreen {
    // The order of these matters, they must match that in CustomBarsOverlayFeature
    private static final List<Pair<Texture, List<BarTexture>>> availableBars = List.of(
            Pair.of(Texture.UNIVERSAL_BAR, List.of(UniversalTexture.values())),
            Pair.of(Texture.HEALTH_BAR, List.of(HealthTexture.values())),
            Pair.of(Texture.MANA_BAR, List.of(ManaTexture.values())),
            Pair.of(Texture.EXPERIENCE_BAR, List.of(ObjectivesTextures.values())),
            Pair.of(Texture.BUBBLE_BAR, List.of(ObjectivesTextures.values())));

    private final OverlaySelectionScreen previousScreen;

    private Button textureButton;
    private float barX;
    private float barY;
    private int barTextureIndex = 0;
    private int barTypeIndex = 0;

    private CustomBarSelectionScreen(OverlaySelectionScreen previousScreen) {
        super(Component.translatable("Custom Bar Overlay Selection Screen"));

        this.previousScreen = previousScreen;
    }

    public static Screen create(OverlaySelectionScreen previousScreen) {
        return new CustomBarSelectionScreen(previousScreen);
    }

    @Override
    protected void doInit() {
        calculateBarPosition();

        textureButton = this.addRenderableWidget(new Button.Builder(
                        Component.literal(availableBars
                                .get(barTypeIndex)
                                .b()
                                .get(barTextureIndex)
                                .toString()),
                        (b) -> {})
                .pos((int) ((this.width / 2f) - 30), (int) (barY + 20))
                .size(60, 20)
                .tooltip(Tooltip.create(Component.translatable("screens.wynntils.customBarSelection.textureTooltip")))
                .build());

        this.addRenderableWidget(new Button.Builder(Component.literal("🠜"), (button) -> scrollBars(-1))
                .pos((int) (this.width / 2f) - 120, (int) (barY + 45))
                .size(20, 20)
                .build());

        this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.customBarSelection.cancel"), (button) -> onClose())
                .pos((int) (this.width / 2f) - 90, (int) (barY + 45))
                .size(80, 20)
                .build());

        this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.customBarSelection.select"),
                        (button) -> addCustomBar())
                .pos((int) (this.width / 2f) + 10, (int) (barY + 45))
                .size(80, 20)
                .build());

        this.addRenderableWidget(new Button.Builder(Component.literal("🠞"), (button) -> scrollBars(1))
                .pos((int) (this.width / 2f) + 100, (int) (barY + 45))
                .size(20, 20)
                .build());
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.doRender(guiGraphics, mouseX, mouseY, partialTick);
        PoseStack poseStack = guiGraphics.pose();

        // Draw the custom bar centered in the screen with 50% progress
        BufferedRenderUtils.drawProgressBar(
                poseStack,
                guiGraphics.bufferSource(),
                availableBars.get(barTypeIndex).a(),
                barX,
                barY,
                barX + availableBars.get(barTypeIndex).a().width(),
                barY + availableBars.get(barTypeIndex).b().get(barTextureIndex).getHeight(),
                0,
                availableBars.get(barTypeIndex).b().get(barTextureIndex).getTextureY1(),
                availableBars.get(barTypeIndex).a().width(),
                availableBars.get(barTypeIndex).b().get(barTextureIndex).getTextureY2(),
                0.5f);
    }

    @Override
    public void onClose() {
        McUtils.mc().setScreen(previousScreen);
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        for (GuiEventListener listener : this.children) {
            if (listener.isMouseOver(mouseX, mouseY)) {
                // Special case for the texture button to handle both
                // left and right clicks
                if (listener == textureButton) {
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        scrollTextures(1);
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        return true;
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        scrollTextures(-1);
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        return true;
                    }

                    return false;
                } else {
                    return listener.mouseClicked(mouseX, mouseY, button);
                }
            }
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double scrollValue = -Math.signum(deltaY);

        scrollBars((int) scrollValue);

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void calculateBarPosition() {
        // Calculate the central position to render the current selected bar at
        barX = (this.width - availableBars.get(barTypeIndex).a().width()) / 2f;
        barY = (this.height
                        - availableBars
                                .get(barTypeIndex)
                                .b()
                                .get(barTextureIndex)
                                .getHeight())
                / 2f;
    }

    private void scrollBars(int direction) {
        // Loop around the different bar options
        if (barTypeIndex + direction > availableBars.size() - 1) {
            barTypeIndex = 0;
        } else if (barTypeIndex + direction < 0) {
            barTypeIndex = availableBars.size() - 1;
        } else {
            barTypeIndex += direction;
        }

        // Reset back to the default texture for new bar
        barTextureIndex = 0;

        // Change texture button message to the texture name
        textureButton.setMessage(Component.literal(
                availableBars.get(barTypeIndex).b().get(barTextureIndex).toString()));

        calculateBarPosition();
    }

    private void scrollTextures(int direction) {
        // Loop around the different bar textures
        if (barTextureIndex + direction > availableBars.get(barTypeIndex).b().size() - 1) {
            barTextureIndex = 0;
        } else if (barTextureIndex + direction < 0) {
            barTextureIndex = availableBars.get(barTypeIndex).b().size() - 1;
        } else {
            barTextureIndex += direction;
        }

        // Change texture button message to the texture name
        textureButton.setMessage(Component.literal(
                availableBars.get(barTypeIndex).b().get(barTextureIndex).toString()));

        calculateBarPosition();
    }

    private void addCustomBar() {
        Feature customBarsFeature = Managers.Feature.getFeatureInstance(CustomBarsOverlayFeature.class);

        // Get the group holder for the selected bar type
        List<OverlayGroupHolder> groupHolders = Managers.Overlay.getFeatureOverlayGroups(customBarsFeature);
        OverlayGroupHolder barGroup = groupHolders.get(barTypeIndex);

        // Add the new custom bar
        int id = Managers.Overlay.extendOverlayGroup(barGroup);

        Managers.Config.loadConfigOptions(true, false);
        Managers.Config.saveConfig();
        Managers.Config.reloadConfiguration();

        // Repopulate the overlays on selection screen
        previousScreen.populateOverlays();

        // Set the new custom bar as selected for easier editing access
        previousScreen.setSelectedOverlay(barGroup.getOverlays().getLast());

        McUtils.sendMessageToClient(Component.translatable(
                        "screens.wynntils.overlaySelection.createdOverlay",
                        barGroup.getOverlayClass().getSimpleName(),
                        barGroup.getFieldName(),
                        id)
                .withStyle(ChatFormatting.GREEN));

        // Return to overlay selection screen
        onClose();
    }
}
