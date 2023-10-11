/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.annotators.game;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.items.items.game.GameItem;
import com.wynntils.models.items.items.game.MaterialItem;
import com.wynntils.models.profession.type.MaterialProfile;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.item.ItemStack;

public final class MaterialAnnotator extends GameItemAnnotator {
    private static final Pattern MATERIAL_PATTERN = Pattern.compile("^§f(.*) ([^ ]+)§6 \\[§e✫((?:§8)?✫(?:§8)?)✫§6\\]$");

    @Override
    public GameItem getAnnotation(ItemStack itemStack, StyledText name, List<StyledText> lore, int emeraldPrice) {
        Matcher matcher = name.getMatcher(MATERIAL_PATTERN);
        if (!matcher.matches()) return null;

        String materialSource = matcher.group(1);
        String resourceType = matcher.group(2);
        String tierIndicator = matcher.group(3);
        int tier =
                switch (tierIndicator) {
                    case "§8✫" -> 1;
                    case "✫§8" -> 2;
                    case "✫" -> 3;
                    default -> {
                        WynntilsMod.warn("Cannot parse tier from material: " + name);
                        yield 1;
                    }
                };

        MaterialProfile materialProfile = MaterialProfile.lookup(materialSource, resourceType, tier);
        if (materialProfile == null) return null;

        return new MaterialItem(emeraldPrice, materialProfile);
    }
}
