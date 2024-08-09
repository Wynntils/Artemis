/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers.containers.personal;

import com.wynntils.models.containers.type.HighlightableProfessionProperty;
import com.wynntils.models.containers.type.PersonalStorageType;
import java.util.List;
import java.util.regex.Pattern;

public class CharacterBankContainer extends PersonalStorageContainer implements HighlightableProfessionProperty {
    private static final Pattern TITLE_PATTERN = Pattern.compile("\uDAFF\uDFF0\uE00F\uDAFF\uDF68\uF001");
    private static final int FINAL_PAGE = 10;
    private static final List<Integer> QUICK_JUMP_DESTINATIONS = List.of(1, 3, 5, 7, 9);

    public CharacterBankContainer() {
        super(TITLE_PATTERN, PersonalStorageType.CHARACTER_BANK, FINAL_PAGE, QUICK_JUMP_DESTINATIONS);
    }
}
