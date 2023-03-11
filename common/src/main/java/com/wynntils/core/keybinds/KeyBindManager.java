/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.keybinds;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.features.Feature;
import com.wynntils.core.features.properties.RegisterKeyBind;
import com.wynntils.mc.event.InventoryKeyPressEvent;
import com.wynntils.mc.event.InventoryMouseClickedEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.mc.mixin.accessors.OptionsAccessor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Pair;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.reflect.FieldUtils;

/** Registers and handles keybinds */
public final class KeyBindManager extends Manager {
    private final Set<KeyBind> enbaledKeyBinds = ConcurrentHashMap.newKeySet();
    private final Map<Feature, List<KeyBind>> keyBinds = new ConcurrentHashMap<>();

    public KeyBindManager() {
        super(List.of());
    }

    public void discoverKeyBinds(Feature feature) {
        for (Field f : FieldUtils.getFieldsWithAnnotation(feature.getClass(), RegisterKeyBind.class)) {
            if (!f.getType().equals(KeyBind.class)) continue;

            try {
                KeyBind keyBind = (KeyBind) FieldUtils.readField(f, feature, true);
                keyBinds.putIfAbsent(feature, new LinkedList<>());
                keyBinds.get(feature).add(keyBind);
            } catch (Exception e) {
                WynntilsMod.error(
                        "Failed to register KeyBind " + f.getName() + " in "
                                + feature.getClass().getName(),
                        e);
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent e) {
        triggerKeybinds();
    }

    @SubscribeEvent
    public void onKeyPress(InventoryKeyPressEvent e) {
        checkAllKeyBinds(keyBind -> {
            if (keyBind.getKeyMapping().matches(e.getKeyCode(), e.getScanCode())) {
                keyBind.onInventoryPress(e.getHoveredSlot());
            }
        });
    }

    @SubscribeEvent
    public void onMousePress(InventoryMouseClickedEvent e) {
        checkAllKeyBinds(keyBind -> {
            if (keyBind.getKeyMapping().matchesMouse(e.getButton())) {
                keyBind.onInventoryPress(e.getHoveredSlot());
            }
        });
    }

    public void enableFeatureKeyBinds(Feature feature) {
        if (!keyBinds.containsKey(feature)) return;

        for (KeyBind keyBind : keyBinds.get(feature)) {
            registerKeybind(feature, keyBind);
        }
    }

    public void disableFeatureKeyBinds(Feature feature) {
        if (!keyBinds.containsKey(feature)) return;

        for (KeyBind keyBind : keyBinds.get(feature)) {
            unregisterKeybind(feature, keyBind);
        }
    }

    private void registerKeybind(Feature parent, KeyBind toAdd) {
        if (hasName(toAdd.getName())) {
            throw new IllegalStateException(
                    "Can not add keybind " + toAdd.getName() + " since the name already exists");
        }

        KeyMapping keyMapping = toAdd.getKeyMapping();

        synchronized (McUtils.options()) {
            enbaledKeyBinds.add(toAdd);

            Options options = McUtils.options();
            KeyMapping[] keyMappings = options.keyMappings;

            List<KeyMapping> newKeyMappings = Lists.newArrayList(keyMappings);
            newKeyMappings.add(keyMapping);

            ((OptionsAccessor) options).setKeyBindMixins(newKeyMappings.toArray(new KeyMapping[0]));
        }

        // Bind keybind to its default key, however, this might get overwritten by options loading later
        keyMapping.setKey(keyMapping.getDefaultKey());
        KeyMapping.resetMapping();
    }

    private void unregisterKeybind(Feature parent, KeyBind toRemove) {
        if (!enbaledKeyBinds.remove(toRemove)) return;

        KeyMapping keyMapping = toRemove.getKeyMapping();

        synchronized (McUtils.options()) {
            Options options = McUtils.options();
            KeyMapping[] keyMappings = options.keyMappings;

            List<KeyMapping> newKeyMappings = Lists.newArrayList(keyMappings);
            newKeyMappings.remove(toRemove.getKeyMapping());

            ((OptionsAccessor) options).setKeyBindMixins(newKeyMappings.toArray(new KeyMapping[0]));
        }

        // Unbind keybind
        keyMapping.setKey(InputConstants.UNKNOWN);
        KeyMapping.resetMapping();
    }

    private void triggerKeybinds() {
        checkAllKeyBinds(keyBind -> {
            if (keyBind.onlyFirstPress()) {
                while (keyBind.getKeyMapping().consumeClick()) {
                    keyBind.onPress();
                }
            } else if (keyBind.getKeyMapping().isDown()) {
                keyBind.onPress();
            }
        });
    }

    private void checkAllKeyBinds(Consumer<KeyBind> checkKeybind) {
        if (!Managers.Connection.onServer()) return;

        List<Pair<Feature, KeyBind>> crashedKeyBinds = new LinkedList<>();

        for (Feature parent : keyBinds.keySet()) {
            for (KeyBind keyBind : keyBinds.get(parent)) {
                try {
                    checkKeybind.accept(keyBind);
                } catch (Throwable t) {
                    WynntilsMod.error("Exception when handling key bind " + parent, t);
                    WynntilsMod.warn("This key bind will be disabled");
                    McUtils.sendMessageToClient(Component.literal(
                                    "Wynntils error: Key bind " + parent + " has crashed and will be disabled")
                            .withStyle(ChatFormatting.RED));
                    // We can't disable it right away since that will cause ConcurrentModificationException
                    crashedKeyBinds.add(Pair.of(parent, keyBind));
                }
            }
        }

        // Hopefully we have none :)
        for (Pair<Feature, KeyBind> keyBindPair : crashedKeyBinds) {
            unregisterKeybind(keyBindPair.key(), keyBindPair.value());
        }
    }

    private boolean hasName(String name) {
        return enbaledKeyBinds.stream().anyMatch(k -> k.getName().equals(name));
    }

    /**
     * Note: this is called directly from a mixin!
     */
    public static void initKeyMapping(String category, Map<String, Integer> categorySortOrder) {
        if (categorySortOrder.containsKey(category)) return;

        int max = 0;

        for (int val : categorySortOrder.values()) {
            if (val > max) {
                max = val;
            }
        }

        categorySortOrder.put(category, max + 1);
    }
}
