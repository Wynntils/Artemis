/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays.lootrun;

import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.TextOverlay;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;

public class LootrunBeaconCountOverlay extends TextOverlay {
    private static final String TEMPLATE = "{if_string(string_equals(lootrun_state; \"NOT_RUNNING\"); \"\"; \\nCONCAT("
            + "\"§eYellow: \"; string(lootrun_beacon_count(\"YELLOW\"));\"\\n§9Blue: \"; "
            + "string(lootrun_beacon_count(\"BLUE\"));\"\\n§5Purple: \"; string(lootrun_beacon_count(\"PURPLE\"));\"\\n§7Gray: \"; "
            + "string(lootrun_beacon_count(\"GRAY\"));\"/3\";\"\\n§6Orange: \"; string(lootrun_beacon_count(\"ORANGE\"));\"\";\"\\n§bAqua: \"; "
            + "string(lootrun_beacon_count(\"AQUA\"));\"\";\"\\n§8Dark Gray: \"; string(lootrun_beacon_count(\"DARK_GRAY\"));\"/1\";\"\\n§aGreen: \"; "
            + "string(lootrun_beacon_count(\"GREEN\"));\"\";\"\\n§cRed: \"; string(lootrun_beacon_count(\"RED\"));\" (\"; "
            + "string(lootrun_red_beacon_challenge_count);\")\";\"\\n§fWhite: \"; string(lootrun_beacon_count(\"WHITE\"));\"/1\";\"\\n§4R§ca§6i§en§ab§2o§bw§9: §f\"; "
            + "string(lootrun_beacon_count(\"RAINBOW\"))))}\\n";

    private static final String PREVIEW_TEMPLATE =
            """
            §eYellow: 0
            §9Blue: 0
            §5Purple: 0
            §7Gray: 0/3
            §6Orange: 0
            §bAqua: 0
            §8Dark Gray: 0/1
            §aGreen: 0
            §cRed: 0 (0)
            §fWhite: 0/1
            §4R§ca§6i§en§ab§2o§bw§9: 0
            """;

    public LootrunBeaconCountOverlay() {
        super(
                new OverlayPosition(
                        120,
                        5,
                        VerticalAlignment.TOP,
                        HorizontalAlignment.LEFT,
                        OverlayPosition.AnchorSection.MIDDLE_LEFT),
                250,
                300);
    }

    @Override
    protected String getTemplate() {
        return TEMPLATE;
    }

    @Override
    protected String getPreviewTemplate() {
        return PREVIEW_TEMPLATE;
    }
}
