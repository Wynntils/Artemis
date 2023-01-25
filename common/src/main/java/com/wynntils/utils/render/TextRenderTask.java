/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render;

public final class TextRenderTask {
    private String text;
    private TextRenderSetting setting;

    public TextRenderTask(String text, TextRenderSetting setting) {
        this.text = text;
        this.setting = setting;
    }

    public String getText() {
        return text;
    }

    public TextRenderSetting getSetting() {
        return setting;
    }

    public TextRenderTask setSetting(TextRenderSetting setting) {
        this.setting = setting;
        return this;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "TextRenderTask[" + "text=" + text + ", " + "setting=" + setting + ']';
    }
}
