/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.labels.event;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.labels.type.LabelInfo;
import java.util.Optional;
import net.minecraft.world.entity.Display;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * This event is called when data of a {@link net.minecraft.world.entity.Display.TextDisplay} is changed.
 * <p>
 * The various events are cancellable.
 */
public abstract class TextDisplayChangedEvent extends Event implements ICancellableEvent {
    private final Display.TextDisplay textDisplay;
    private final LabelInfo labelInfo;

    protected TextDisplayChangedEvent(Display.TextDisplay textDisplay, LabelInfo labelInfo) {
        this.textDisplay = textDisplay;
        this.labelInfo = labelInfo;
    }

    public Display.TextDisplay getTextDisplay() {
        return textDisplay;
    }

    public Optional<LabelInfo> getLabelInfo() {
        return Optional.ofNullable(labelInfo);
    }

    public static class Text extends TextDisplayChangedEvent {
        private StyledText text;

        public Text(Display.TextDisplay textDisplay, LabelInfo labelInfo, StyledText text) {
            super(textDisplay, labelInfo);
            this.text = text;
        }

        public StyledText getText() {
            return text;
        }

        public void setText(StyledText text) {
            this.text = text;
        }
    }
}
