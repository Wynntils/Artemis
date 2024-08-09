/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.actionbar;

public interface ActionBarSegmentMatcher {
    /**
     * Parse the given action bar string and return the corresponding segment.
     * The segment can be found anywhere in the string, and is not required to be at the start.
     * @param actionBar The action bar string to parse
     * @return The parsed segment, or null if the string does not match the segment
     */
    ActionBarSegment parse(String actionBar);
}
