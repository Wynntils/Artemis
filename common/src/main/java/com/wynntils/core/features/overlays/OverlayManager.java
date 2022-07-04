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

    private static final List<SectionCoordinates> sections = new ArrayList<>(9);

    public static void registerOverlay(Overlay overlay, OverlayInfo overlayInfo) {
        overlayInfoMap.put(overlay, overlayInfo);
    }

    public static void disableOverlays(List<Overlay> overlays) {
        enabledOverlays.removeIf(overlays::contains);
    }

    public static void enableOverlays(List<Overlay> overlays, boolean ignoreState) {
        if (ignoreState) {
            enabledOverlays.addAll(overlays);
        } else {
            enabledOverlays.addAll(overlays.stream()
                    .filter(overlay -> overlayInfoMap.get(overlay).enabled())
                    .toList());
        }
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

            if (annotation.renderType() != event.getType()) {
                continue;
            }

            if (annotation.renderAt() == OverlayInfo.RenderState.Replace) {
                if (renderState != OverlayInfo.RenderState.Pre) {
                    continue;
                }
                event.setCanceled(true);
            } else {
                if (annotation.renderAt() != renderState) {
                    continue;
                }
            }

            overlay.render(event.getPoseStack(), event.getPartialTicks(), event.getWindow());
        }
    }

    public static void init() {
        WynntilsMod.getEventBus().register(OverlayManager.class);
    }

    @SubscribeEvent
    public static void onResizeEvent(DisplayResizeEvent event) {
        calculateSections();
    }

    // Calculate the sections when loading is finished (this acts as a "game loaded" event)
    @SubscribeEvent
    public static void gameInitEvent(TitleScreenInitEvent event) {
        calculateSections();
    }

    private static void calculateSections() {
        Window window = McUtils.mc().getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();

        int wT = width / 3;
        int hT = height / 3;

        sections.clear();
        for (int h = 0; h < 3; h++) {
            for (int w = 0; w < 3; w++) {
                sections.add(new SectionCoordinates(w * wT, h * hT, (w + 1) * wT, (h + 1) * hT));
            }
        }
    }

    public static SectionCoordinates getSection(OverlayPosition.AnchorSection section) {
        return sections.get(section.getIndex());
    }

    public static List<SectionCoordinates> getSections() {
        return sections;
    }
}
