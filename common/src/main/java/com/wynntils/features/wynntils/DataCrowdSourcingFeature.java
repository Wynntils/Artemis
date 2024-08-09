/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.wynntils;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.crowdsource.type.CrowdSourcedDataType;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.config.HiddenConfig;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.ConfirmedBoolean;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.WYNNTILS)
public class DataCrowdSourcingFeature extends Feature {
    @Persisted
    public final HiddenConfig<Map<CrowdSourcedDataType, ConfirmedBoolean>> crowdSourcedDataTypeEnabledMap =
            new HiddenConfig<>(new TreeMap<>());

    @SubscribeEvent
    public void onWorldChange(WorldStateEvent event) {
        if (event.getNewState() != WorldState.WORLD) return;

        Map<CrowdSourcedDataType, ConfirmedBoolean> enabledMap = crowdSourcedDataTypeEnabledMap.get();
        List<CrowdSourcedDataType> nonConfirmedDataTypes = Arrays.stream(CrowdSourcedDataType.values())
                .filter(dataType -> !enabledMap.containsKey(dataType))
                .toList();

        if (nonConfirmedDataTypes.isEmpty()) return;

        MutableComponent component =
                Component.literal("Wynntils Crowd Sourcing\n").withStyle(ChatFormatting.AQUA);
        component.append(Component.literal(
                        """
                        Wynntils can collect data during your
                        gameplay to improve the mod.
                        This data does not contain any personal information,
                        and is only stored locally on your computer.
                        To share this data with the Wynntils team,
                        you must copy it to the clipboard and send it to us,
                        during periods when we are collecting data.
                        """)
                .withStyle(ChatFormatting.GRAY));

        component.append(Component.literal("\nThe following data types are not confirmed to be collected:\n"));
        for (CrowdSourcedDataType dataType : nonConfirmedDataTypes) {
            component.append(Component.literal(" - ").withStyle(ChatFormatting.GRAY));
            component.append(Component.literal(dataType.getTranslatedName()).withStyle(ChatFormatting.YELLOW));
            component.append(Component.literal("\n"));
        }

        component.append(
                Component.literal(
                        "\nYou can confirm or deny the collection of each data type in the Wynntils Crowd Sourcing Screen, which you can access from the Wynntils Menu."));

        McUtils.sendMessageToClient(component);
    }
}
