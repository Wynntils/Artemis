/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.utils.keybinds;

import com.google.common.collect.Lists;
import com.wynntils.core.WynntilsMod;
import com.wynntils.mc.mixin.accessors.OptionsAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/** Registers and handles keybinds */
public class KeyManager {
    private static final List<KeyHolder> keyHolders = new ArrayList<>();

    public static void init() {
        WynntilsMod.getProvider().registerEndTickEvent(client -> triggerKeybinds());
    }

    public static void registerKeybinding(KeyHolder toAdd) {
        if (hasName(toAdd.getName())) {
            throw new IllegalStateException(
                    "Can not add " + toAdd + " since the name already exists");
        }

        keyHolders.add(toAdd);

        Options options = Minecraft.getInstance().options;
        KeyMapping[] keyMappings = options.keyMappings;

        List<KeyMapping> newKeyMappings = Lists.newArrayList(keyMappings);
        newKeyMappings.add(toAdd.getKeybind());

        synchronized (options) {
            ((OptionsAccessor) options).setKeyBindMixins(newKeyMappings.toArray(new KeyMapping[0]));
        }
    }

    public static void unregisterKeybind(KeyHolder toAdd) {
        if (keyHolders.remove(toAdd)) {
            Options options = Minecraft.getInstance().options;
            KeyMapping[] keyMappings = options.keyMappings;

            List<KeyMapping> newKeyMappings = Lists.newArrayList(keyMappings);
            newKeyMappings.remove(toAdd.getKeybind());

            synchronized (options) {
                ((OptionsAccessor) options)
                        .setKeyBindMixins(newKeyMappings.toArray(new KeyMapping[0]));
            }
        }
    }

    public static void triggerKeybinds() {
        keyHolders.forEach(
                k -> {
                    if (k.isFirstPress()) {
                        if (k.getKeybind().consumeClick()) {
                            k.onPress();
                        }
                        return;
                    }

                    if (k.getKeybind().isDown()) {
                        k.onPress();
                    }
                });
    }

    private static boolean hasName(String name) {
        return keyHolders.stream().anyMatch(k -> k.getName().equals(name));
    }
}
