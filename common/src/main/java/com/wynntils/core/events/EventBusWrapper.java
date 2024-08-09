/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.events;

import com.wynntils.core.WynntilsMod;
import java.util.Arrays;
import java.util.Locale;
import net.neoforged.bus.BusBuilderImpl;
import net.neoforged.bus.EventBus;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;

public class EventBusWrapper extends EventBus {
    private EventBusWrapper(BusBuilderImpl busBuilder) {
        super(busBuilder);
    }

    public static IEventBus createEventBus() {
        if (WynntilsMod.isDevelopmentEnvironment()) {
            // In development, we want to catch events posted on the wrong thread,
            // as this can cause issues that are hard to debug
            return new DevelopmentEnvironment((BusBuilderImpl) BusBuilder.builder());
        } else {
            // Wrap the event bus to make sure we don't trigger registration errors in production
            return new EventBusWrapper((BusBuilderImpl) BusBuilder.builder());
        }
    }

    @Override
    public void register(Object target) {
        boolean anyEvents = Arrays.stream(target.getClass().getMethods())
                .anyMatch(method -> method.isAnnotationPresent(SubscribeEvent.class));

        // NeoForge EventBus does some sanity checking on registration, to help people forgetting to add @SubscribeEvent
        // This actually bites us, as sometimes we deliberately register objects without any events
        // (because they are of a certain class)
        if (!anyEvents) return;

        super.register(target);
    }

    private static final class DevelopmentEnvironment extends EventBusWrapper {
        private DevelopmentEnvironment(BusBuilderImpl busBuilder) {
            super(busBuilder);
        }

        @Override
        public <T extends Event> T post(T event) {
            Class<? extends Event> eventClass = event.getClass();
            EventThread threadAnnotation = eventClass.getDeclaredAnnotation(EventThread.class);
            String threadName = Thread.currentThread().getName();
            if (threadAnnotation == null) {
                // Events without annotation are only allowed on Render thread
                if (!threadName.equals("Render thread")) {
                    WynntilsMod.warn(
                            "Handling non-annotated event " + eventClass.getSimpleName() + " on thread " + threadName);
                }
            } else {
                // Make sure annotation matches the actual thread
                boolean threadOk =
                        switch (threadAnnotation.value()) {
                            case RENDER -> threadName.equals("Render thread");
                            case IO -> threadName.startsWith("Netty Client IO #");
                            case WORKER -> threadName.toLowerCase(Locale.ROOT).contains("pool");
                            case ANY -> true;
                        };
                if (!threadOk) {
                    WynntilsMod.warn("Handling event " + eventClass.getSimpleName() + " annotated as "
                            + threadAnnotation.value() + " on thread " + threadName);
                }
            }

            return super.post(event);
        }
    }
}
