/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.consumers.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.overlays.annotations.OverlayGroup;
import com.wynntils.core.consumers.overlays.annotations.OverlayInfo;
import com.wynntils.core.mod.CrashReportManager;
import com.wynntils.core.mod.type.CrashType;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.OverlayGroupHolder;
import com.wynntils.mc.event.DisplayResizeEvent;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.mc.event.TitleScreenInitEvent;
import com.wynntils.screens.overlays.placement.OverlayManagementScreen;
import com.wynntils.screens.overlays.selection.OverlaySelectionScreen;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class OverlayManager extends Manager {
    private static final MultiBufferSource.BufferSource BUFFER_SOURCE =
            MultiBufferSource.immediate(new ByteBufferBuilder(256));

    private final Map<Feature, List<Overlay>> overlayParentMap = new HashMap<>();
    private final Map<Overlay, OverlayInfoContainer> overlayInfoMap = new HashMap<>();
    private final Map<Feature, List<OverlayGroupHolder>> overlayGroupMap = new HashMap<>();

    private final Set<Overlay> enabledOverlays = new HashSet<>();

    private final List<SectionCoordinates> sections = new ArrayList<>(9);
    private final Map<Class<?>, Integer> profilingTimes = new HashMap<>();
    private final Map<Class<?>, Integer> profilingCounts = new HashMap<>();

    public OverlayManager(CrashReportManager crashReportManager) {
        super(List.of(crashReportManager));

        addCrashCallbacks();
    }

    // region Initialization and Registration

    private void registerOverlay(
            Overlay overlay,
            Feature parent,
            RenderEvent.ElementType elementType,
            RenderState renderAt,
            boolean enabledByDefault) {
        overlayParentMap.putIfAbsent(parent, new LinkedList<>());
        overlayParentMap.get(parent).add(overlay);

        overlayInfoMap.put(overlay, new OverlayInfoContainer(parent, elementType, renderAt, enabledByDefault));
    }

    private void unregisterOverlay(Overlay overlay) {
        overlayParentMap.get(overlayInfoMap.get(overlay).parent()).remove(overlay);

        WynntilsMod.unregisterEventListener(overlay);

        overlayInfoMap.remove(overlay);
        enabledOverlays.remove(overlay);
    }

    public void disableOverlays(Feature parent) {
        overlayParentMap.getOrDefault(parent, List.of()).forEach(this::disableOverlay);
    }

    public void disableOverlay(Overlay disabledOverlay) {
        if (!isEnabled(disabledOverlay)) return;

        enabledOverlays.remove(disabledOverlay);
        WynntilsMod.unregisterEventListener(disabledOverlay);

        enabledOverlays.forEach(
                overlay -> overlay.getConfigOptionFromString("userEnabled").ifPresent(overlay::callOnConfigUpdate));
    }

    public void enableOverlays(Feature parent) {
        overlayParentMap.getOrDefault(parent, List.of()).forEach(this::enableOverlay);
    }

    public void enableOverlay(Overlay enableOverlay) {
        if (!enableOverlay.shouldBeEnabled() || isEnabled(enableOverlay)) return;

        enabledOverlays.add(enableOverlay);
        WynntilsMod.registerEventListener(enableOverlay);

        enabledOverlays.forEach(
                overlay -> overlay.getConfigOptionFromString("userEnabled").ifPresent(overlay::callOnConfigUpdate));
    }

    public void discoverOverlays(Feature feature) {
        Field[] overlayFields = FieldUtils.getFieldsWithAnnotation(feature.getClass(), OverlayInfo.class);
        for (Field overlayField : overlayFields) {
            try {
                Object fieldValue = FieldUtils.readField(overlayField, feature, true);

                if (!(fieldValue instanceof Overlay overlay)) {
                    throw new RuntimeException("A non-Overlay class was marked with OverlayInfo annotation.");
                }

                OverlayInfo annotation = overlayField.getAnnotation(OverlayInfo.class);
                Managers.Overlay.registerOverlay(
                        overlay, feature, annotation.renderType(), annotation.renderAt(), annotation.enabled());

                assert !overlay.getTranslatedName().startsWith("feature.wynntils.")
                        : "Fix i18n for " + overlay.getTranslatedName();
            } catch (IllegalAccessException e) {
                WynntilsMod.error("Unable to get field " + overlayField, e);
            }
        }
    }

    public void discoverOverlayGroups(Feature feature) {
        List<OverlayGroupHolder> holders = Stream.of(feature.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(OverlayGroup.class))
                .map(field -> {
                    OverlayGroup annotation = field.getAnnotation(OverlayGroup.class);
                    return new OverlayGroupHolder(
                            field, feature, annotation.renderType(), annotation.renderAt(), annotation.instances());
                })
                .toList();

        holders.forEach(this::createOverlayGroupWithDefaults);

        overlayGroupMap.put(feature, holders);
    }

    // endregion

    // region Overlay Groups

    public void createOverlayGroupWithDefaults(OverlayGroupHolder holder) {
        recreateGroupOverlaysWithIds(
                holder,
                IntStream.rangeClosed(1, holder.getDefaultCount()).boxed().toList());
    }

    public void createOverlayGroupWithIds(OverlayGroupHolder holder, List<Integer> ids) {
        recreateGroupOverlaysWithIds(holder, ids);
    }

    public int extendOverlayGroup(OverlayGroupHolder holder) {
        List<Integer> ids = holder.getOverlays().stream()
                .map(overlay -> ((DynamicOverlay) overlay).getId())
                .collect(Collectors.toList());

        int newId = 1;
        while (ids.contains(newId)) {
            newId++;
        }

        ids.add(newId);

        recreateGroupOverlaysWithIds(holder, ids);

        return newId;
    }

    public void removeIdFromOverlayGroup(OverlayGroupHolder holder, int id) {
        List<Integer> ids = holder.getOverlays().stream()
                .map(overlay -> ((DynamicOverlay) overlay).getId())
                .collect(Collectors.toList());

        ids.remove((Integer) id);

        recreateGroupOverlaysWithIds(holder, ids);
    }

    private void recreateGroupOverlaysWithIds(OverlayGroupHolder holder, List<Integer> ids) {
        holder.getOverlays().forEach(this::unregisterOverlay);

        holder.initGroup(ids);

        holder.getOverlays()
                .forEach(overlay -> registerOverlay(
                        overlay, holder.getParent(), holder.getElementType(), holder.getRenderState(), true));
    }

    // endregion

    // region Ticking
    @SubscribeEvent
    public void onTick(TickEvent event) {
        enabledOverlays.forEach(Overlay::tick);
    }

    // endregion

    // region Rendering

    @SubscribeEvent
    public void onRenderPre(RenderEvent.Pre event) {
        McUtils.mc().getProfiler().push("preRenOverlay");
        renderOverlays(event, RenderState.PRE);
        McUtils.mc().getProfiler().pop();
    }

    @SubscribeEvent
    public void onRenderPost(RenderEvent.Post event) {
        McUtils.mc().getProfiler().push("postRenOverlay");
        renderOverlays(event, RenderState.POST);
        McUtils.mc().getProfiler().pop();
    }

    private void renderOverlays(RenderEvent event, RenderState renderState) {
        boolean showPreview = false;
        boolean renderNonSelected = true;
        boolean shouldRender = true;
        Overlay selectedOverlay = null;

        if (McUtils.mc().screen instanceof OverlayManagementScreen screen) {
            shouldRender = false;
            showPreview = screen.showPreview();
            renderNonSelected = screen.shouldRenderAllOverlays();
            selectedOverlay = screen.getSelectedOverlay();
        } else if (McUtils.mc().screen instanceof OverlaySelectionScreen screen) {
            if (screen.renderingPreview()) {
                showPreview = true;
                renderNonSelected = screen.shouldShowOverlays();
                selectedOverlay = screen.getSelectedOverlay();
            }
        }

        List<Overlay> crashedOverlays = new LinkedList<>();
        for (Overlay overlay : enabledOverlays) {
            OverlayInfoContainer renderInfo = overlayInfoMap.get(overlay);

            if (renderInfo.elementType() != event.getType()) {
                continue;
            }

            if (renderInfo.renderState() == RenderState.REPLACE) {
                if (renderState != RenderState.PRE) {
                    continue;
                }
                if (event instanceof ICancellableEvent cancellableEvent) {
                    cancellableEvent.setCanceled(true);
                }
            } else {
                if (renderInfo.renderState() != renderState) {
                    continue;
                }
            }

            try {
                if (showPreview) {
                    if (selectedOverlay != null && overlay != selectedOverlay && !renderNonSelected) continue;

                    overlay.renderPreview(
                            event.getPoseStack(), BUFFER_SOURCE, event.getDeltaTracker(), event.getWindow());
                } else if (shouldRender) {
                    long startTime = System.currentTimeMillis();
                    overlay.render(event.getPoseStack(), BUFFER_SOURCE, event.getDeltaTracker(), event.getWindow());
                    logProfilingData(startTime, overlay);
                }
            } catch (Throwable t) {
                RenderUtils.disableScissor();
                RenderUtils.clearMask();

                // We can't disable it right away since that will cause ConcurrentModificationException
                crashedOverlays.add(overlay);

                WynntilsMod.reportCrash(
                        CrashType.OVERLAY,
                        overlay.getTranslatedName(),
                        overlay.getClass().getName(),
                        "render",
                        t);
            }
        }

        BUFFER_SOURCE.endBatch();

        // Hopefully we have none :)
        for (Overlay overlay : crashedOverlays) {
            overlay.getConfigOptionFromString("userEnabled")
                    .ifPresent(config -> ((Config<Boolean>) config).setValue(false));
        }
    }

    // endregion

    // region Profiling

    private void logProfilingData(long startTime, Overlay overlay) {
        long endTime = System.currentTimeMillis();
        int timeSpent = (int) (endTime - startTime);
        int allTime = profilingTimes.getOrDefault(overlay.getClass(), 0);
        profilingTimes.put(overlay.getClass(), allTime + timeSpent);

        int allCount = profilingCounts.getOrDefault(overlay.getClass(), 0);
        profilingCounts.put(overlay.getClass(), allCount + 1);
    }

    public Map<Class<?>, Integer> getProfilingTimes() {
        return profilingTimes;
    }

    public Map<Class<?>, Integer> getProfilingCounts() {
        return profilingCounts;
    }

    public void resetProfiling() {
        profilingTimes.clear();
        profilingCounts.clear();
    }

    // endregion

    // region Sections
    @SubscribeEvent
    public void onResizeEvent(DisplayResizeEvent event) {
        calculateSections();
    }

    // Calculate the sections when loading is finished (this acts as a "game loaded" event)
    @SubscribeEvent
    public void gameInitEvent(TitleScreenInitEvent.Post event) {
        calculateSections();
    }

    private void calculateSections() {
        Window window = McUtils.window();
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

    // endregion

    private void addCrashCallbacks() {
        Managers.CrashReport.registerCrashContext("Loaded Overlays", () -> {
            StringBuilder result = new StringBuilder();

            for (Overlay overlay : enabledOverlays) {
                result.append("\n\t\t").append(overlay.getTranslatedName());
            }

            return result.toString();
        });
    }

    public SectionCoordinates getSection(OverlayPosition.AnchorSection section) {
        return sections.get(section.getIndex());
    }

    public List<SectionCoordinates> getSections() {
        return sections;
    }

    public Set<Overlay> getOverlays() {
        return overlayInfoMap.keySet();
    }

    public <T extends Overlay> T getOverlay(Class<T> overlayClass) {
        return (T) overlayInfoMap.keySet().stream()
                .filter(overlayClass::isInstance)
                .findFirst()
                .orElse(null);
    }

    public Feature getOverlayParent(Overlay overlay) {
        return overlayInfoMap.get(overlay).parent();
    }

    public boolean isEnabled(Overlay overlay) {
        return enabledOverlays.contains(overlay);
    }

    public boolean isEnabledByDefault(Overlay overlay) {
        return overlayInfoMap.get(overlay).enabledByDefault();
    }

    public List<OverlayGroupHolder> getOverlayGroups() {
        return overlayGroupMap.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    public List<Overlay> getFeatureOverlays(Feature feature) {
        return overlayParentMap.getOrDefault(feature, List.of());
    }

    public List<OverlayGroupHolder> getFeatureOverlayGroups(Feature feature) {
        return overlayGroupMap.getOrDefault(feature, List.of());
    }

    private record OverlayInfoContainer(
            Feature parent, RenderEvent.ElementType elementType, RenderState renderState, boolean enabledByDefault) {}
}
