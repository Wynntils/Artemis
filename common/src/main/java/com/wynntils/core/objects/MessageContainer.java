/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.objects;

import com.wynntils.features.user.overlays.GameUpdateOverlayFeature;
import com.wynntils.mc.render.TextRenderSetting;
import com.wynntils.mc.render.TextRenderTask;

public class MessageContainer {
    TextRenderTask message;
    long endTime;

    public MessageContainer(String message) {
        this.message = new TextRenderTask(message, TextRenderSetting.DEFAULT);
        this.endTime = System.currentTimeMillis()
                + (long) (GameUpdateOverlayFeature.getInstance().GameUpdateOverlay.messageTimeLimit * 1000);
    }

    public MessageContainer(String message, long endTime) {
        this.message = new TextRenderTask(message, TextRenderSetting.DEFAULT);
        this.endTime = endTime;
    }

    public MessageContainer(TextRenderTask message) {
        this.message = message;
        this.endTime = System.currentTimeMillis()
                + (long) (GameUpdateOverlayFeature.getInstance().GameUpdateOverlay.messageTimeLimit * 1000);
    }

    public long getRemainingTime() {
        return endTime - System.currentTimeMillis();
    }

    public long getEndTime() {
        return endTime;
    }

    public TextRenderTask getRenderTask() {
        return message;
    }

    public MessageContainer editMessage(String newMessage) {
        this.message.setText(newMessage);
        return this;
    }

    public void resetRemainingTime() {
        this.endTime = System.currentTimeMillis()
                + (long) (GameUpdateOverlayFeature.getInstance().GameUpdateOverlay.messageTimeLimit * 1000);
    }
}
