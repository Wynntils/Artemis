/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.athena.event;

import com.wynntils.core.events.EventThread;
import net.neoforged.bus.api.Event;

@EventThread(EventThread.Type.WORKER)
public class AthenaLoginEvent extends Event {}
