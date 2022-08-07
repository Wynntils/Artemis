/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import static com.wynntils.mc.utils.InventoryUtils.MouseClickType.RIGHT_CLICK;

import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import com.wynntils.core.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyHolder;
import com.wynntils.core.notifications.NotificationManager;
import com.wynntils.mc.render.TextRenderSetting;
import com.wynntils.mc.render.TextRenderTask;
import com.wynntils.mc.utils.InventoryUtils;
import com.wynntils.mc.utils.InventoryUtils.EmeraldPouch;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.utils.objects.CommonColors;
import com.wynntils.wc.utils.WynnUtils;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

@FeatureInfo(stability = Stability.STABLE)
public class EmeraldPouchHotkeyFeature extends UserFeature {

    @RegisterKeyBind
    private final KeyHolder emeraldPouchKeybind = new KeyHolder(
            "Open Emerald Pouch",
            GLFW.GLFW_KEY_UNKNOWN,
            "Wynntils",
            true,
            EmeraldPouchHotkeyFeature::onOpenPouchKeyPress);

    private static void onOpenPouchKeyPress() {
        if (!WynnUtils.onWorld()) return;

        Player player = McUtils.player();
        List<EmeraldPouch> emeraldPouches = InventoryUtils.getEmeraldPouches(player.getInventory());

        if (emeraldPouches.isEmpty()) {
            NotificationManager.queueMessage(new TextRenderTask(
                    ChatFormatting.RED + I18n.get("feature.wynntils.emeraldPouchKeybind.noPouch"),
                    TextRenderSetting.DEFAULT.withCustomColor(CommonColors.RED)));
        } else {
            EmeraldPouch emeraldPouch = findSelectableEmeraldPouch(emeraldPouches);
            if (emeraldPouch != null) {
                // We found exactly one usable emerald pouch
                int slotNumber = emeraldPouch.getSlotNumber();

                if (slotNumber < 9) {
                    slotNumber += 36; // Raw slot numbers, remap if in hotbar
                }

                InventoryUtils.sendInventorySlotMouseClick(slotNumber, emeraldPouch.getStack(), RIGHT_CLICK);
            } else {
                // We found more than one filled pouch, cannot choose between them
                NotificationManager.queueMessage(new TextRenderTask(
                        ChatFormatting.RED + I18n.get("feature.wynntils.emeraldPouchKeybind.multipleFilled"),
                        TextRenderSetting.DEFAULT.withCustomColor(CommonColors.RED)));
            }
        }
    }

    private static EmeraldPouch findSelectableEmeraldPouch(List<EmeraldPouch> emeraldPouches) {
        EmeraldPouch largestEmpty = null;
        EmeraldPouch foundNonEmpty = null;

        for (EmeraldPouch pouch : emeraldPouches) {
            if (pouch.isEmpty()) {
                if (largestEmpty == null || pouch.getCapacity() > largestEmpty.getCapacity()) {
                    largestEmpty = pouch;
                }
            } else {
                if (foundNonEmpty != null) {
                    // Multiple filled pouches found, we can't chose between them
                    return null;
                } else {
                    foundNonEmpty = pouch;
                }
            }
        }

        if (foundNonEmpty != null) {
            // We found just a single, non-empty pouch, so use that
            return foundNonEmpty;
        } else {
            // As long as emeraldPouches was non-empty, we should have either at least
            // one non-empty pouch, or at least one empty pouch. Return the empty pouch
            // with the largest capacity.
            assert (largestEmpty != null);
            return largestEmpty;
        }
    }
}
