/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.scoreboard;

import java.util.regex.Pattern;

public record SegmentMatcher(Pattern headerPattern) {
    public static SegmentMatcher fromPattern(String pattern) {
        return new SegmentMatcher(Pattern.compile(pattern));
    }
}
