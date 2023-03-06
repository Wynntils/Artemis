/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.overlays;

import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.overlays.OverlayPosition;
import com.wynntils.core.features.overlays.TextOverlay;
import com.wynntils.core.features.overlays.annotations.OverlayInfo;
import com.wynntils.core.features.overlays.sizes.GuiScaledOverlaySize;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.utils.mc.RenderedStringUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ConfigCategory(Category.OVERLAYS)
public class MobTotemTrackingFeature extends UserFeature {
    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final MobTotemTimerOverlay mobTotemTimerOverlay = new MobTotemTimerOverlay();

    public static class MobTotemTimerOverlay extends TextOverlay {
        private static final String TEMPLATE =
                "{MOB_TOTEM_OWNER(%d)}'s Mob Totem [{MOB_TOTEM_DISTANCE(%d):0} m] ({MOB_TOTEM_TIME_LEFT(%d)})";

        protected MobTotemTimerOverlay() {
            super(
                    new OverlayPosition(
                            330,
                            -5,
                            VerticalAlignment.Top,
                            HorizontalAlignment.Right,
                            OverlayPosition.AnchorSection.TopRight),
                    new GuiScaledOverlaySize(120, 35));
        }

        @Override
        public String getTemplate() {
            return IntStream.rangeClosed(1, Models.MobTotem.getMobTotems().size())
                    .mapToObj(i -> TEMPLATE.replaceAll("%d", String.valueOf(i)))
                    .collect(Collectors.joining("\n"));
        }

        @Override
        public String getPreviewTemplate() {
            return "Player's Mob Totem [16 m] (4:11)";
        }

        @Override
        protected String[] calculateTemplateValue(String template) {
            return Arrays.stream(super.calculateTemplateValue(template))
                    .map(s -> RenderedStringUtils.trySplitOptimally(s, this.getWidth()))
                    .map(s -> s.split("\n"))
                    .flatMap(Arrays::stream)
                    .toArray(String[]::new);
        }
    }
}
