/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.net.event;

import com.wynntils.core.net.UrlId;
import net.neoforged.bus.api.Event;

public class NetResultProcessedEvent extends Event {
    public static final class ForUrlId extends NetResultProcessedEvent {
        private final UrlId urlId;

        public ForUrlId(UrlId urlId) {
            this.urlId = urlId;
        }

        public UrlId getUrlId() {
            return urlId;
        }
    }

    public static final class ForLocalFile extends NetResultProcessedEvent {
        private final String localFileName;

        public ForLocalFile(String localFileName) {
            this.localFileName = localFileName;
        }

        public String getLocalFileName() {
            return localFileName;
        }
    }
}
