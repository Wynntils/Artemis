/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.debug;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.StartDisabled;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.models.items.WynnItem;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.RenderedStringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@StartDisabled
@ConfigCategory(Category.DEBUG)
public class ItemDebugTooltipsFeature extends Feature {
    @SubscribeEvent
    public void onTooltipPre(ItemTooltipRenderEvent.Pre event) {
        Optional<WynnItem> wynnItemOpt = Models.Item.getWynnItem(event.getItemStack());
        if (wynnItemOpt.isEmpty()) return;
        WynnItem wynnItem = wynnItemOpt.get();

        List<Component> tooltips =
                LoreUtils.appendTooltip(event.getItemStack(), event.getTooltips(), getTooltipAddon(wynnItem));
        event.setTooltips(tooltips);
    }

    private List<Component> getTooltipAddon(WynnItem wynnItem) {
        List<Component> addon = new ArrayList<>();

        // We do not want to treat § in the text as formatting codes later on
        StyledText rawString =
                StyledText.fromUnformattedString(wynnItem.toString()).replaceAll("§", "%");
        List<StyledText> wrappedDescription = Arrays.stream(RenderedStringUtils.wrapTextBySize(rawString, 150))
                .toList();
        if (!KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT) && wrappedDescription.size() > 4) {
            wrappedDescription = new ArrayList<>(wrappedDescription.subList(0, 3));
            wrappedDescription.add(StyledText.fromString("..."));
            wrappedDescription.add(StyledText.fromString("Press Right Shift for all"));
        }

        for (StyledText line : wrappedDescription) {
            addon.add(line.getComponent().withStyle(ChatFormatting.DARK_GREEN));
        }

        return addon;
    }
}
