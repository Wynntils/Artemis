/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.map;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.RenderState;
import com.wynntils.core.consumers.overlays.annotations.OverlayInfo;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.overlays.minimap.CoordinateOverlay;
import com.wynntils.overlays.minimap.MinimapOverlay;
import com.wynntils.overlays.minimap.TerritoryOverlay;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.MAP)
public class MinimapFeature extends Feature {
    @OverlayInfo(renderType = RenderEvent.ElementType.GUI, renderAt = RenderState.PRE)
    public final MinimapOverlay minimapOverlay = new MinimapOverlay();

    @OverlayInfo(renderAt = RenderState.PRE, renderType = RenderEvent.ElementType.GUI)
    private final Overlay coordinatesOverlay = new CoordinateOverlay();

    @OverlayInfo(renderAt = RenderState.PRE, renderType = RenderEvent.ElementType.GUI)
    private final Overlay territoryOverlay = new TerritoryOverlay();

    @RegisterKeyBind
    public final KeyBind zoomIn =
            new KeyBind("Zoom in Minimap", GLFW.GLFW_KEY_EQUAL, false, () -> minimapOverlay.scale(0.95f));

    @RegisterKeyBind
    public final KeyBind zoomOut =
            new KeyBind("Zoom out of Minimap", GLFW.GLFW_KEY_MINUS, false, () -> minimapOverlay.scale(1.05f));
}
