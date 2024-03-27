/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.skillpointloadouts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.character.type.SavableSkillPointSet;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.screens.base.WynntilsGridLayoutScreen;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.skillpointloadouts.widgets.ConvertButton;
import com.wynntils.screens.skillpointloadouts.widgets.DeleteButton;
import com.wynntils.screens.skillpointloadouts.widgets.LoadButton;
import com.wynntils.screens.skillpointloadouts.widgets.LoadoutWidget;
import com.wynntils.screens.skillpointloadouts.widgets.SaveButton;
import com.wynntils.screens.skillpointloadouts.widgets.ScrollBar;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class SkillPointLoadoutsScreen extends WynntilsGridLayoutScreen {
    private static final int MAX_LOADOUTS_PER_PAGE = 11;
    private List<LoadoutWidget> loadoutWidgets = new ArrayList<>();

    private boolean firstInit = true;
    private final List<Pair<Supplier<String>, Function<Skill, Integer>>> summaryParts = new ArrayList<>();

    private SaveButton saveAssignedButton;
    private SaveButton saveBuildButton;
    public TextInputBoxWidget saveNameInput;
    public boolean hasSaveNameConflict = false;

    public Pair<String, SavableSkillPointSet> selectedLoadout;
    private WynntilsButton loadButton;
    private WynntilsButton deleteButton;
    private WynntilsButton convertButton;

    private ScrollBar scrollBar;
    private float scrollPercent = 0;

    private SkillPointLoadoutsScreen() {
        super(Component.literal("Skill Point Loadouts Screen"));
    }

    public static Screen create() {
        return new SkillPointLoadoutsScreen();
    }

    @Override
    protected void doInit() {
        super.doInit();
        if (firstInit) {
            firstInit = false;

            summaryParts.add(Pair.of(
                    () -> (Models.SkillPoint.hasIllegalAssigned() ? ChatFormatting.RED : "")
                            + I18n.get(
                                    "screens.wynntils.skillPointLoadouts.assigned", Models.SkillPoint.getAssignedSum()),
                    Models.SkillPoint::getAssignedSkillPoints));
            summaryParts.add(Pair.of(
                    () -> I18n.get("screens.wynntils.skillPointLoadouts.gear", Models.SkillPoint.getGearSum()),
                    Models.SkillPoint::getGearSkillPoints));
            summaryParts.add(Pair.of(
                    () -> I18n.get("screens.wynntils.skillPointLoadouts.setBonus", Models.SkillPoint.getSetBonusSum()),
                    Models.SkillPoint::getSetBonusSkillPoints));
            summaryParts.add(Pair.of(
                    () -> I18n.get("screens.wynntils.skillPointLoadouts.tomes", Models.SkillPoint.getTomeSum()),
                    Models.SkillPoint::getTomeSkillPoints));
            summaryParts.add(Pair.of(
                    () -> I18n.get("screens.wynntils.skillPointLoadouts.crafted", Models.SkillPoint.getCraftedSum()),
                    Models.SkillPoint::getCraftedSkillPoints));
            summaryParts.add(Pair.of(
                    () -> I18n.get(
                            "screens.wynntils.skillPointLoadouts.statusEffects",
                            Models.SkillPoint.getStatusEffectsSum()),
                    Models.SkillPoint::getStatusEffectSkillPoints));
            summaryParts.add(Pair.of(
                    () -> I18n.get("screens.wynntils.skillPointLoadouts.total", Models.SkillPoint.getTotalSum()),
                    Models.SkillPoint::getTotalSkillPoints));

            Models.SkillPoint.populateSkillPoints();
        }

        populateLoadouts();

        // region Widget initialization
        saveNameInput = new TextInputBoxWidget(
                (int) (dividedWidth * 35),
                (int) (dividedHeight * 24),
                (int) ((dividedWidth * 48) - (dividedWidth * 35)),
                BUTTON_HEIGHT,
                (x) -> {
                    saveAssignedButton.active = !x.isBlank();
                    saveBuildButton.active = !x.isBlank();
                    hasSaveNameConflict = false;
                    resetSaveButtons();
                },
                this,
                saveNameInput);
        this.addRenderableWidget(saveNameInput);

        saveAssignedButton = new SaveButton(
                (int) (dividedWidth * 49),
                (int) (dividedHeight * 24),
                (int) ((dividedWidth * 53) - (dividedWidth * 49)),
                BUTTON_HEIGHT,
                Component.translatable("screens.wynntils.skillPointLoadouts.save"),
                this,
                Models.SkillPoint::saveCurrentSkillPoints);
        this.addRenderableWidget(saveAssignedButton);

        saveBuildButton = new SaveButton(
                (int) (dividedWidth * 54),
                (int) (dividedHeight * 24),
                (int) ((dividedWidth * 59) - (dividedWidth * 54)),
                BUTTON_HEIGHT,
                Component.translatable("screens.wynntils.skillPointLoadouts.saveBuild"),
                this,
                Models.SkillPoint::saveCurrentBuild);
        this.addRenderableWidget(saveBuildButton);

        loadButton = new LoadButton(
                (int) (dividedWidth * 35),
                (int) (dividedHeight * 52),
                (int) ((dividedWidth * 44) - (dividedWidth * 35)),
                BUTTON_HEIGHT,
                Component.translatable("screens.wynntils.skillPointLoadouts.load"),
                this);
        this.addRenderableWidget(loadButton);

        deleteButton = new DeleteButton(
                (int) (dividedWidth * 45),
                (int) (dividedHeight * 52),
                (int) ((dividedWidth * 51) - (dividedWidth * 45)),
                BUTTON_HEIGHT,
                Component.translatable("screens.wynntils.skillPointLoadouts.delete")
                        .withStyle(ChatFormatting.RED),
                this);
        this.addRenderableWidget(deleteButton);

        convertButton = new ConvertButton(
                (int) (dividedWidth * 52),
                (int) (dividedHeight * 52),
                (int) ((dividedWidth * 59) - (dividedWidth * 52)),
                BUTTON_HEIGHT,
                Component.translatable("screens.wynntils.skillPointLoadouts.convert"),
                this);
        this.addRenderableWidget(convertButton);
        // endregion

        scrollBar = new ScrollBar(dividedWidth * 30, dividedHeight * 8, dividedWidth * 0.5f, 0, this, dividedHeight);
        this.addRenderableWidget(scrollBar);

        setSelectedLoadout(null);
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.doRender(guiGraphics, mouseX, mouseY, partialTick);
        PoseStack poseStack = guiGraphics.pose();

        // region Loadout headers
        RenderUtils.drawRect(
                poseStack,
                CommonColors.WHITE,
                dividedWidth * 4,
                dividedHeight * 8,
                0,
                dividedWidth * 30 - dividedWidth * 4,
                1);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.skillPointLoadouts.loadoutName")),
                        dividedWidth * 4,
                        dividedHeight * 8,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.BOTTOM,
                        TextShadow.NORMAL);
        for (int i = 0; i < 5; i++) {
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString(Skill.values()[i].getColorCode() + Skill.values()[i].getSymbol()),
                            dividedWidth * (21 + i * 2),
                            dividedHeight * 8,
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.BOTTOM,
                            TextShadow.NORMAL);
        }
        // endregion

        // region Summary
        RenderUtils.drawRectBorders(
                poseStack,
                CommonColors.WHITE,
                dividedWidth * 34,
                dividedHeight * 8,
                dividedWidth * 60,
                dividedHeight * 28,
                1,
                1);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.skillPointLoadouts.summary")),
                        dividedWidth * 34,
                        dividedHeight * 8,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.BOTTOM,
                        TextShadow.NORMAL);
        for (int i = 0; i < 5; i++) {
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString(Skill.values()[i].getColorCode() + Skill.values()[i].getSymbol()),
                            dividedWidth * (51 + i * 2),
                            dividedHeight * 8,
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.BOTTOM,
                            TextShadow.NORMAL);
        }

        // i iterates over the summary parts populating top to bottom
        // j iterates over the skills populating left to right
        for (int i = 0; i < summaryParts.size(); i++) {
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString(summaryParts.get(i).key().get()),
                            dividedWidth * 35,
                            dividedHeight * (11 + i * 2),
                            CommonColors.WHITE,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.BOTTOM,
                            TextShadow.NORMAL);
            for (int j = 0; j < 5; j++) {
                FontRenderer.getInstance()
                        .renderText(
                                poseStack,
                                StyledText.fromString(Skill.values()[j].getColorCode() + ""
                                        + summaryParts.get(i).value().apply(Skill.values()[j])),
                                dividedWidth * (51 + j * 2),
                                dividedHeight * (10 + i * 2),
                                CommonColors.WHITE,
                                HorizontalAlignment.CENTER,
                                VerticalAlignment.BOTTOM,
                                TextShadow.NORMAL);
            }
        }

        if (hasSaveNameConflict) {
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString(I18n.get("screens.wynntils.skillPointLoadouts.saveNameConflict")),
                            dividedWidth * 35,
                            dividedHeight * 23,
                            CommonColors.WHITE,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.BOTTOM,
                            TextShadow.NORMAL);
        }
        // endregion

        // region Selected loadout
        if (selectedLoadout != null) {
            RenderUtils.drawRectBorders(
                    poseStack,
                    CommonColors.WHITE,
                    dividedWidth * 34,
                    dividedHeight * 34,
                    dividedWidth * 60,
                    dividedHeight * 56,
                    1,
                    1);
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString(selectedLoadout.key()),
                            dividedWidth * 34,
                            dividedHeight * 34,
                            CommonColors.WHITE,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.BOTTOM,
                            TextShadow.NORMAL);
            for (int i = 0; i < 5; i++) {
                FontRenderer.getInstance()
                        .renderText(
                                poseStack,
                                StyledText.fromString(Skill.values()[i].getColorCode() + Skill.values()[i].getSymbol()),
                                dividedWidth * (51 + i * 2),
                                dividedHeight * 34,
                                CommonColors.WHITE,
                                HorizontalAlignment.CENTER,
                                VerticalAlignment.BOTTOM,
                                TextShadow.NORMAL);
            }
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString(I18n.get(
                                    "screens.wynntils.skillPointLoadouts.assigned",
                                    selectedLoadout.value().getSkillPointsSum())),
                            dividedWidth * 35,
                            dividedHeight * 37,
                            CommonColors.WHITE,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.BOTTOM,
                            TextShadow.NORMAL);
            for (int i = 0; i < 5; i++) {
                FontRenderer.getInstance()
                        .renderText(
                                poseStack,
                                StyledText.fromString(Skill.values()[i].getColorCode() + ""
                                        + selectedLoadout.value().getSkillPointsAsArray()[i]),
                                dividedWidth * (51 + i * 2),
                                dividedHeight * 37,
                                CommonColors.WHITE,
                                HorizontalAlignment.CENTER,
                                VerticalAlignment.BOTTOM,
                                TextShadow.NORMAL);
            }
            if (selectedLoadout.value().isBuild()) {
                List<TextRenderTask> tasks = new ArrayList<>();
                for (int i = 0; i < selectedLoadout.value().armourNames().size(); i++) {
                    String armour = selectedLoadout.value().armourNames().get(i);

                    tasks.add(new TextRenderTask(
                            StyledText.fromString(armour),
                            new TextRenderSetting(
                                    dividedWidth * 9,
                                    CommonColors.WHITE,
                                    HorizontalAlignment.LEFT,
                                    VerticalAlignment.BOTTOM,
                                    TextShadow.NORMAL)));
                    tasks.add(new TextRenderTask(
                            StyledText.EMPTY,
                            new TextRenderSetting(
                                    0,
                                    CommonColors.WHITE,
                                    HorizontalAlignment.LEFT,
                                    VerticalAlignment.BOTTOM,
                                    TextShadow.NORMAL)));
                }
                FontRenderer.getInstance().renderTexts(poseStack, dividedWidth * 35, dividedHeight * 41, tasks);

                tasks = new ArrayList<>();
                for (int i = 0; i < selectedLoadout.value().accessoryNames().size(); i++) {
                    String accessory = selectedLoadout.value().accessoryNames().get(i);

                    tasks.add(new TextRenderTask(
                            StyledText.fromString(accessory),
                            new TextRenderSetting(
                                    dividedWidth * 9,
                                    CommonColors.WHITE,
                                    HorizontalAlignment.LEFT,
                                    VerticalAlignment.BOTTOM,
                                    TextShadow.NORMAL)));
                    tasks.add(new TextRenderTask(
                            StyledText.EMPTY,
                            new TextRenderSetting(
                                    0,
                                    CommonColors.WHITE,
                                    HorizontalAlignment.LEFT,
                                    VerticalAlignment.BOTTOM,
                                    TextShadow.NORMAL)));
                }

                FontRenderer.getInstance()
                        .renderTexts(
                                poseStack,
                                dividedWidth
                                        * (selectedLoadout.value().armourNames().isEmpty()
                                                ? 35
                                                : 44), // left align accessories if no armour
                                dividedHeight * 41,
                                tasks);
            } else {
                FontRenderer.getInstance()
                        .renderText(
                                poseStack,
                                StyledText.fromString(I18n.get("screens.wynntils.skillPointLoadouts.notBuild")),
                                dividedWidth * 35,
                                dividedHeight * 42,
                                CommonColors.WHITE,
                                HorizontalAlignment.LEFT,
                                VerticalAlignment.BOTTOM,
                                TextShadow.NORMAL);
            }
        }
        // endregion

        // region Scrollbar
        if (loadoutWidgets.size() > MAX_LOADOUTS_PER_PAGE) {
            scrollBar.visible = true;
            scrollBar.active = true;
            float visibleRatio = Math.min(1, (float) MAX_LOADOUTS_PER_PAGE / loadoutWidgets.size());
            float scrollbarLength = dividedHeight * 48 * visibleRatio + 1;
            scrollBar.setY((int) (dividedHeight * 8 + dividedHeight * 48 * scrollPercent));
            scrollBar.setHeight((int) scrollbarLength);
        } else {
            scrollBar.visible = false;
            scrollBar.active = false;
        }
        // Only render from 8 to 56 for scrollable area
        // -/+ 1 to not overlap/cut off content
        RenderUtils.createRectMask(
                poseStack,
                (int) (dividedWidth * 4) - 1,
                (int) (dividedHeight * 8) + 1,
                (int) (dividedWidth * 26) + 1,
                (int) (dividedHeight * 48) + 1);
        loadoutWidgets.forEach(widget -> widget.render(guiGraphics, mouseX, mouseY, partialTick));
        RenderUtils.clearMask();
        // endregion
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        for (LoadoutWidget widget : loadoutWidgets) {
            if (widget.isMouseOver(mouseX, mouseY)) {
                widget.mouseClicked(mouseX, mouseY, button);
                return true;
            }
        }
        return super.doMouseClicked(mouseX, mouseY, button);
    }

    // baseYPosition - dividedHeight * maxScrollOffset * scrollPercent = dividedHeight * 52
    // Solve for maxScrollOffset
    // Full explanation in #artemis-dev
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // In terms of grid divisions
        // 11 loadouts are fully displayed from 9 to 56
        // 12th one is very slightly cut off (it needs 57)
        if (loadoutWidgets.size() <= MAX_LOADOUTS_PER_PAGE) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        doScroll(scrollY);
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public void doScroll(double scrollAmount) {
        int scrollableWidgets = Math.max(0, loadoutWidgets.size() - MAX_LOADOUTS_PER_PAGE);
        float scrollableRatio = (float) scrollableWidgets / loadoutWidgets.size();
        float maxScrollOffset = (4 * (loadoutWidgets.size() - 1) - 43) / scrollableRatio;
        scrollPercent = (float) Math.max(0, Math.min(scrollableRatio, scrollPercent - scrollAmount / 50));

        loadoutWidgets.forEach(widget -> {
            float baseYPosition = dividedHeight * (9f + loadoutWidgets.indexOf(widget) * 4f);

            float scrollOffset = dividedHeight * maxScrollOffset * scrollPercent;
            widget.setY((int) (baseYPosition - scrollOffset));
            widget.visible = !(widget.getY() <= dividedHeight * 4) && !(widget.getY() >= dividedHeight * 56);
        });
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_DELETE && deleteButton.active) {
            deleteButton.onPress();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_END) {
            doScroll(Float.NEGATIVE_INFINITY);
        } else if (keyCode == GLFW.GLFW_KEY_HOME) {
            doScroll(Float.POSITIVE_INFINITY);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void setSelectedLoadout(Pair<String, SavableSkillPointSet> loadout) {
        if (loadout == null) {
            selectedLoadout = null;
            loadButton.active = false;
            loadButton.visible = false;
            deleteButton.active = false;
            deleteButton.visible = false;
            convertButton.active = false;
            convertButton.visible = false;
            return;
        }

        selectedLoadout = loadout;
        loadButton.active = true;
        loadButton.visible = true;
        deleteButton.active = true;
        deleteButton.visible = true;
        convertButton.active = true;
        convertButton.visible = true;
        if (selectedLoadout.value().getMinimumCombatLevel()
                > Models.CombatXp.getCombatLevel().current()) {
            loadButton.setTooltip(
                    Tooltip.create(Component.translatable("screens.wynntils.skillPointLoadouts.levelIncompatible")
                            .withStyle(ChatFormatting.RED)));
        }
    }

    public Pair<String, SavableSkillPointSet> getSelectedLoadout() {
        return selectedLoadout;
    }

    public void resetSaveButtons() {
        saveAssignedButton.reset();
        saveBuildButton.reset();
    }

    public void populateLoadouts() {
        loadoutWidgets = new ArrayList<>();
        Map<String, SavableSkillPointSet> loadouts = Models.SkillPoint.getLoadouts();
        for (Map.Entry<String, SavableSkillPointSet> entry : loadouts.entrySet()) {
            loadoutWidgets.add(new LoadoutWidget(
                    (int) (dividedWidth * 4),
                    (int) (dividedHeight * (9 + loadoutWidgets.size() * 4)),
                    (int) (dividedWidth * 26),
                    (int) (dividedHeight * 4),
                    dividedWidth,
                    entry.getKey(),
                    entry.getValue(),
                    this));
        }
    }
}
