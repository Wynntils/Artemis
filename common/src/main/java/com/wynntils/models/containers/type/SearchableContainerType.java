/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers.type;

import java.util.regex.Pattern;

public enum SearchableContainerType {
    BANK(Pattern.compile("§0\\[Pg. (\\d+)\\] §8(.*)'s?§0 Bank"), Pattern.compile("§f§lPage \\d+§a >§2>§a>§2>§a>"), 8),
    MISC_BUCKET(
            Pattern.compile("§0\\[Pg. (\\d+)\\] §8(.*)'s?§0 Misc. Bucket"),
            Pattern.compile("§f§lPage \\d+§a >§2>§a>§2>§a>"),
            8),
    GUILD_BANK(Pattern.compile(".+: Bank \\(.+\\)"), Pattern.compile("§a§lNext Page"), 27),
    MEMBER_LIST(Pattern.compile(".+: Members"), Pattern.compile("§a§lNext Page"), 28);

    private final Pattern titlePattern;
    private final Pattern nextItemPattern;
    private final int nextItemSlot;

    SearchableContainerType(Pattern titlePattern, Pattern nextItemPattern, int nextItemSlot) {
        this.titlePattern = titlePattern;
        this.nextItemPattern = nextItemPattern;
        this.nextItemSlot = nextItemSlot;
    }

    public int getNextItemSlot() {
        return nextItemSlot;
    }

    public Pattern getNextItemPattern() {
        return nextItemPattern;
    }

    public static SearchableContainerType getContainerType(String title) {
        for (SearchableContainerType type : SearchableContainerType.values()) {
            if (type.titlePattern.matcher(title).matches()) {
                return type;
            }
        }

        return null;
    }
}
