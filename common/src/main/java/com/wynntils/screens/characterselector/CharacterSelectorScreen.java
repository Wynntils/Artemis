/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.characterselector;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.screens.WynntilsScreen;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.character.type.ClassInfo;
import com.wynntils.screens.characterselector.widgets.ChangeWorldButton;
import com.wynntils.screens.characterselector.widgets.ClassInfoButton;
import com.wynntils.screens.characterselector.widgets.ClassSelectionAddButton;
import com.wynntils.screens.characterselector.widgets.ClassSelectionDeleteButton;
import com.wynntils.screens.characterselector.widgets.ClassSelectionEditButton;
import com.wynntils.screens.characterselector.widgets.DisconnectButton;
import com.wynntils.screens.characterselector.widgets.PlayButton;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.wynn.ContainerUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class CharacterSelectorScreen extends WynntilsScreen {
    private static final int CHARACTER_INFO_PER_PAGE = 7;

    private final AbstractContainerScreen<?> actualClassSelectionScreen;
    private List<ClassInfo> classInfoList = new ArrayList<>();
    private final List<ClassInfoButton> classInfoButtons = new ArrayList<>();
    private int firstNewCharacterSlot = -1;
    private float currentTextureScale = 1f;

    private int scrollOffset = 0;
    private boolean draggingScroll = false;
    private double lastMouseY = 0;
    private double mouseDrag = 0;
    private ClassInfoButton selected = null;

    private CharacterSelectorScreen(AbstractContainerScreen<?> classSelectionScreen) {
        super(Component.translatable("screens.wynntils.characterSelection.name"));

        actualClassSelectionScreen = classSelectionScreen;
    }

    public static Screen create(AbstractContainerScreen<?> classSelectionScreen) {
        return new CharacterSelectorScreen(classSelectionScreen);
    }

    @Override
    public void onClose() {
        ContainerUtils.closeContainer(actualClassSelectionScreen.getMenu().containerId);
        super.onClose();
    }

    @Override
    protected void doInit() {
        currentTextureScale = (float) this.height / Texture.CHARACTER_LIST_BACKGROUND.height();

        float listWidth = Texture.CHARACTER_LIST_BACKGROUND.width() * currentTextureScale;

        float playButtonWidth = Texture.PLAY_BUTTON.width() * currentTextureScale;
        float playButtonHeight = Texture.PLAY_BUTTON.height() / 2f * currentTextureScale;
        this.addRenderableWidget(new PlayButton(
                (int) (this.width - playButtonWidth - (10f * currentTextureScale)),
                (int) (this.height - playButtonHeight - (10f * currentTextureScale)),
                (int) playButtonWidth,
                (int) playButtonHeight,
                this));

        float deleteButtonWidth = Texture.REMOVE_ICON_OFFSET.width() * currentTextureScale;
        float deleteButtonHeight = Texture.REMOVE_ICON_OFFSET.height() / 2f * currentTextureScale;
        this.addRenderableWidget(new ClassSelectionDeleteButton(
                (int) (listWidth * 0.6f),
                (int) (this.height * 0.915f),
                (int) deleteButtonWidth,
                (int) deleteButtonHeight,
                this));

        float editButtonWidth = Texture.EDIT_ICON.width() * currentTextureScale;
        float editButtonHeight = Texture.EDIT_ICON.height() * currentTextureScale;
        this.addRenderableWidget(new ClassSelectionEditButton(
                (int) (listWidth * 0.44f),
                (int) (this.height * 0.915f),
                (int) editButtonWidth,
                (int) editButtonHeight,
                this));

        float addButtonWidth = Texture.ADD_ICON_OFFSET.width() * currentTextureScale;
        float addButtonHeight = Texture.ADD_ICON_OFFSET.height() / 2f * currentTextureScale;
        this.addRenderableWidget(new ClassSelectionAddButton(
                (int) (listWidth * 0.22f),
                (int) (this.height * 0.915f),
                (int) addButtonWidth,
                (int) addButtonHeight,
                this));

        float disconnectButtonWidth = Texture.DISCONNECT_BUTTON.width() * currentTextureScale;
        float disconnectButtonHeight = Texture.DISCONNECT_BUTTON.height() / 2f * currentTextureScale;
        this.addRenderableWidget(new DisconnectButton(
                (int) (this.width - disconnectButtonWidth - (10f * currentTextureScale)),
                (int) (10f * currentTextureScale),
                (int) disconnectButtonWidth,
                (int) disconnectButtonHeight,
                this));

        float changeWorldButtonWidth = Texture.CHANGE_WORLD_BUTTON.width() * currentTextureScale;
        float changeWorldButtonHeight = Texture.CHANGE_WORLD_BUTTON.height() / 2f * currentTextureScale;
        this.addRenderableWidget(new ChangeWorldButton(
                (int) (this.width - changeWorldButtonWidth - (10f * currentTextureScale)),
                (int) ((15f * currentTextureScale) + changeWorldButtonHeight),
                (int) changeWorldButtonWidth,
                (int) changeWorldButtonHeight,
                this));

        reloadButtons();
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (draggingScroll) {
            mouseDrag += mouseY - lastMouseY;
            lastMouseY = mouseY;

            if (Math.abs(mouseDrag) > this.height / CHARACTER_INFO_PER_PAGE) {
                boolean positive = mouseDrag > 0;

                mouseDrag += (positive ? -1 : 1) * this.height / CHARACTER_INFO_PER_PAGE;
                setScrollOffset(positive ? -1 : 1);
            }
        }

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        PoseStack poseStack = guiGraphics.pose();
        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.CHARACTER_LIST_BACKGROUND.resource(),
                0,
                0,
                0,
                Texture.CHARACTER_LIST_BACKGROUND.width() * currentTextureScale,
                Texture.CHARACTER_LIST_BACKGROUND.height() * currentTextureScale,
                Texture.CHARACTER_LIST_BACKGROUND.width(),
                Texture.CHARACTER_LIST_BACKGROUND.height());

        renderWidgets(guiGraphics, mouseX, mouseY, partialTick);

        renderScrollButton(poseStack);

        renderPlayer(guiGraphics, mouseX, mouseY);

        if (selected == null) return;

        renderCharacterInfo(poseStack);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.BACKGROUND_SPLASH.resource(),
                0,
                0,
                0,
                this.width,
                this.height,
                Texture.BACKGROUND_SPLASH.width(),
                Texture.BACKGROUND_SPLASH.height());
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        for (GuiEventListener child : children()) {
            child.mouseClicked(mouseX, mouseY, button);
        }

        for (ClassInfoButton classInfoButton : classInfoButtons) {
            if (classInfoButton.isMouseOver(mouseX, mouseY)) {
                classInfoButton.mouseClicked(mouseX, mouseY, button);
                this.selected = classInfoButton;
            }
        }

        if (!draggingScroll) {
            float scrollButtonRenderX = Texture.CHARACTER_LIST_BACKGROUND.width() * currentTextureScale * 0.916f;
            float scrollButtonRenderY = MathUtils.map(
                    scrollOffset,
                    0,
                    classInfoList.size() - CHARACTER_INFO_PER_PAGE,
                    Texture.CHARACTER_LIST_BACKGROUND.height() * currentTextureScale * 0.01f,
                    Texture.CHARACTER_LIST_BACKGROUND.height() * currentTextureScale * 0.92f);

            if (mouseX >= scrollButtonRenderX
                    && mouseX <= scrollButtonRenderX + Texture.SCROLL_BUTTON.width() * currentTextureScale
                    && mouseY >= scrollButtonRenderY
                    && mouseY <= scrollButtonRenderY + Texture.SCROLL_BUTTON.height() * currentTextureScale) {
                draggingScroll = true;
                lastMouseY = mouseY;
            }
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        setScrollOffset((int) deltaY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        KeyMapping[] keyHotbarSlots = McUtils.options().keyHotbarSlots;

        for (int i = 0; i < Math.min(keyHotbarSlots.length, classInfoList.size()); i++) {
            if (!keyHotbarSlots[i].matches(keyCode, scanCode)) continue;
            int slot = classInfoList.get(i).slot();
            Models.CharacterSelection.playWithCharacter(slot);
            return true;
        }

        return true;
    }

    private void renderCharacterInfo(PoseStack poseStack) {
        float renderWidth = Texture.CHARACTER_INFO.width() * currentTextureScale;
        float renderHeight = Texture.CHARACTER_INFO.height() * currentTextureScale;
        float renderX = (this.width * 0.6f) - renderWidth / 2f;
        float renderY = this.height / 8f;

        poseStack.pushPose();
        poseStack.translate(renderX, renderY, 0);

        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.CHARACTER_INFO.resource(),
                0,
                0,
                0,
                renderWidth,
                renderHeight,
                Texture.CHARACTER_INFO.width(),
                Texture.CHARACTER_INFO.height());
        float offsetX = renderWidth * 0.028f;
        float offsetY = renderHeight * 0.02f;
        float scale = this.height * 0.0035f;

        poseStack.translate(offsetX, offsetY, 0);

        RenderUtils.drawProgressBar(
                poseStack,
                Texture.XP_BAR,
                renderWidth * 0.05f,
                renderHeight * 0.7f,
                renderWidth * 0.8f,
                renderHeight * 0.8f,
                0,
                0,
                Texture.XP_BAR.width(),
                Texture.XP_BAR.height(),
                selected.getClassInfo().xp() / 100f);

        poseStack.pushPose();
        poseStack.translate(renderWidth * 0.08f, renderHeight * 0.15f, 0);

        poseStack.translate(renderWidth * 0.27f, 0, 0);
        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.QUESTS_SCROLL_ICON.resource(),
                0,
                0,
                0,
                Texture.QUESTS_SCROLL_ICON.width() * currentTextureScale,
                Texture.QUESTS_SCROLL_ICON.height() * currentTextureScale,
                Texture.QUESTS_SCROLL_ICON.width(),
                Texture.QUESTS_SCROLL_ICON.height());

        poseStack.pushPose();
        poseStack.scale(scale, scale, 0f);
        poseStack.translate(renderWidth * 0.15f / scale, 0, 0);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(
                                String.valueOf(this.selected.getClassInfo().completedQuests())),
                        0,
                        0,
                        CommonColors.BLACK,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NONE);
        poseStack.popPose();

        poseStack.translate(renderWidth * 0.32f, 0, 0);
        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.CHALLENGES_ICON.resource(),
                0,
                0,
                0,
                Texture.CHALLENGES_ICON.width() * currentTextureScale,
                Texture.CHALLENGES_ICON.height() * currentTextureScale,
                Texture.CHALLENGES_ICON.width(),
                Texture.CHALLENGES_ICON.height());
        // FIXME: Render character challenge mode info here

        poseStack.popPose();
        poseStack.popPose();
    }

    private void renderScrollButton(PoseStack poseStack) {
        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.SCROLL_BUTTON.resource(),
                Texture.CHARACTER_LIST_BACKGROUND.width() * currentTextureScale * 0.916f,
                MathUtils.map(
                        scrollOffset,
                        0,
                        classInfoList.size() - CHARACTER_INFO_PER_PAGE,
                        Texture.CHARACTER_LIST_BACKGROUND.height() * currentTextureScale * 0.01f,
                        Texture.CHARACTER_LIST_BACKGROUND.height() * currentTextureScale * 0.92f),
                0,
                Texture.SCROLL_BUTTON.width() * currentTextureScale,
                Texture.SCROLL_BUTTON.height() * currentTextureScale,
                Texture.SCROLL_BUTTON.width(),
                Texture.SCROLL_BUTTON.height());
    }

    private void renderWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        for (ClassInfoButton classInfoButton : classInfoButtons) {
            classInfoButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderPlayer(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        McUtils.player().setInvisible(false);
        // This is actually needed...
        McUtils.player().resetFallDistance();
        McUtils.player().setSwimming(false);

        int scale = (int) (this.height / 4.5f);

        int renderX = (int) (this.width * 0.5f);
        int renderY = (int) (this.height * 0.4f);

        final int renderWidth = (int) (this.width * 0.128f);
        final int renderHeight = (int) (this.height * 0.5f);

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                renderX,
                renderY,
                renderX + renderWidth,
                renderY + renderHeight,
                scale,
                0,
                renderX + renderWidth / 2,
                renderY + renderHeight / 2,
                McUtils.player());
    }

    private void setScrollOffset(int delta) {
        scrollOffset =
                MathUtils.clamp(scrollOffset - delta, 0, Math.max(0, classInfoList.size() - CHARACTER_INFO_PER_PAGE));

        reloadButtons();
    }

    private void reloadButtons() {
        classInfoButtons.clear();

        final float width = Texture.CHARACTER_LIST_BACKGROUND.width() * currentTextureScale * 0.9f - 3;
        final int height = Math.round(Texture.CHARACTER_LIST_BACKGROUND.height() * currentTextureScale / 8f);
        for (int i = scrollOffset; i < Math.min(classInfoList.size(), scrollOffset + CHARACTER_INFO_PER_PAGE); i++) {
            ClassInfo classInfo = classInfoList.get(i);
            ClassInfoButton newButton = new ClassInfoButton(
                    5, (5 + (classInfoButtons.size() * height)), (int) width, height, classInfo, this);
            classInfoButtons.add(newButton);

            if (selected != null && selected.getClassInfo() == classInfo) {
                selected = newButton;
            }
        }
    }

    public void setFirstNewCharacterSlot(int firstNewCharacterSlot) {
        this.firstNewCharacterSlot = firstNewCharacterSlot;
    }

    public void setClassInfoList(List<ClassInfo> classInfoList) {
        firstNewCharacterSlot = -1;

        this.classInfoList = classInfoList;

        reloadButtons();
    }

    public ClassInfoButton getSelected() {
        return selected;
    }

    public AbstractContainerScreen<?> getActualClassSelectionScreen() {
        return actualClassSelectionScreen;
    }

    public int getFirstNewCharacterSlot() {
        return firstNewCharacterSlot;
    }
}
