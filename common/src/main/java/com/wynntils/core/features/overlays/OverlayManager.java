/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.features.overlays;

import com.mojang.blaze3d.platform.Window;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.features.overlays.annotations.OverlayInfo;
import com.wynntils.mc.event.DisplayResizeEvent;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.event.TitleScreenInitEvent;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.utils.objects.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class OverlayManager {
    private static final Map<Overlay, OverlayInfo> overlayInfoMap = new HashMap<>();

    private static final Set<Overlay> enabledOverlays = new HashSet<>();

    private static final List<Pair<Coordinate, Coordinate>> ninths = new ArrayList<>(9);

    public static void registerOverlay(Overlay overlay, OverlayInfo overlayInfo) {
        overlayInfoMap.put(overlay, overlayInfo);

        if (overlayInfo.enabled()) {
            enabledOverlays.add(overlay);
        }
    }

    public static void disableOverlays(List<Overlay> overlays) {
        enabledOverlays.removeIf(overlays::contains);
    }

    public static void enableOverlays(List<Overlay> overlays) {
        enabledOverlays.addAll(overlays);
    }

    @SubscribeEvent
    public static void onRenderPre(RenderEvent.Pre event) {
        McUtils.mc().getProfiler().push("preRenOverlay");
        renderOverlays(event, OverlayInfo.RenderState.Pre);
        McUtils.mc().getProfiler().pop();
    }

    @SubscribeEvent
    public static void onRenderPost(RenderEvent.Post event) {
        McUtils.mc().getProfiler().push("postRenOverlay");
        renderOverlays(event, OverlayInfo.RenderState.Post);
        McUtils.mc().getProfiler().pop();
    }

    private static void renderOverlays(RenderEvent event, OverlayInfo.RenderState renderState) {
        for (Overlay overlay : enabledOverlays) {
            OverlayInfo annotation = overlayInfoMap.get(overlay);

            if (renderState != annotation.renderAt() || event.getType() != annotation.renderType()) continue;

            if (annotation.cancelRender()) {
                event.setCanceled(true);
            }

            overlay.render(event.getPoseStack(), event.getPartialTicks(), event.getWindow());
        }
    }

    public static void init() {
        WynntilsMod.getEventBus().register(OverlayManager.class);
    }

    @SubscribeEvent
    public static void onResizeEvent(DisplayResizeEvent event) {
        calculateNinths();
    }

    // Calculate the ninths when loading is finished (this acts as a "game loaded" event)
    @SubscribeEvent
    public static void gameInitEvent(TitleScreenInitEvent event) {
        calculateNinths();
    }

    private static void calculateNinths() {
        Window window = McUtils.mc().getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();

        int wT = width / 3;
        int hT = height / 3;

        ninths.clear();
        for (int h = 0; h < 3; h++) {
            for (int w = 0; w < 3; w++) {
                ninths.add(new Pair<>(new Coordinate(w * wT, h * hT), new Coordinate((w + 1) * wT, (h + 1) * hT)));
            }
        }
    }

    public static Pair<Coordinate, Coordinate> getNinth(OverlayPosition.AnchorNinth ninth) {
        return ninths.get(ninth.getIndex());
    }

    public static List<Pair<Coordinate, Coordinate>> getNinths() {
        return ninths;
    }
}
