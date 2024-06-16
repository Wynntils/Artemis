/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.maps;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.WynntilsGridLayoutScreen;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.screens.maps.widgets.CategoryWidget;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public final class WaypointCategoryScreen extends WynntilsGridLayoutScreen {
    private static final int CATEGORIES_PER_PAGE = 16;
    private static final int SCROLLBAR_HEIGHT = 40;
    private static final float SCROLL_FACTOR = 10f;
    private static final String DEFAULT_CATEGORY = "wynntils:personal:waypoint";
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("^[a-z0-9:-]+$");

    private final List<String> categoriesList = new ArrayList<>();
    private final WaypointCreationScreen creationScreen;
    private List<CategoryWidget> categoryWidgets = new ArrayList<>();

    private boolean draggingScroll;
    private Button useNewCategoryButton;
    private float scrollRenderY;
    private int scrollOffset = 0;
    private String categoryPath;
    private String currentCategory = "";
    private Set<String> allCategories;
    private TextInputBoxWidget newCategoryInput;

    private WaypointCategoryScreen(WaypointCreationScreen creationScreen, String categoryPath) {
        super(Component.literal("Waypoint Category Screen"));

        this.creationScreen = creationScreen;
        this.categoryPath = categoryPath;

        String[] pathList = this.categoryPath.split(":");

        if (pathList.length == 1) {
            currentCategory = pathList[0];
        } else {
            currentCategory = pathList[pathList.length - 1];
        }

        allCategories = Services.Waypoints.getCategories();
        // Get all of the categories and their subcategories
        allCategories = allCategories.stream()
                .map(category -> {
                    if (category.equals(DEFAULT_CATEGORY)) {
                        return "";
                    } else {
                        return category.substring(DEFAULT_CATEGORY.length() + 1); // +1 for the ":" on end
                    }
                })
                .filter(category -> !category.isEmpty())
                .collect(Collectors.toSet());

        updateCategoryList();
    }

    public static Screen create(WaypointCreationScreen creationScreen, String categoryPath) {
        return new WaypointCategoryScreen(creationScreen, categoryPath);
    }

    @Override
    protected void doInit() {
        super.doInit();

        newCategoryInput = new TextInputBoxWidget(
                (int) (dividedWidth * 49),
                (int) (dividedHeight * 27),
                (int) (dividedWidth * 14),
                BUTTON_HEIGHT,
                (s) -> useNewCategoryButton.active =
                        CATEGORY_PATTERN.matcher(s).matches() && !s.equals(categoryPath) && !s.endsWith(":"),
                this,
                newCategoryInput);
        newCategoryInput.setTooltip(
                Tooltip.create(Component.translatable("screens.wynntils.waypointCategory.nameTooltip")));
        this.addRenderableWidget(newCategoryInput);

        this.addRenderableWidget(
                new Button.Builder(Component.translatable("screens.wynntils.waypointCategory.useCurrent"), (button) -> {
                            creationScreen.setCategory(categoryPath);
                            onClose();
                        })
                        .pos((int) (dividedWidth * 35), (int) (dividedHeight * 33))
                        .size((int) (dividedWidth * 12), BUTTON_HEIGHT)
                        .build());

        useNewCategoryButton = new Button.Builder(
                        Component.translatable("screens.wynntils.waypointCategory.useNew"), (button) -> {
                            creationScreen.setCategory(newCategoryInput.getTextBoxInput());
                            onClose();
                        })
                .pos((int) (dividedWidth * 50), (int) (dividedHeight * 33))
                .size((int) (dividedWidth * 12), BUTTON_HEIGHT)
                .build();
        useNewCategoryButton.active =
                CATEGORY_PATTERN.matcher(newCategoryInput.getTextBoxInput()).matches();
        this.addRenderableWidget(useNewCategoryButton);

        this.addRenderableWidget(new Button.Builder(
                        Component.translatable("screens.wynntils.waypointCategory.back"), (button) -> onClose())
                .pos((int) (dividedWidth * 45), (int) (dividedHeight * 39))
                .size((int) (dividedWidth * 7), BUTTON_HEIGHT)
                .build());

        if (!categoryPath.isEmpty()) {
            newCategoryInput.setTextBoxInput(categoryPath);
        }

        populateCategories();
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        RenderUtils.drawRect(
                poseStack,
                CommonColors.BLACK.withAlpha(100),
                dividedWidth * 33,
                dividedHeight * 18,
                0,
                dividedWidth * 31,
                dividedHeight * 26);

        super.doRender(guiGraphics, mouseX, mouseY, partialTick);

        for (CategoryWidget categoryWidget : categoryWidgets) {
            categoryWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (categoryWidgets.size() > CATEGORIES_PER_PAGE) {
            renderScrollBar(poseStack);
        } else if (categoryWidgets.isEmpty()) {
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromComponent(
                                    Component.translatable("screens.wynntils.waypointCategory.noCategories")),
                            dividedWidth * 2,
                            dividedHeight * 32,
                            CommonColors.WHITE,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.MIDDLE,
                            TextShadow.NORMAL,
                            2);
        }

        FontRenderer.getInstance()
                .renderScrollingText(
                        poseStack,
                        StyledText.fromString(
                                I18n.get("screens.wynntils.waypointCategory.currentCategory") + ": " + currentCategory),
                        dividedWidth * 34,
                        dividedHeight * 21,
                        dividedWidth * 30,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL,
                        1);

        FontRenderer.getInstance()
                .renderScrollingText(
                        poseStack,
                        StyledText.fromString(
                                I18n.get("screens.wynntils.waypointCategory.categoryPath") + ": " + categoryPath),
                        dividedWidth * 34,
                        dividedHeight * 25,
                        dividedWidth * 30,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL,
                        1);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.waypointCategory.newCategory") + ":"),
                        dividedWidth * 34,
                        dividedHeight * 29,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL,
                        1);
    }

    @Override
    public void onClose() {
        McUtils.mc().setScreen(creationScreen);
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        for (CategoryWidget widget : categoryWidgets) {
            if (widget.isMouseOver(mouseX, mouseY)) {
                return widget.mouseClicked(mouseX, mouseY, button);
            }
        }

        if (!draggingScroll
                && (categoryWidgets.size() > CATEGORIES_PER_PAGE)
                && MathUtils.isInside(
                        (int) mouseX,
                        (int) mouseY,
                        (int) (dividedWidth * 32),
                        (int) (dividedWidth * 32) + (int) (dividedWidth / 2),
                        (int) scrollRenderY,
                        (int) (scrollRenderY + SCROLLBAR_HEIGHT))) {
            draggingScroll = true;
            return true;
        }

        return super.doMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScroll) {
            int newOffset = Math.round(
                    MathUtils.map((float) mouseY, 20, 20 + this.height - SCROLLBAR_HEIGHT, 0, getMaxScrollOffset()));

            newOffset = Math.max(0, Math.min(newOffset, getMaxScrollOffset()));

            scroll(newOffset);

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int scrollAmount = (int) (-deltaY * SCROLL_FACTOR);

        if (categoryWidgets.size() > CATEGORIES_PER_PAGE) {
            int newOffset = Math.max(0, Math.min(scrollOffset + scrollAmount, getMaxScrollOffset()));
            scroll(newOffset);
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    public void selectCategory(String category) {
        currentCategory = category;

        if (categoryPath.isEmpty()) {
            categoryPath += category;
        } else {
            categoryPath += ":" + category;
        }

        newCategoryInput.setTextBoxInput(categoryPath);
        updateCategoryList();
        populateCategories();
    }

    public void selectPreviousCategory() {
        String[] pathList = categoryPath.split(":");

        if (pathList.length == 1) {
            currentCategory = "";
        } else {
            currentCategory = pathList[pathList.length - 2];
        }

        String[] newPathList = Arrays.copyOfRange(pathList, 0, pathList.length - 1);
        categoryPath = String.join(":", newPathList);

        newCategoryInput.setTextBoxInput(categoryPath);
        updateCategoryList();
        populateCategories();
    }

    private void populateCategories() {
        categoryWidgets = new ArrayList<>();

        int renderY = 0;

        if (!currentCategory.isEmpty()) {
            CategoryWidget rootCategoryWidget = new CategoryWidget(
                    renderY, (int) (dividedWidth * 32), (int) (dividedHeight * 4), currentCategory, true);
            categoryWidgets.add(rootCategoryWidget);

            renderY += (int) (dividedHeight * 4);
        }

        for (String category : categoriesList) {
            CategoryWidget categoryWidget =
                    new CategoryWidget(renderY, (int) (dividedWidth * 32), (int) (dividedHeight * 4), category, false);
            categoryWidgets.add(categoryWidget);

            renderY += (int) (dividedHeight * 4);
        }

        scroll(scrollOffset);
    }

    private void updateCategoryList() {
        categoriesList.clear();

        categoriesList.addAll(allCategories.stream()
                .filter(category -> category.startsWith(categoryPath + ":") || categoryPath.isEmpty())
                .map(category -> {
                    if (categoryPath.isEmpty()) {
                        return category.split(":")[0];
                    } else {
                        return category.split(":")[categoryPath.split(":").length];
                    }
                })
                .filter(category -> !category.isEmpty())
                .collect(Collectors.toSet()));
    }

    private void renderScrollBar(PoseStack poseStack) {
        RenderUtils.drawRect(
                poseStack, CommonColors.LIGHT_GRAY, (dividedWidth * 32), 0, 0, (dividedWidth / 2), this.height);

        scrollRenderY = (int) (MathUtils.map(scrollOffset, 0, getMaxScrollOffset(), 0, this.height - SCROLLBAR_HEIGHT));

        RenderUtils.drawRect(
                poseStack,
                draggingScroll ? CommonColors.BLACK : CommonColors.GRAY,
                (dividedWidth * 32),
                scrollRenderY,
                0,
                (dividedWidth / 2),
                SCROLLBAR_HEIGHT);
    }

    private void scroll(int newOffset) {
        scrollOffset = newOffset;

        for (CategoryWidget categoryWidget : categoryWidgets) {
            int newY = (categoryWidgets.indexOf(categoryWidget) * (int) (dividedHeight * 4)) - scrollOffset;

            categoryWidget.setY(newY);
        }
    }

    private int getMaxScrollOffset() {
        return (categoriesList.size() - CATEGORIES_PER_PAGE) * (int) (dividedHeight * 4);
    }
}
