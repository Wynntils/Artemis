/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.itemfilter.widgets.numeric;

import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.screens.itemfilter.ItemFilterScreen;
import com.wynntils.screens.itemfilter.widgets.GeneralFilterWidget;
import com.wynntils.screens.itemfilter.widgets.ProviderFilterListWidget;
import com.wynntils.services.itemfilter.type.StatFilter;
import com.wynntils.services.itemfilter.type.StatProviderAndFilterPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public abstract class InequalityNumericFilterWidget<T> extends GeneralFilterWidget {
    private final Button removeButton;
    private final Button inequalityButton;
    private final TextInputBoxWidget entryInput;

    private boolean ignoreUpdate = false;
    private InequalityType inequalityType = InequalityType.GREATER_THAN;

    protected InequalityNumericFilterWidget(
            int x, int y, int width, int height, ProviderFilterListWidget parent, ItemFilterScreen filterScreen) {
        super(x, y, width, height, Component.literal("Inequality Numeric Filter Widget"), parent);

        this.entryInput = new TextInputBoxWidget(
                getX(),
                getY(),
                width - 76,
                getHeight(),
                (s -> {
                    if (ignoreUpdate) return;

                    parent.updateQuery();
                }),
                filterScreen);

        this.inequalityButton = new Button.Builder(Component.literal(inequalityType.getMessage()), null)
                .pos(getX() + width - 74, getY())
                .size(20, 20)
                .build();

        this.removeButton = new Button.Builder(Component.literal("🗑"), (button -> parent.removeWidget(this)))
                .pos(getX() + width - 20, getY())
                .size(20, 20)
                .build();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        entryInput.render(guiGraphics, mouseX, mouseY, partialTick);
        inequalityButton.render(guiGraphics, mouseX, mouseY, partialTick);
        removeButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (entryInput.isMouseOver(mouseX, mouseY)) {
            return entryInput.mouseClicked(mouseX, mouseY, button);
        } else if (removeButton.isMouseOver(mouseX, mouseY)) {
            return removeButton.mouseClicked(mouseX, mouseY, button);
        } else if (inequalityButton.isMouseOver(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                cycleInequality(1);
                return true;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                cycleInequality(-1);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (entryInput.isMouseOver(mouseX, mouseY)) {
            return entryInput.mouseReleased(mouseX, mouseY, button);
        } else if (inequalityButton.isMouseOver(mouseX, mouseY)) {
            return inequalityButton.mouseReleased(mouseX, mouseY, button);
        } else if (removeButton.isMouseOver(mouseX, mouseY)) {
            return removeButton.mouseReleased(mouseX, mouseY, button);
        }

        return false;
    }

    @Override
    public void updateY(int y) {
        setY(y);

        entryInput.setY(y);
        inequalityButton.setY(y);
        removeButton.setY(y);
    }

    @Override
    protected StatProviderAndFilterPair getFilterPair() {
        if (entryInput.getTextBoxInput().isEmpty()) return null;

        String value = entryInput.getTextBoxInput();
        Optional<StatFilter<T>> singleStatFilterOpt = getInequalityStatFilter(value, inequalityType);

        return singleStatFilterOpt
                .map(statFilter -> new StatProviderAndFilterPair<>(parent.getProvider(), statFilter))
                .orElse(null);
    }

    private void cycleInequality(int direction) {
        List<InequalityType> types = new ArrayList<>(List.of(InequalityType.values()));

        if (types.indexOf(inequalityType) + direction < 0) {
            inequalityType = types.getLast();
        } else if (types.indexOf(inequalityType) + direction == types.size()) {
            inequalityType = types.getFirst();
        } else {
            inequalityType = types.get(types.indexOf(inequalityType) + direction);
        }

        inequalityButton.setMessage(Component.literal(inequalityType.getMessage()));

        parent.updateQuery();
    }

    protected void setEntryInput(String input) {
        ignoreUpdate = true;
        entryInput.setTextBoxInput(input);
        ignoreUpdate = false;
    }

    protected void setInequalityType(InequalityType inequalityType) {
        this.inequalityType = inequalityType;

        inequalityButton.setMessage(Component.literal(inequalityType.getMessage()));
    }

    protected abstract Optional<StatFilter<T>> getInequalityStatFilter(String value, InequalityType inequalityType);

    protected enum InequalityType {
        GREATER_THAN(">"),
        GREATER_THAN_EQUAL(">="),
        LESS_THAN("<"),
        LESS_THAN_EQUAL("<=");

        private final String message;

        InequalityType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
