/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.objects.item.render;

import com.wynntils.mc.event.SlotRenderEvent;

public interface TextOverlayItem {
    String getText(SlotRenderEvent e);

    int getColor(SlotRenderEvent e);
}
