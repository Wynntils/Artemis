/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.features;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.features.debug.ConnectionProgressFeature;
import com.wynntils.features.debug.KeyBindTestFeature;
import com.wynntils.features.debug.PacketDebuggerFeature;
import com.wynntils.features.internal.FixPacketBugsFeature;
import com.wynntils.features.internal.LootrunFeature;
import com.wynntils.features.user.DialogueOptionOverrideFeature;
import com.wynntils.features.user.EmeraldPouchHotkeyFeature;
import com.wynntils.features.user.GammabrightFeature;
import com.wynntils.features.user.HealthPotionBlockerFeature;
import com.wynntils.features.user.IngredientPouchHotkeyFeature;
import com.wynntils.features.user.ItemGuessFeature;
import com.wynntils.features.user.ItemHighlightFeature;
import com.wynntils.features.user.ItemScreenshotFeature;
import com.wynntils.features.user.ItemStatInfoFeature;
import com.wynntils.features.user.MountHorseHotkeyFeature;
import com.wynntils.features.user.MythicBlockerFeature;
import com.wynntils.features.user.PlayerGhostTransparencyFeature;
import com.wynntils.features.user.SoulPointTimerFeature;
import com.wynntils.features.user.WynncraftButtonFeature;
import com.wynntils.mc.event.ClientTickEvent;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.utils.CrashReportManager;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wc.utils.WynnUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Loads {@link Feature}s */
public class FeatureRegistry {
    private static final Map<Class<? extends Feature>, Feature> FEATURES = new HashMap<>();
    private static final Map<Class<? extends Overlay>, Overlay> OVERLAYS = new HashMap<>();

    public static void registerFeature(Feature feature) {
        if (feature instanceof Overlay overlay) {
            OVERLAYS.put(overlay.getClass(), overlay);
        }

        FEATURES.put(feature.getClass(), feature);
    }

    public static void registerFeatures(List<Feature> features) {
        features.forEach(FeatureRegistry::registerFeature);
    }

    public static Map<Class<? extends Feature>, Feature> getFeatures() {
        return FEATURES;
    }

    public static Map<Class<? extends Overlay>, Overlay> getOverlays() {
        return OVERLAYS;
    }

    public static void init() {
        // debug
        registerFeature(new ConnectionProgressFeature());
        registerFeature(new KeyBindTestFeature());
        registerFeature(new PacketDebuggerFeature());

        registerFeature(new DialogueOptionOverrideFeature());
        registerFeature(new EmeraldPouchHotkeyFeature());
        registerFeature(new FixPacketBugsFeature());
        registerFeature(new GammabrightFeature());
        registerFeature(new HealthPotionBlockerFeature());
        registerFeature(new IngredientPouchHotkeyFeature());
        registerFeature(new ItemGuessFeature());
        registerFeature(new ItemHighlightFeature());
        registerFeature(new ItemScreenshotFeature());
        registerFeature(new ItemStatInfoFeature());
        registerFeature(new LootrunFeature());
        registerFeature(new MountHorseHotkeyFeature());
        registerFeature(new MythicBlockerFeature());
        registerFeature(new PlayerGhostTransparencyFeature());
        registerFeature(new SoulPointTimerFeature());
        registerFeature(new WynncraftButtonFeature());

        FEATURES.values().forEach(Feature::init);

        FEATURES.get(LootrunFeature.class).disable(); // TODO: Make it an option to not enable a feature on init

        WynntilsMod.getEventBus().register(OverlayListener.class);

        addCrashCallbacks();
    }

    private static void addCrashCallbacks() {
        CrashReportManager.registerCrashContext(new CrashReportManager.ICrashContext() {
            @Override
            public String name() {
                return "Loaded Features";
            }

            @Override
            public Object generate() {
                StringBuilder result = new StringBuilder();

                for (Feature feature : FEATURES.values()) {
                    if (feature.isEnabled()) {
                        result.append("\n\t\t").append(feature.getName());
                    }
                }

                return result.toString();
            }
        });

        CrashReportManager.registerCrashContext(new CrashReportManager.ICrashContext() {
            @Override
            public String name() {
                return "Loaded Overlays";
            }

            @Override
            public Object generate() {
                StringBuilder result = new StringBuilder();

                for (Overlay overlay : OVERLAYS.values()) {
                    if (overlay.isEnabled()) {
                        result.append("\n\t\t").append(overlay.getName());
                    }
                }

                return result.toString();
            }
        });
    }

    private static class OverlayListener { // TODO create a enum map for overlays instead of this
        @SubscribeEvent
        public static void onTick(ClientTickEvent e) {
            if (e.getTickPhase() == ClientTickEvent.Phase.END) {
                for (Overlay overlay : OVERLAYS.values()) {
                    overlay.tick();
                }
            }
        }

        @SubscribeEvent
        public static void onRenderPre(RenderEvent.Pre e) {
            if (!WynnUtils.onServer()) // || !McUtils.mc().playerController.isSpectator())
            return;

            McUtils.mc().getProfiler().push("preRenOverlay");
            for (Overlay overlay : OVERLAYS.values()) {
                if (!overlay.visible) continue;
                // if (!overlay.active) continue;

                if (overlay.hookElements.length != 0) {
                    boolean contained = false;
                    for (RenderEvent.ElementType type : overlay.hookElements) {
                        if (e.getType() == type) {
                            contained = true;
                            break;
                        }
                    }

                    if (contained && overlay.visible) {
                        McUtils.mc().getProfiler().push(overlay.getName());
                        overlay.render(e);
                        McUtils.mc().getProfiler().pop();
                    }
                }
            }

            McUtils.mc().getProfiler().pop();

            // McIf.mc().getTextureManager().bindTexture(ICONS);
        }

        @SubscribeEvent
        public static void onRenderPost(RenderEvent.Post e) {
            if (!WynnUtils.onServer()) // || !McUtils.mc().playerController.isSpectator())
            return;

            McUtils.mc().getProfiler().push("postRenOverlay");

            for (Overlay overlay : OVERLAYS.values()) {
                if (!overlay.visible)
                    // if (!overlay.active) continue;

                    if (overlay.hookElements.length != 0) {
                        for (RenderEvent.ElementType type : overlay.hookElements) {
                            if (e.getType() == type) {
                                McUtils.mc().getProfiler().push(overlay.getName());
                                overlay.render(e);
                                McUtils.mc().getProfiler().pop();
                                break;
                            }
                        }
                    }
            }

            McUtils.mc().getProfiler().pop();
        }
    }
}
