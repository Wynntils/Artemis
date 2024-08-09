/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.container.widgets;

import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.inventory.PersonalStorageUtilitiesFeature;
import com.wynntils.mc.extension.ScreenExtension;
import com.wynntils.models.containers.containers.personal.PersonalStorageContainer;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

public class PersonalStorageUtilitiesWidget extends AbstractWidget {
    private static final int BUTTON_SPACING = 18;

    private final PersonalStorageContainer container;
    private final List<QuickJumpButton> quickJumpButtons = new ArrayList<>();
    private final PersonalStorageEditNameButton editButton;
    private final PersonalStorageUtilitiesFeature feature;
    private final AbstractContainerScreen<?> screen;

    private String pageName;
    private TextInputBoxWidget editInput;

    public PersonalStorageUtilitiesWidget(
            int x,
            int y,
            PersonalStorageContainer container,
            PersonalStorageUtilitiesFeature feature,
            AbstractContainerScreen<?> screen) {
        super(x, y, 100, 110, Component.literal("Personal Storage Utilities Widget"));

        this.container = container;
        this.feature = feature;
        this.screen = screen;

        editButton = new PersonalStorageEditNameButton(x + 86, y + 9, 14, 14, this);

        updatePageName();

        addJumpButtons();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderUtils.drawTexturedRect(guiGraphics.pose(), Texture.BANK_PANEL, getX(), getY());

        if (!Models.Bank.isEditingName()) {
            FontRenderer.getInstance()
                    .renderScrollingText(
                            guiGraphics.pose(),
                            StyledText.fromString(pageName),
                            getX() + 4,
                            getY() + 11,
                            getWidth() - 18,
                            CommonColors.WHITE,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.TOP,
                            TextShadow.NORMAL,
                            1f);
        }

        editButton.render(guiGraphics, mouseX, mouseY, partialTick);

        quickJumpButtons.forEach(button -> button.render(guiGraphics, mouseX, mouseY, partialTick));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (GuiEventListener listener : quickJumpButtons) {
            if (listener.isMouseOver(mouseX, mouseY)) {
                return listener.mouseClicked(mouseX, mouseY, button);
            }
        }

        if (editButton.isMouseOver(mouseX, mouseY)) {
            return editButton.mouseClicked(mouseX, mouseY, button);
        }

        return false;
    }

    public void jumpToPage(int destination) {
        feature.jumpToDestination(destination);
    }

    public void addEditInput() {
        if (editInput != null) return;

        editInput = new TextInputBoxWidget(
                getX() + 2,
                getY() + 10,
                getWidth() - 18,
                FontRenderer.getInstance().getFont().lineHeight + 2,
                null,
                (ScreenExtension) screen);

        editInput.setTextBoxInput(Models.Bank.getPageName(Models.Bank.getCurrentPage()));
        screen.addRenderableWidget(editInput);
    }

    public void removeEditInput() {
        if (editInput == null) return;

        screen.removeWidget(editInput);
        editInput = null;
    }

    public void updatePageName() {
        pageName = Models.Bank.getPageName(Models.Bank.getCurrentPage());
    }

    public String getName() {
        return editInput.getTextBoxInput();
    }

    private void addJumpButtons() {
        int renderX = getX() + 6;
        int renderY = getY() + 23;

        for (int i = 0; i < container.getFinalPage(); i++) {
            quickJumpButtons.add(new QuickJumpButton(renderX, renderY, i + 1, this));

            renderX += BUTTON_SPACING;

            if ((i + 1) % 5 == 0) {
                renderX = getX() + 6;
                renderY += BUTTON_SPACING;
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
