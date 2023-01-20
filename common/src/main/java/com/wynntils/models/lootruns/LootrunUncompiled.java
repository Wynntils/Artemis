/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.lootruns;

import com.wynntils.models.lootruns.type.LootrunNote;
import com.wynntils.models.lootruns.type.LootrunPath;
import java.io.File;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;

public record LootrunUncompiled(LootrunPath path, Set<BlockPos> chests, List<LootrunNote> notes, File file) {}
