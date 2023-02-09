/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.profession.objects;

public record ProfessionProgress(int level, float progress) {
    public static final ProfessionProgress NO_PROGRESS = new ProfessionProgress(0, 0);
}
