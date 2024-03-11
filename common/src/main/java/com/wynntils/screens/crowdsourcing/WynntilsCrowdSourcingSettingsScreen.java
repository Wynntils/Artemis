/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.crowdsourcing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.consumers.screens.WynntilsScreen;
import com.wynntils.core.crowdsource.type.CrowdSourcedDataType;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.TextboxScreen;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.screens.base.widgets.BasicTexturedButton;
import com.wynntils.screens.base.widgets.SearchWidget;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.screens.crowdsourcing.widgets.CrowdSourcedDataWidget;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class WynntilsCrowdSourcingSettingsScreen extends WynntilsScreen implements TextboxScreen {
    private static final int MAX_WIDGETS_PER_PAGE = 6;
    private static final int SCROLLBAR_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_RENDER_X = 162;

    private final SearchWidget searchWidget;

    private BasicTexturedButton infoButton;
    private boolean draggingScroll = false;
    private double currentUnusedScroll = 0;
    private float translationX;
    private float translationY;
    private int scrollOffset = 0;
    private int scrollRenderY;
    private List<CrowdSourcedDataWidget> crowdSourceWidgets = new ArrayList<>();
    private List<CrowdSourcedDataType> dataTypes = new ArrayList<>();
    private TextInputBoxWidget focusedTextInput;

    private WynntilsCrowdSourcingSettingsScreen() {
        super(Component.translatable("screens.wynntils.wynntilsCrowdSourcing.name"));

        searchWidget = new SearchWidget(
                20,
                238,
                140,
                20,
                (s) -> {
                    scrollOffset = 0;
                    populateWidgets();
                },
                this);

        setFocusedTextInput(searchWidget);
    }

    public static WynntilsCrowdSourcingSettingsScreen create() {
        return new WynntilsCrowdSourcingSettingsScreen();
    }

    @Override
    protected void doInit() {
        translationX = (this.width - Texture.CROWD_SOURCE_BACKGROUND.width()) / 2f;
        translationY = (this.height - Texture.CROWD_SOURCE_BACKGROUND.height()) / 2f;

        infoButton = new BasicTexturedButton(
                5,
                -5,
                Texture.CROWD_SOURCE_STICKER.width(),
                Texture.CROWD_SOURCE_STICKER.height(),
                Texture.CROWD_SOURCE_STICKER,
                (b) -> {},
                List.of(Component.translatable("screens.wynntils.wynntilsCrowdSourcing.screenDescription")));

        this.addRenderableWidget(infoButton);

        this.addRenderableWidget(searchWidget);

        populateWidgets();
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(translationX, translationY, 0);

        // Adjust mouse for posestack translation
        int adjustedMouseX = mouseX - (int) translationX;
        int adjustedMouseY = mouseY - (int) translationY;

        RenderUtils.drawTexturedRect(poseStack, Texture.CROWD_SOURCE_BACKGROUND, 0, 0);

        infoButton.render(guiGraphics, adjustedMouseX, adjustedMouseY, partialTick);
        searchWidget.render(guiGraphics, adjustedMouseX, adjustedMouseY, partialTick);

        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromComponent(Component.translatable("screens.wynntils.wynntilsCrowdSourcing.name")),
                        10,
                        170,
                        48,
                        60,
                        160,
                        CommonColors.LIGHT_GRAY,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.TOP,
                        TextShadow.NORMAL,
                        1.5f);

        RenderUtils.drawLine(poseStack, CommonColors.DARK_GRAY, 20, 61, 160, 61, 0, 1);

        if (dataTypes.size() > MAX_WIDGETS_PER_PAGE) {
            renderScrollBar(poseStack);
        } else if (crowdSourceWidgets.isEmpty()) {
            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            StyledText.fromComponent(
                                    Component.translatable("screens.wynntils.wynntilsCrowdSourcing.noDataTypes")),
                            10,
                            170,
                            60,
                            220,
                            140,
                            CommonColors.LIGHT_GRAY,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            TextShadow.NORMAL);
        }

        renderWidgets(guiGraphics, adjustedMouseX, adjustedMouseY, partialTick);

        renderTooltips(guiGraphics, adjustedMouseX, adjustedMouseY);

        poseStack.popPose();
    }

    @Override
    public void added() {
        searchWidget.opened();
        super.added();
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        double adjustedMouseX = mouseX - translationX;
        double adjustedMouseY = mouseY - translationY;

        if (!draggingScroll && dataTypes.size() > MAX_WIDGETS_PER_PAGE) {
            if (MathUtils.isInside(
                    (int) adjustedMouseX,
                    (int) adjustedMouseY,
                    SCROLLBAR_RENDER_X,
                    SCROLLBAR_RENDER_X + SCROLLBAR_WIDTH,
                    scrollRenderY,
                    scrollRenderY + SCROLLBAR_HEIGHT)) {
                draggingScroll = true;

                return true;
            }
        }

        for (GuiEventListener listener : getWidgetsForIteration().toList()) {
            if (listener.isMouseOver(adjustedMouseX, adjustedMouseY)) {
                return listener.mouseClicked(adjustedMouseX, adjustedMouseY, button);
            }
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double adjustedMouseX = mouseX - translationX;
        double adjustedMouseY = mouseY - translationY;

        if (draggingScroll) {
            int renderY = 61;
            int scrollAreaStartY = renderY + 10;

            int newValue = Math.round(MathUtils.map(
                    (float) adjustedMouseY,
                    scrollAreaStartY,
                    scrollAreaStartY + MAX_WIDGETS_PER_PAGE * 28 - SCROLLBAR_HEIGHT,
                    0,
                    Math.max(0, dataTypes.size() - MAX_WIDGETS_PER_PAGE)));

            scroll(newValue - scrollOffset);

            return super.mouseDragged(adjustedMouseX, adjustedMouseY, button, dragX, dragY);
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double adjustedMouseX = mouseX - translationX;
        double adjustedMouseY = mouseY - translationY;

        for (GuiEventListener listener : getWidgetsForIteration().toList()) {
            if (listener.isMouseOver(adjustedMouseX, adjustedMouseY)) {
                listener.mouseReleased(adjustedMouseX, adjustedMouseY, button);
            }
        }

        draggingScroll = false;

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (dataTypes.size() > MAX_WIDGETS_PER_PAGE) {
            if (Math.abs(deltaY) == 1.0) {
                scroll((int) -deltaY);
                return true;
            }

            // Account for scrollpad
            currentUnusedScroll -= deltaY / 5d;

            if (Math.abs(currentUnusedScroll) < 1) return true;

            int scroll = (int) (currentUnusedScroll);
            currentUnusedScroll = currentUnusedScroll % 1;

            scroll(scroll);
        }

        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return focusedTextInput != null && focusedTextInput.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        return focusedTextInput != null && focusedTextInput.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public TextInputBoxWidget getFocusedTextInput() {
        return focusedTextInput;
    }

    @Override
    public void setFocusedTextInput(TextInputBoxWidget focusedTextInput) {
        this.focusedTextInput = focusedTextInput;
    }

    private void populateWidgets() {
        crowdSourceWidgets = new ArrayList<>();

        // Get all data types and filter them based on search query
        dataTypes = Stream.of(CrowdSourcedDataType.values())
                .filter(crowdSourcedDataType -> searchMatches(crowdSourcedDataType.getTranslatedName()))
                .toList();

        int currentDataType;
        int renderY = 62;

        for (int i = 0; i < MAX_WIDGETS_PER_PAGE; i++) {
            currentDataType = i + scrollOffset;

            if (currentDataType > dataTypes.size() - 1) {
                break;
            }

            crowdSourceWidgets.add(new CrowdSourcedDataWidget(
                    20, renderY, dataTypes.get(currentDataType), translationX, translationY));

            renderY += 28;
        }
    }

    private void scroll(int delta) {
        scrollOffset = MathUtils.clamp(scrollOffset + delta, 0, Math.max(0, dataTypes.size() - MAX_WIDGETS_PER_PAGE));

        populateWidgets();
    }

    private boolean searchMatches(String translatedName) {
        return StringUtils.partialMatch(translatedName, searchWidget.getTextBoxInput());
    }

    private Stream<GuiEventListener> getWidgetsForIteration() {
        return Stream.concat(this.children.stream(), crowdSourceWidgets.stream());
    }

    private void renderWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (CrowdSourcedDataWidget crowdSourcedDataWidget : crowdSourceWidgets) {
            crowdSourcedDataWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderScrollBar(PoseStack poseStack) {
        RenderUtils.drawRect(poseStack, CommonColors.LIGHT_GRAY, SCROLLBAR_RENDER_X, 61, 0, SCROLLBAR_WIDTH, 168);

        scrollRenderY = (int) (61
                + MathUtils.map(
                        scrollOffset,
                        0,
                        dataTypes.size() - MAX_WIDGETS_PER_PAGE,
                        0,
                        MAX_WIDGETS_PER_PAGE * 28 - SCROLLBAR_HEIGHT));

        RenderUtils.drawRect(
                poseStack,
                draggingScroll ? CommonColors.BLACK : CommonColors.GRAY,
                SCROLLBAR_RENDER_X,
                scrollRenderY,
                0,
                SCROLLBAR_WIDTH,
                SCROLLBAR_HEIGHT);
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (GuiEventListener child : this.children) {
            if (child instanceof TooltipProvider tooltipProvider && child.isMouseOver(mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(
                        FontRenderer.getInstance().getFont(), tooltipProvider.getTooltipLines(), mouseX, mouseY);
                break;
            }
        }
    }
}
