/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.gui.render.FontRenderer;
import com.wynntils.gui.render.HorizontalAlignment;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.gui.render.VerticalAlignment;
import com.wynntils.gui.widgets.ClassInfoButton;
import com.wynntils.gui.widgets.ClassSelectionAddButton;
import com.wynntils.gui.widgets.ClassSelectionDeleteButton;
import com.wynntils.gui.widgets.ClassSelectionEditButton;
import com.wynntils.gui.widgets.PlayButton;
import com.wynntils.mc.event.ContainerSetContentEvent;
import com.wynntils.mc.objects.CommonColors;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.mc.utils.ItemUtils;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.utils.MathUtils;
import com.wynntils.wynn.objects.ClassInfo;
import com.wynntils.wynn.objects.ClassType;
import com.wynntils.wynn.utils.ContainerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CharacterSelectorScreen extends Screen {
    private static final int CHARACTER_INFO_PER_PAGE = 7;

    private static final Pattern NEW_CLASS_ITEM_NAME_PATTERN = Pattern.compile("§a\\[+\\] Create new character");
    private static final Pattern CLASS_ITEM_NAME_PATTERN = Pattern.compile("§l§6\\[>\\] Select (.+)");
    private static final Pattern CLASS_ITEM_CLASS_PATTERN = Pattern.compile("§e- §r§7Class: §r§f(.+)");
    private static final Pattern CLASS_ITEM_LEVEL_PATTERN = Pattern.compile("§e- §r§7Level: §r§f(\\d+)");
    private static final Pattern CLASS_ITEM_XP_PATTERN = Pattern.compile("§e- §r§7XP: §r§f(\\d+)%");
    private static final Pattern CLASS_ITEM_SOUL_POINTS_PATTERN = Pattern.compile("§e- §r§7Soul Points: §r§f(\\d+)");
    private static final Pattern CLASS_ITEM_FINISHED_QUESTS_PATTERN =
            Pattern.compile("§e- §r§7Finished Quests: §r§f(\\d+)/\\d+");

    private final AbstractContainerScreen<?> actualClassSelectionScreen;
    private final List<ClassInfo> classInfoList = new ArrayList<>();
    private final List<ClassInfoButton> classInfoButtons = new ArrayList<>();
    private int firstNewCharacterSlot = -1;
    private float currentTextureScale = 1f;

    private int scrollOffset = 0;
    private boolean draggingScroll = false;
    private double lastMouseY = 0;
    private ClassInfoButton selected = null;

    public CharacterSelectorScreen() {
        super(new TranslatableComponent("screens.wynntils.characterSelection.name"));

        if (McUtils.mc().screen instanceof AbstractContainerScreen<?> abstractContainerScreen) {
            actualClassSelectionScreen = abstractContainerScreen;
        } else {
            throw new IllegalStateException(
                    "Tried to open custom character selection screen when normal character selection screen is not open");
        }

        WynntilsMod.registerEventListener(this);
    }

    @Override
    public void onClose() {
        WynntilsMod.unregisterEventListener(this);
        ContainerUtils.closeContainer(actualClassSelectionScreen.getMenu().containerId);
        super.onClose();
    }

    @Override
    protected void init() {
        currentTextureScale = (float) this.height / Texture.LIST_BACKGROUND.height();

        float playButtonWidth = Texture.PLAY_BUTTON.width() * currentTextureScale;
        float playButtonHeight = Texture.PLAY_BUTTON.height() / 2f * currentTextureScale;
        this.addRenderableWidget(new PlayButton(
                (int) (this.width - playButtonWidth - 10f),
                (int) (this.height - playButtonHeight - 10f),
                (int) playButtonWidth,
                (int) playButtonHeight,
                this));

        float deleteButtonWidth = Texture.REMOVE_BUTTON.width() * currentTextureScale;
        float deleteButtonHeight = Texture.REMOVE_BUTTON.height() / 2f * currentTextureScale;
        this.addRenderableWidget(new ClassSelectionDeleteButton(
                (int) (this.width * 0.1535f),
                (int) (this.height * 0.915f),
                (int) deleteButtonWidth,
                (int) deleteButtonHeight,
                this));

        float editButtonWidth = Texture.EDIT_BUTTON.width() * currentTextureScale;
        float editButtonHeight = Texture.EDIT_BUTTON.height() * currentTextureScale;
        this.addRenderableWidget(new ClassSelectionEditButton(
                (int) (this.width * 0.1132f),
                (int) (this.height * 0.915f),
                (int) editButtonWidth,
                (int) editButtonHeight,
                this));

        float addButtonWidth = Texture.ADD_BUTTON.width() * currentTextureScale;
        float addButtonHeight = Texture.ADD_BUTTON.height() / 2f * currentTextureScale;
        this.addRenderableWidget(new ClassSelectionAddButton(
                (int) (this.width * 0.057f),
                (int) (this.height * 0.915f),
                (int) addButtonWidth,
                (int) addButtonHeight,
                this));

        reloadButtons();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (Math.abs(lastMouseY - mouseY) > 20f && draggingScroll) {
            setScrollOffset(lastMouseY > mouseY ? 1 : -1);
            lastMouseY = mouseY;
        }

        this.renderBackground(poseStack);

        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.LIST_BACKGROUND.resource(),
                0,
                0,
                0,
                Texture.LIST_BACKGROUND.width() * currentTextureScale,
                Texture.LIST_BACKGROUND.height() * currentTextureScale,
                Texture.LIST_BACKGROUND.width(),
                Texture.LIST_BACKGROUND.height());

        renderButtons(poseStack, mouseX, mouseY, partialTick);

        renderScrollButton(poseStack);

        renderPlayer();

        if (selected == null) return;

        renderCharacterInfo(poseStack);
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (GuiEventListener child : children()) {
            child.mouseClicked(mouseX, mouseY, button);
        }

        for (int i = scrollOffset; i < Math.min(classInfoButtons.size(), scrollOffset + CHARACTER_INFO_PER_PAGE); i++) {
            ClassInfoButton classInfoButton = classInfoButtons.get(i);
            if (classInfoButton.isMouseOver(mouseX, mouseY)) {
                classInfoButton.mouseClicked(mouseX, mouseY, button);
                this.selected = classInfoButton;
            }
        }

        if (!draggingScroll) {
            float scrollButtonRenderX = Texture.LIST_BACKGROUND.width() * currentTextureScale * 0.916f;
            float scrollButtonRenderY = MathUtils.map(
                    scrollOffset,
                    0,
                    classInfoButtons.size() - CHARACTER_INFO_PER_PAGE,
                    Texture.LIST_BACKGROUND.height() * currentTextureScale * 0.01f,
                    Texture.LIST_BACKGROUND.height() * currentTextureScale * 0.92f);

            if (mouseX >= scrollButtonRenderX
                    && mouseX
                            <= scrollButtonRenderX
                                    + Texture.CHARACTER_SELECTION_SCROLL_BUTTON.width() * currentTextureScale
                    && mouseY >= scrollButtonRenderY
                    && mouseY
                            <= scrollButtonRenderY
                                    + Texture.CHARACTER_SELECTION_SCROLL_BUTTON.height() * currentTextureScale) {
                draggingScroll = true;
                lastMouseY = mouseY;
            }
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        setScrollOffset((int) delta);

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return true;
    }

    @SubscribeEvent
    public void onContainerItemsSet(ContainerSetContentEvent event) {
        classInfoList.clear();
        firstNewCharacterSlot = -1;

        if (event.getContainerId() == actualClassSelectionScreen.getMenu().containerId) {
            List<ItemStack> items = event.getItems();
            for (int i = 0; i < items.size(); i++) {
                ItemStack item = items.get(i);
                String itemName = ComponentUtils.getCoded(item.getHoverName());
                Matcher classItemMatcher = CLASS_ITEM_NAME_PATTERN.matcher(itemName);
                if (classItemMatcher.matches()) {
                    ClassInfo classInfo = getClassInfoFromItem(item, i, classItemMatcher.group(1));
                    classInfoList.add(classInfo);
                    continue;
                }

                if (firstNewCharacterSlot != -1
                        && NEW_CLASS_ITEM_NAME_PATTERN.matcher(itemName).matches()) {
                    firstNewCharacterSlot = i;
                }
            }
        }

        reloadButtons();
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
        float offsetX = this.width * 0.028f;
        float offsetY = this.height * 0.02f;
        float scale = this.height * 0.0035f;

        poseStack.translate(offsetX, offsetY, 0);

        RenderUtils.drawProgressBar(
                poseStack,
                Texture.XP_BAR,
                0,
                this.height * 0.08f,
                this.width * 0.2f,
                this.height * 0.095f,
                0,
                0,
                Texture.XP_BAR.width(),
                Texture.XP_BAR.height(),
                selected.getClassInfo().xp() / 100f);

        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.SOUL_POINT_ICON.resource(),
                0,
                0,
                0,
                Texture.SOUL_POINT_ICON.width() * currentTextureScale,
                Texture.SOUL_POINT_ICON.height() * currentTextureScale,
                Texture.SOUL_POINT_ICON.width(),
                Texture.SOUL_POINT_ICON.height());
        poseStack.pushPose();

        poseStack.scale(scale, scale, 0f);
        poseStack.translate(this.width * 0.04f / currentTextureScale, this.height * 0.01f / currentTextureScale, 0);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        String.valueOf(this.selected.getClassInfo().soulPoints()),
                        0,
                        0,
                        CommonColors.BLACK,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NONE);
        poseStack.popPose();

        poseStack.translate(this.width * 0.073f, 0, 0);
        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.QUESTS_ICON.resource(),
                0,
                0,
                0,
                Texture.QUESTS_ICON.width() * currentTextureScale,
                Texture.QUESTS_ICON.height() * currentTextureScale,
                Texture.QUESTS_ICON.width(),
                Texture.QUESTS_ICON.height());

        poseStack.pushPose();
        poseStack.scale(scale, scale, 0f);
        poseStack.translate(this.width * 0.045f / currentTextureScale, this.height * 0.01f / currentTextureScale, 0);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        String.valueOf(this.selected.getClassInfo().completedQuests()),
                        0,
                        0,
                        CommonColors.BLACK,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NONE);
        poseStack.popPose();

        poseStack.translate(this.width * 0.084f, this.height * 0.005f, 0);
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
        // FIXME: I have no idea what to render here..

        poseStack.popPose();
    }

    private void renderScrollButton(PoseStack poseStack) {
        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.CHARACTER_SELECTION_SCROLL_BUTTON.resource(),
                Texture.LIST_BACKGROUND.width() * currentTextureScale * 0.916f,
                MathUtils.map(
                        scrollOffset,
                        0,
                        classInfoButtons.size() - CHARACTER_INFO_PER_PAGE,
                        Texture.LIST_BACKGROUND.height() * currentTextureScale * 0.01f,
                        Texture.LIST_BACKGROUND.height() * currentTextureScale * 0.92f),
                0,
                Texture.CHARACTER_SELECTION_SCROLL_BUTTON.width() * currentTextureScale,
                Texture.CHARACTER_SELECTION_SCROLL_BUTTON.height() * currentTextureScale,
                Texture.CHARACTER_SELECTION_SCROLL_BUTTON.width(),
                Texture.CHARACTER_SELECTION_SCROLL_BUTTON.height());
    }

    private void renderButtons(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        for (Widget renderable : this.renderables) {
            renderable.render(poseStack, mouseX, mouseY, partialTick);
        }

        for (int i = scrollOffset; i < Math.min(classInfoButtons.size(), scrollOffset + CHARACTER_INFO_PER_PAGE); i++) {
            classInfoButtons.get(i).render(poseStack, mouseX, mouseY, partialTick);
        }
    }

    private void renderPlayer() {
        McUtils.player().setInvisible(false);
        // This is actually needed...
        McUtils.player().resetFallDistance();
        McUtils.player().setSwimming(false);

        int scale = this.height / 5;
        InventoryScreen.renderEntityInInventory(
                (int) (this.width * 0.6f), (int) (this.height * 0.85f), scale, 0, 0, McUtils.player());
    }

    private void setScrollOffset(int delta) {
        scrollOffset = MathUtils.clamp(scrollOffset - delta, 0, classInfoButtons.size() - CHARACTER_INFO_PER_PAGE);
    }

    private void reloadButtons() {
        this.selected = null;
        this.scrollOffset = 0;
        classInfoButtons.clear();

        final float width = Texture.LIST_BACKGROUND.width() * currentTextureScale * 0.9f - 3;
        final int height = Math.round(Texture.LIST_BACKGROUND.height() * currentTextureScale / 8f);
        for (int i = 0; i < classInfoList.size(); i++) {
            ClassInfo classInfo = classInfoList.get(i);
            classInfoButtons.add(new ClassInfoButton(
                    5, (5 + ((i % CHARACTER_INFO_PER_PAGE) * height)), (int) width, height, classInfo, this));
        }
    }

    private static ClassInfo getClassInfoFromItem(ItemStack item, int slot, String className) {
        ClassType classType = null;
        int level = 0;
        int xp = 0;
        int soulPoints = 0;
        int finishedQuests = 0;
        for (String line : ItemUtils.getLore(item)) {
            Matcher matcher = CLASS_ITEM_CLASS_PATTERN.matcher(line);

            if (matcher.matches()) {
                classType = ClassType.fromName(matcher.group(1));
                continue;
            }

            matcher = CLASS_ITEM_LEVEL_PATTERN.matcher(line);
            if (matcher.matches()) {
                level = Integer.parseInt(matcher.group(1));
                continue;
            }

            matcher = CLASS_ITEM_XP_PATTERN.matcher(line);
            if (matcher.matches()) {
                xp = Integer.parseInt(matcher.group(1));
                continue;
            }

            matcher = CLASS_ITEM_SOUL_POINTS_PATTERN.matcher(line);
            if (matcher.matches()) {
                soulPoints = Integer.parseInt(matcher.group(1));
                continue;
            }

            matcher = CLASS_ITEM_FINISHED_QUESTS_PATTERN.matcher(line);
            if (matcher.matches()) {
                finishedQuests = Integer.parseInt(matcher.group(1));
            }
        }

        return new ClassInfo(className, item, slot, classType, level, xp, soulPoints, finishedQuests);
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

    public float getCurrentTextureScale() {
        return currentTextureScale;
    }
}
