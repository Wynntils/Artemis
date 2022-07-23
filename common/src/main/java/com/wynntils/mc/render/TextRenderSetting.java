/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.render;

import com.wynntils.utils.objects.CommonColors;
import com.wynntils.utils.objects.CustomColor;

public record TextRenderSetting(
        float maxWidth, CustomColor customColor, FontRenderer.TextAlignment alignment, FontRenderer.TextShadow shadow) {

    public static final TextRenderSetting DEFAULT = new TextRenderSetting(
            0, CommonColors.WHITE, FontRenderer.TextAlignment.LEFT_ALIGNED, FontRenderer.TextShadow.NORMAL);

    public static TextRenderSetting getWithHorizontalAlignment(
            float maxWidth, CustomColor customColor, HorizontalAlignment horizontalAlignment) {
        switch (horizontalAlignment) {
            case Left -> {
                return new TextRenderSetting(
                        maxWidth, customColor, FontRenderer.TextAlignment.LEFT_ALIGNED, FontRenderer.TextShadow.NORMAL);
            }
            case Center -> {
                return new TextRenderSetting(
                        maxWidth,
                        customColor,
                        FontRenderer.TextAlignment.CENTER_ALIGNED,
                        FontRenderer.TextShadow.NORMAL);
            }
            case Right -> {
                return new TextRenderSetting(
                        maxWidth,
                        customColor,
                        FontRenderer.TextAlignment.RIGHT_ALIGNED,
                        FontRenderer.TextShadow.NORMAL);
            }
        }

        return new TextRenderSetting(
                maxWidth, customColor, FontRenderer.TextAlignment.LEFT_ALIGNED, FontRenderer.TextShadow.NORMAL);
    }

    public TextRenderSetting withTextShadow(FontRenderer.TextShadow textShadow) {
        return new TextRenderSetting(this.maxWidth, this.customColor, this.alignment, textShadow);
    }

    public TextRenderSetting withAlignment(FontRenderer.TextAlignment alignment) {
        return new TextRenderSetting(this.maxWidth, this.customColor, alignment, this.shadow);
    }

    public TextRenderSetting withCustomColor(CustomColor color) {
        return new TextRenderSetting(this.maxWidth, color, this.alignment, this.shadow);
    }

    public TextRenderSetting withMaxWidth(float maxWidth) {
        return new TextRenderSetting(maxWidth, this.customColor, this.alignment, this.shadow);
    }
}
