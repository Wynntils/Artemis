/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gear2;

import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.concepts.Skill;
import com.wynntils.utils.type.Pair;
import java.util.List;
import java.util.Optional;

// FIXME: quest should be a type, not a string
public record GearRequirements(
        int level, Optional<ClassType> classType, List<Pair<Skill, Integer>> skills, Optional<String> quest) {}
