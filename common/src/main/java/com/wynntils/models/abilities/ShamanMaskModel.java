/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.abilities;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.models.abilities.event.ShamanMaskTitlePacketEvent;
import com.wynntils.models.abilities.type.ShamanMaskType;
import com.wynntils.models.worlds.WorldStateModel;
import com.wynntils.models.worlds.event.WorldStateEvent;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ShamanMaskModel extends Model {
    private static final Pattern MASK_PATTERN = Pattern.compile("§cMask of the (Coward|Lunatic|Fanatic)");

    private ShamanMaskType currentMaskType = ShamanMaskType.NONE;

    public ShamanMaskModel(WorldStateModel worldStateModel) {
        super(List.of(worldStateModel));
    }

    @SubscribeEvent
    public void onTitle(SubtitleSetTextEvent event) {
        StyledText title = StyledText.fromComponent(event.getComponent());

        if (title.contains("Mask of the ") || title.contains("➤")) {
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
        Matcher matcher = title.getMatcher(MASK_PATTERN);

        ShamanMaskType parsedMask = ShamanMaskType.NONE;

        if (matcher.matches()) {
            parsedMask = ShamanMaskType.find(matcher.group(1));
        } else {
            for (ShamanMaskType type : ShamanMaskType.values()) {
                if (type.getParseString() == null) continue;

                if (title.contains(type.getParseString())) {
                    parsedMask = type;
                    break;
                }
            }
        }

        currentMaskType = parsedMask;
    }

    public ShamanMaskType getCurrentMaskType() {
        return currentMaskType;
    }
}
