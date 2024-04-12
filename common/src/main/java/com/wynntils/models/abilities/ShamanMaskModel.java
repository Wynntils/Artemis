/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilities;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.mc.event.TitleSetTextEvent;
import com.wynntils.models.abilities.event.ShamanMaskTitlePacketEvent;
import com.wynntils.models.abilities.type.ShamanMaskType;
import com.wynntils.models.worlds.event.WorldStateEvent;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ShamanMaskModel extends Model {
    private static final Pattern AWAKENED_PATTERN = Pattern.compile("^§[0-9a-f]§lAwakened$");

    private ShamanMaskType currentMaskType = ShamanMaskType.NONE;

    public ShamanMaskModel() {
        super(List.of());
    }

    @SubscribeEvent
    public void onTitle(TitleSetTextEvent event) {
        StyledText title = StyledText.fromComponent(event.getComponent());

        if (title.matches(AWAKENED_PATTERN)) {
            currentMaskType = ShamanMaskType.AWAKENED;
            ShamanMaskTitlePacketEvent maskEvent = new ShamanMaskTitlePacketEvent();
            WynntilsMod.postEvent(maskEvent);

            if (maskEvent.isCanceled()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onSubtitle(SubtitleSetTextEvent event) {
        StyledText title = StyledText.fromComponent(event.getComponent());

        if (title.contains("➤")) {
            parseMask(title);
            ShamanMaskTitlePacketEvent maskEvent = new ShamanMaskTitlePacketEvent();
            WynntilsMod.postEvent(maskEvent);

            if (maskEvent.isCanceled()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        currentMaskType = ShamanMaskType.NONE;
    }

    private void parseMask(StyledText title) {
        ShamanMaskType parsedMask = ShamanMaskType.NONE;

        for (ShamanMaskType type : ShamanMaskType.values()) {
            if (type.getParseString() == null) continue;

            if (title.contains(type.getParseString())) {
                parsedMask = type;
                break;
            }
        }

        currentMaskType = parsedMask;
    }

    public ShamanMaskType getCurrentMaskType() {
        return currentMaskType;
    }
}
