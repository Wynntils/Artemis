/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.mobtotem;

import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.labels.event.TextDisplayChangedEvent;
import com.wynntils.mc.event.RemoveEntitiesEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.mc.PosUtils;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.entity.Display;
import net.neoforged.bus.api.SubscribeEvent;

public class MobTotemModel extends Model {
    private static final Pattern MOB_TOTEM_TEXT =
            Pattern.compile("^§f§l(?<user>.*)'s§6§l Mob Totem\\n§c§l(?<timer>[0-9]+:[0-9]+)$");

    private final Map<Integer, MobTotem> mobTotems = new LinkedHashMap<>();

    public MobTotemModel() {
        super(List.of());
    }

    @SubscribeEvent
    public void onTotemRename(TextDisplayChangedEvent.Text event) {
        if (!Models.WorldState.onWorld()) return;

        Display.TextDisplay textDisplay = event.getTextDisplay();
        StyledText text = event.getText();

        // If a new mob totem just appeared, add it to the unstarted list
        Matcher totemMatcher = text.getMatcher(MOB_TOTEM_TEXT);
        if (!totemMatcher.matches()) return;

        int mobTotemId = textDisplay.getId();

        // If the totem is already in the list, don't add it again
        mobTotems.putIfAbsent(mobTotemId, new MobTotem(PosUtils.newPosition(textDisplay), totemMatcher.group("user")));
        mobTotems.get(mobTotemId).setTimerString(totemMatcher.group("timer"));
    }

    @SubscribeEvent
    public void onTotemDestroy(RemoveEntitiesEvent e) {
        if (!Models.WorldState.onWorld()) return;

        e.getEntityIds().forEach(mobTotems::remove);
    }

    @SubscribeEvent
    public void onWorldChange(WorldStateEvent e) {
        mobTotems.clear();
    }

    public List<MobTotem> getMobTotems() {
        return mobTotems.values().stream()
                .sorted(Comparator.comparing(MobTotem::getOwner))
                .toList();
    }

    public MobTotem getMobTotem(int index) {
        if (index < 0 || index >= mobTotems.size()) {
            return null;
        }

        return getMobTotems().get(index);
    }
}
