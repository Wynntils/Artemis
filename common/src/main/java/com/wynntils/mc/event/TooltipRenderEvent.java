/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.neoforged.bus.api.Event;

/**
 * This event is fired when a tooltip is about to be rendered, when calculating the position of the tooltip.
 * You can use this event to change the positioner of the tooltip.
 */
public class TooltipRenderEvent extends Event {
    private ClientTooltipPositioner positioner;

    public TooltipRenderEvent() {
        this.positioner = null;
    }

    public ClientTooltipPositioner getPositioner() {
        return positioner;
    }

    public void setPositioner(ClientTooltipPositioner positioner) {
        this.positioner = positioner;
    }
}
