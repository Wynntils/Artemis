/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.activities.widgets.QuestBookSearchWidget;
import com.wynntils.screens.base.widgets.SearchWidget;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public abstract class WynntilsListScreen<E, B extends WynntilsButton> extends WynntilsMenuScreenBase
        implements WynntilsPagedScreen {
    private double currentScroll = 0;

    protected int currentPage = 0;
    protected int maxPage = 0;
    protected List<E> elements = new ArrayList<>();

    private final List<B> elementButtons = new ArrayList<>();
    protected SearchWidget searchWidget;
    protected Renderable hovered = null;

    @Override
    protected void doInit() {
        reloadElements(searchWidget.getTextBoxInput());

        this.addRenderableWidget(searchWidget);
    }

    protected WynntilsListScreen(Component component) {
        super(component);

        // Do not lose search info on re-init
        this.searchWidget = new QuestBookSearchWidget(
                (int) (Texture.CONTENT_BOOK_BACKGROUND.width() / 2f + 15),
                0,
                Texture.CONTENT_BOOK_SEARCH.width(),
                Texture.CONTENT_BOOK_SEARCH.height(),
                s -> reloadElements(),
                this);
    }

    protected void renderWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.hovered = null;

        final float translationX = getTranslationX();
        final float translationY = getTranslationY();

        for (Renderable renderable : new ArrayList<>(this.renderables)) {
            renderable.render(guiGraphics, (int) (mouseX - translationX), (int) (mouseY - translationY), partialTick);

            if (renderable instanceof WynntilsButton button) {
                if (button.isMouseOver(mouseX - translationX, mouseY - translationY)) {
                    this.hovered = button;
                }
            }
        }
    }

    protected void renderPageInfo(PoseStack poseStack, int currentPage, int maxPage) {
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromString((currentPage) + " / " + (maxPage)),
                        Texture.CONTENT_BOOK_BACKGROUND.width() / 2f,
                        Texture.CONTENT_BOOK_BACKGROUND.width(),
                        Texture.CONTENT_BOOK_BACKGROUND.height() - 25,
                        0,
                        CommonColors.BLACK,
                        HorizontalAlignment.CENTER,
                        TextShadow.NONE);
    }

    protected void renderNoElementsHelper(PoseStack poseStack, String key) {
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromString(key),
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

    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<Component> tooltipLines = List.of();

        if (this.hovered instanceof TooltipProvider tooltipWidget) {
            tooltipLines = tooltipWidget.getTooltipLines();
        }

        if (tooltipLines.isEmpty()) return;

        guiGraphics.renderComponentTooltip(FontRenderer.getInstance().getFont(), tooltipLines, mouseX, mouseY);
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        final float translationX = getTranslationX();
        final float translationY = getTranslationY();

        for (GuiEventListener child : new ArrayList<>(this.children())) {
            if (child.isMouseOver(mouseX - translationX, mouseY - translationY)) {
                child.mouseClicked(mouseX - translationX, mouseY - translationY, button);
            }
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        final float translationX = getTranslationX();
        final float translationY = getTranslationY();

        for (GuiEventListener child : new ArrayList<>(this.children())) {
            child.mouseDragged(mouseX - translationX, mouseY - translationY, button, dragX, dragY);
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        final float translationX = getTranslationX();
        final float translationY = getTranslationY();

        for (GuiEventListener child : new ArrayList<>(this.children())) {
            child.mouseReleased(mouseX - translationX, mouseY - translationY, button);
        }

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.shouldCloseOnEsc()) {
            onClose();
            return true;
        }

        if (searchWidget != null) {
            return searchWidget.keyPressed(keyCode, scanCode, modifiers);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchWidget != null) {
            return searchWidget.charTyped(codePoint, modifiers);
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        // Usually, mouse scroll wheel delta is always (-)1
        if (Math.abs(deltaY) == 1) {
            setCurrentPage(getCurrentPage() - (int) deltaY);
            return true;
        }

        // Now we handle touchpad scrolling

        // Delta is divided by 10 to make it more precise
        // We subtract so scrolling down actually scrolls down
        currentScroll -= deltaY / 10d;

        if (Math.abs(currentScroll) < 1) return true;

        int scroll = (int) (currentScroll);
        currentScroll = currentScroll % 1;

        setCurrentPage(getCurrentPage() + scroll);

        return true;
    }

    @Override
    public int getCurrentPage() {
        return currentPage;
    }

    @Override
    public void setCurrentPage(int currentPage) {
        this.currentPage = MathUtils.clamp(currentPage, 0, maxPage);
        reloadElements(searchWidget.getTextBoxInput());
    }

    @Override
    public int getMaxPage() {
        return maxPage;
    }

    private void reloadElements(String searchTerm) {
        elements.clear();
        reloadElementsList(searchTerm);

        this.maxPage = Math.max(
                0,
                (elements.size() / getElementsPerPage() + (elements.size() % getElementsPerPage() != 0 ? 1 : 0)) - 1);

        for (B button : elementButtons) {
            this.removeWidget(button);
        }

        elementButtons.clear();

        final int start = Math.max(0, currentPage * getElementsPerPage());
        for (int i = start; i < Math.min(elements.size(), start + getElementsPerPage()); i++) {
            B button = getButtonFromElement(i);
            elementButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    protected abstract B getButtonFromElement(int i);

    protected abstract void reloadElementsList(String searchTerm);

    public void reloadElements() {
        reloadElements(searchWidget.getTextBoxInput());
        // Set the current page to the same page, but with the new elements
        // This will clamp the current page to the new max page
        // But if the contents are the same, it will not change the page
        setCurrentPage(getCurrentPage());
    }

    @Override
    public TextInputBoxWidget getFocusedTextInput() {
        return this.searchWidget;
    }

    // Dummy impl
    @Override
    public void setFocusedTextInput(TextInputBoxWidget focusedTextInput) {}

    protected int getElementsPerPage() {
        return 13;
    }

    @Override
    public void added() {
        searchWidget.opened();
        super.added();
    }
}
