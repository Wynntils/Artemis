/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.players;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.StartDisabled;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.PlayerRenderLayerEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;

@StartDisabled
@ConfigCategory(Category.PLAYERS)
public class PlayerArmorHidingFeature extends Feature {
    @Persisted
    public final Config<Boolean> hideHelmets = new Config<>(true);

    @Persisted
    public final Config<Boolean> hideChestplates = new Config<>(true);

    @Persisted
    public final Config<Boolean> hideLeggings = new Config<>(true);

    @Persisted
    public final Config<Boolean> hideBoots = new Config<>(true);

    @Persisted
    public final Config<Boolean> showCosmetics = new Config<>(true);

    @SubscribeEvent
    public void onPlayerArmorRender(PlayerRenderLayerEvent.Armor event) {
        switch (event.getSlot()) {
            case HEAD -> {
                if (!hideHelmets.get()) return;
                if (!showCosmetics.get()) { // helmet is hidden regardless, no extra logic needed
                    event.setCanceled(true);
                    return;
                }

                // only cancel if helmet item isn't cosmetic - all helmet skins use a pickaxe texture
                ItemStack headItem = event.getPlayer().getItemBySlot(event.getSlot());
                if (headItem.getItem() != Items.DIAMOND_PICKAXE) event.setCanceled(true);
                return;
            }
            case CHEST -> {
                if (hideChestplates.get()) event.setCanceled(true);
                return;
            }
            case LEGS -> {
                if (hideLeggings.get()) event.setCanceled(true);
                return;
            }
            case FEET -> {
                if (hideBoots.get()) event.setCanceled(true);
                return;
            }
        }
    }
}
