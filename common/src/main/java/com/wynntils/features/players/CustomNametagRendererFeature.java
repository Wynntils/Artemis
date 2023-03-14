/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.players;

import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.features.UserFeature;
import com.wynntils.mc.event.NametagRenderEvent;
import com.wynntils.mc.event.RenderLevelEvent;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.players.WynntilsUser;
import com.wynntils.models.players.type.AccountType;
import com.wynntils.screens.gearviewer.GearViewerScreen;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.wynn.RaycastUtils;
import com.wynntils.utils.wynn.WynnItemMatchers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.PLAYERS)
public class CustomNametagRendererFeature extends UserFeature {
    // how much larger account tags should be relative to gear lines
    private static final float ACCOUNT_TYPE_MULTIPLIER = 1.5f;
    private static final float NAMETAG_HEIGHT = 0.25875f;

    @RegisterConfig
    public final Config<Boolean> hideAllNametags = new Config<>(false);

    @RegisterConfig
    public final Config<Boolean> showGearOnHover = new Config<>(true);

    @RegisterConfig
    public final Config<Float> customNametagScale = new Config<>(0.5f);

    private Player hitPlayerCache = null;

    @SubscribeEvent
    public void onNameTagRender(NametagRenderEvent event) {
        if (hideAllNametags.get()) {
            event.setCanceled(true);
            return;
        }

        // If we are viewing this player's gears, do not show plus info
        if (McUtils.mc().screen instanceof GearViewerScreen gearViewerScreen
                && gearViewerScreen.getPlayer() == event.getEntity()) {
            return;
        }

        List<CustomNametag> nametags = new ArrayList<>();

        if (showGearOnHover.get()) {
            addGearNametags(event, nametags);
        }

        addAccountTypeNametag(event, nametags);

        // need to handle the rendering ourselves
        if (!nametags.isEmpty()) {
            event.setCanceled(true);
            drawNametags(event, nametags);
        }
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelEvent.Pre event) {
        Optional<Player> hitPlayer = RaycastUtils.getHoveredPlayer();
        hitPlayerCache = hitPlayer.orElse(null);
    }

    private void addGearNametags(NametagRenderEvent event, List<CustomNametag> nametags) {
        LocalPlayer player = McUtils.player();

        if (hitPlayerCache != event.getEntity()) return;

        if (!Models.Player.isLocalPlayer(player)) return;

        ItemStack heldItem = hitPlayerCache.getMainHandItem();
        MutableComponent handComp = getItemComponent(heldItem);
        if (handComp != null) nametags.add(new CustomNametag(handComp, customNametagScale.get()));

        for (ItemStack armorStack : hitPlayerCache.getArmorSlots()) {
            MutableComponent armorComp = getItemComponent(armorStack);
            if (armorComp != null) nametags.add(new CustomNametag(armorComp, customNametagScale.get()));
        }
    }

    private static MutableComponent getItemComponent(ItemStack itemStack) {
        if (itemStack == null || itemStack == ItemStack.EMPTY) return null;

        // This must specifically NOT be normalized; the ֎ is significant
        String gearName = ComponentUtils.getUnformatted(itemStack.getHoverName());
        MutableComponent description = WynnItemMatchers.getNonGearDescription(itemStack, gearName);
        if (description != null) return description;

        GearInfo gearInfo = Models.Gear.getGearInfoFromApiName(gearName);
        if (gearInfo == null) return null;

        return Component.literal(gearInfo.name()).withStyle(gearInfo.tier().getChatFormatting());
    }

    private void addAccountTypeNametag(NametagRenderEvent event, List<CustomNametag> nametags) {
        WynntilsUser user = Models.Player.getUser(event.getEntity().getUUID());
        if (user == null) return;
        AccountType accountType = user.accountType();
        if (accountType.getComponent() == null) return;

        nametags.add(new CustomNametag(accountType.getComponent(), customNametagScale.get() * ACCOUNT_TYPE_MULTIPLIER));
    }

    private void drawNametags(NametagRenderEvent event, List<CustomNametag> nametags) {
        // calculate color of nametag box
        int backgroundColor = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255f) << 24;

        // add vanilla nametag to list
        nametags.add(new CustomNametag(event.getDisplayName(), 1f));

        float yOffset = 0f;
        for (CustomNametag nametag : nametags) {
            // move rendering up to fit the next line, plus a small gap
            yOffset += nametag.nametagScale() * NAMETAG_HEIGHT;

            RenderUtils.renderCustomNametag(
                    event.getPoseStack(),
                    event.getBuffer(),
                    event.getPackedLight(),
                    backgroundColor,
                    event.getEntityRenderDispatcher(),
                    event.getEntity(),
                    nametag.nametagComponent(),
                    event.getFont(),
                    nametag.nametagScale(),
                    yOffset);
        }
    }

    private record CustomNametag(Component nametagComponent, float nametagScale) {}
}
