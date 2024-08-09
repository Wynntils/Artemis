/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.MenuEvent;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import java.util.regex.Pattern;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.INVENTORY)
public class GuildBankHotkeyFeature extends Feature {
    private static final Pattern MANAGE_TITLE_PATTERN = Pattern.compile(".+: Manage");
    private static final int GUILD_BANK_SLOT = 15;

    @RegisterKeyBind
    private final KeyBind guildBankKeybind =
            new KeyBind("Open Guild Bank", GLFW.GLFW_KEY_P, true, this::onOpenGuildBankKeyPress);

    private boolean openGuildBank = false;

    @SubscribeEvent
    public void onMenuOpenPre(MenuEvent.MenuOpenedEvent.Pre event) {
        if (!openGuildBank) return;

        openGuildBank = false;

        // We cannot use ContainerModel here, as it is too early in the event chain.
        StyledText title = StyledText.fromComponent(event.getTitle());
        if (title.matches(MANAGE_TITLE_PATTERN)) {
            event.setCanceled(true);

            AbstractContainerMenu container = event.getMenuType().create(event.getContainerId(), McUtils.inventory());
            ContainerUtils.clickOnSlot(GUILD_BANK_SLOT, event.getContainerId(), 0, container.getItems());
        }
    }

    private void onOpenGuildBankKeyPress() {
        openGuildBank = true;
        Handlers.Command.sendCommandImmediately("guild manage");
    }
}
