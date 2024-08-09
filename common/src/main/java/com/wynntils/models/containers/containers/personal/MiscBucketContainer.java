/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers.containers.personal;

import com.wynntils.models.containers.type.HighlightableProfessionProperty;
import com.wynntils.models.containers.type.PersonalStorageType;
import java.util.List;
import java.util.regex.Pattern;

public class MiscBucketContainer extends PersonalStorageContainer implements HighlightableProfessionProperty {
    private static final Pattern TITLE_PATTERN = Pattern.compile("\uDAFF\uDFF0\uE00F\uDAFF\uDF68\uF004");
    private static final int FINAL_PAGE = 10;
    private static final List<Integer> QUICK_JUMP_DESTINATIONS = List.of(1, 3, 5, 7, 9);

    public MiscBucketContainer() {
        super(TITLE_PATTERN, PersonalStorageType.MISC_BUCKET, FINAL_PAGE, QUICK_JUMP_DESTINATIONS);
    }
}
