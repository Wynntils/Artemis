/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.overlays;

import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.overlays.OverlayPosition;
import com.wynntils.core.features.overlays.TextOverlay;
import com.wynntils.core.features.overlays.annotations.OverlayInfo;
import com.wynntils.core.features.overlays.sizes.GuiScaledOverlaySize;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.extension.EntityExtension;
import com.wynntils.models.abilities.event.TotemEvent;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.RenderedStringUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.OVERLAYS)
public class ShamanTotemTrackingFeature extends UserFeature {
    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final ShamanTotemTimerOverlay shamanTotemTimerOverlay = new ShamanTotemTimerOverlay();

    @Config
    public boolean highlightShamanTotems = true;

    @Config
    public static CustomColor firstTotemColor = CommonColors.WHITE;

    @Config
    public static CustomColor secondTotemColor = CommonColors.BLUE;

    @Config
    public static CustomColor thirdTotemColor = CommonColors.RED;

    private static final int ENTITY_GLOWING_FLAG = 6;

    @SubscribeEvent
    public void onTotemSummoned(TotemEvent.Summoned e) {
        if (!highlightShamanTotems) return;

        int totemNumber = e.getTotemNumber();
        ArmorStand totemAS = e.getTotemEntity();

        CustomColor color =
                switch (totemNumber) {
                    case 1 -> firstTotemColor;
                    case 2 -> secondTotemColor;
                    case 3 -> thirdTotemColor;
                    default -> throw new IllegalArgumentException(
                            "totemNumber should be 1, 2, or 3! (color switch in #onTotemSummoned in ShamanTotemTrackingFeature");
                };

        ((EntityExtension) totemAS).setGlowColor(color);

        totemAS.setGlowingTag(true);
        totemAS.setSharedFlag(ENTITY_GLOWING_FLAG, true);
    }

    public static class ShamanTotemTimerOverlay extends TextOverlay {
        @Config
        public static TotemTrackingDetail totemTrackingDetail = TotemTrackingDetail.COORDS;

        @Config
        public static ChatFormatting firstTotemTextColor = ChatFormatting.WHITE;

        @Config
        public static ChatFormatting secondTotemTextColor = ChatFormatting.BLUE;

        @Config
        public static ChatFormatting thirdTotemTextColor = ChatFormatting.RED;

        private static final ChatFormatting[] TOTEM_COLORS = {
            firstTotemTextColor, secondTotemTextColor, thirdTotemTextColor
        };

        protected ShamanTotemTimerOverlay() {
            super(
                    new OverlayPosition(
                            275,
                            -5,
                            VerticalAlignment.Top,
                            HorizontalAlignment.Right,
                            OverlayPosition.AnchorSection.TopRight),
                    new GuiScaledOverlaySize(120, 35));
        }

        @Override
        public String getTemplate() {
            return Models.ShamanTotem.getActiveTotems().stream()
                    .filter(Objects::nonNull)
                    .map(totem -> TOTEM_COLORS[totem.getTotemNumber() - 1]
                            + totemTrackingDetail
                                    .getTemplate()
                                    .replaceAll("%d", String.valueOf(totem.getTotemNumber())))
                    .collect(Collectors.joining("\n"));
        }

        @Override
        public String getPreviewTemplate() {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < TotemTrackingDetail.values().length; i++) {
                builder.append(TOTEM_COLORS[i])
                        .append(TotemTrackingDetail.values()[i].getPreviewTemplate())
                        .append("\n");
            }

            return builder.toString();
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

    public enum TotemTrackingDetail {
        NONE(
                "{IF_STR(EQ_STR(SHAMAN_TOTEM_STATE(%d); \"ACTIVE\"); CONCAT(CONCAT(\"Totem %d (\"; STRING(SHAMAN_TOTEM_TIME_LEFT(%d)));\" s)\"); IF_STR(EQ_STR(SHAMAN_TOTEM_STATE(%d); \"SUMMONED\"); \"Totem %d summoned\"; \"\"))}",
                "Totem 1 (10 s)"),
        COORDS(
                "{IF_STR(EQ_STR(SHAMAN_TOTEM_STATE(%d); \"ACTIVE\"); CONCAT(CONCAT(CONCAT(CONCAT(\"Totem %d (\"; STRING(SHAMAN_TOTEM_TIME_LEFT(%d)));\" s)\"); \" \"); SHAMAN_TOTEM_LOCATION(%d)); IF_STR(EQ_STR(SHAMAN_TOTEM_STATE(%d); \"SUMMONED\"); \"Totem %d summoned\"; \"\"))}",
                "Totem 2 (15 s) [1425, 12, 512]"),
        DISTANCE(
                "{IF_STR(EQ_STR(SHAMAN_TOTEM_STATE(%d); \"ACTIVE\"); CONCAT(CONCAT(CONCAT(CONCAT(\"Totem %d (\"; STRING(SHAMAN_TOTEM_TIME_LEFT(%d)));\" s, \"); STRING(SHAMAN_TOTEM_DISTANCE(%d):0)); \" m)\"); IF_STR(EQ_STR(SHAMAN_TOTEM_STATE(%d); \"SUMMONED\"); \"Totem %d summoned\"; \"\"))}",
                "Totem 3 (7s, 10 m)");

        private final String template;
        private final String previewTemplate;

        TotemTrackingDetail(String template, String previewTemplate) {
            this.template = template;
            this.previewTemplate = previewTemplate;
        }

        public String getTemplate() {
            return template;
        }

        public String getPreviewTemplate() {
            return previewTemplate;
        }
    }
}
