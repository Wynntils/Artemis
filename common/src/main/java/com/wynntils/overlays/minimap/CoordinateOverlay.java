/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays.minimap;

import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.consumers.overlays.TextOverlay;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.features.map.MinimapFeature;
import com.wynntils.handlers.actionbar.event.ActionBarRenderEvent;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.neoforged.bus.api.SubscribeEvent;

public class CoordinateOverlay extends TextOverlay {
    private static final String TEMPLATE = "{x(my_loc):0} {y(my_loc):0} {z(my_loc):0}";
    private static final String TEMPLATE_COLORED = "&c{x(my_loc):0} &a{y(my_loc):0} &9{z(my_loc):0}";
    private static final String UNMAPPED_TEMPLATE =
            "{if_str(in_mapped_area;concat(str(x(my_loc));\" \";str(y(my_loc));\" \";str(z(my_loc)));\"\")}";
    private static final String UNMAPPED_TEMPLATE_COLORED =
            "{if_str(in_mapped_area;concat(\"&c\";str(x(my_loc));\" &a\";str(y(my_loc));\" &9\";str(z(my_loc)));\"\")}";

    @Persisted
    public final Config<Boolean> shouldBeColored = new Config<>(false);

    @Persisted
    public final Config<Boolean> shouldDisplayOriginal = new Config<>(true);

    public CoordinateOverlay() {
        super(
                new OverlayPosition(
                        0, 6, VerticalAlignment.TOP, HorizontalAlignment.LEFT, OverlayPosition.AnchorSection.TOP_LEFT),
                new OverlaySize(130, 20),
                HorizontalAlignment.CENTER,
                VerticalAlignment.MIDDLE);
    }

    @SubscribeEvent
    public void onActionBarRender(ActionBarRenderEvent event) {
        event.setRenderCoordinates(this.shouldDisplayOriginal.get());
    }

    @Override
    public String getTemplate() {
        if (Managers.Feature.getFeatureInstance(MinimapFeature.class)
                        .minimapOverlay
                        .hideWhenUnmapped
                        .get()
                == MinimapOverlay.UnmappedOption.MINIMAP_AND_COORDS) {
            return this.shouldBeColored.get() ? UNMAPPED_TEMPLATE_COLORED : UNMAPPED_TEMPLATE;
        } else {
            return this.shouldBeColored.get() ? TEMPLATE_COLORED : TEMPLATE;
        }
    }

    @Override
    public String getPreviewTemplate() {
        return this.shouldBeColored.get() ? TEMPLATE_COLORED : TEMPLATE;
    }
}
