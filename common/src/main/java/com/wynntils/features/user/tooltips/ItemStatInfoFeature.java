/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.tooltips;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureCategory;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.models.items.WynnItemCache;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ColorScaleUtils;
import com.wynntils.utils.wynn.GearTooltipBuilder;
import com.wynntils.utils.wynn.WynnItemUtils;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@FeatureInfo(stability = Stability.STABLE, category = FeatureCategory.TOOLTIPS)
public class ItemStatInfoFeature extends UserFeature {
    public static ItemStatInfoFeature INSTANCE;

    private final Set<GearItem> brokenItems = new HashSet<>();

    @Config
    public boolean showStars = true;

    @Config
    public boolean colorLerp = true;

    @Config
    public int decimalPlaces = 1;

    @Config
    public boolean perfect = true;

    @Config
    public boolean defective = true;

    @Config
    public float obfuscationChanceStart = 0.08f;

    @Config
    public float obfuscationChanceEnd = 0.04f;

    @Config
    public boolean reorderIdentifications = true;

    @Config
    public boolean groupIdentifications = true;

    @Config
    public boolean overallPercentageInName = true;

    @SubscribeEvent
    public void onTooltipPre(ItemTooltipRenderEvent.Pre event) {
        if (KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT)) return;

        Optional<GearItem> gearItemOpt = Models.Item.asWynnItem(event.getItemStack(), GearItem.class);
        if (gearItemOpt.isEmpty()) return;

        GearItem gearItem = gearItemOpt.get();
        if (brokenItems.contains(gearItem)) return;

        try {
            GearTooltipBuilder builder = gearItem.getCache()
                    .getOrCalculate(
                            WynnItemCache.TOOLTIP_KEY,
                            () -> GearTooltipBuilder.fromItemStack(
                                    event.getItemStack(), gearItem.getGearProfile(), gearItem));
            if (builder == null) return;

            LinkedList<Component> tooltips =
                    new LinkedList<>(builder.getTooltipLines(WynnItemUtils.getCurrentIdentificationStyle()));

            if (gearItem.hasVariableIds()) {
                if (perfect && gearItem.isPerfect()) {
                    tooltips.removeFirst();
                    tooltips.addFirst(getPerfectName(gearItem.getGearProfile().getDisplayName()));
                } else if (defective && gearItem.isDefective()) {
                    tooltips.removeFirst();
                    tooltips.addFirst(getDefectiveName(gearItem.getGearProfile().getDisplayName()));
                } else if (overallPercentageInName) {
                    MutableComponent name = Component.literal(
                                    tooltips.getFirst().getString())
                            .withStyle(tooltips.getFirst().getStyle());
                    name.append(getPercentageTextComponent(gearItem.getOverallPercentage()));
                    tooltips.removeFirst();
                    tooltips.addFirst(name);
                }
            }

            event.setTooltips(tooltips);
        } catch (Exception e) {
            brokenItems.add(gearItem);
            WynntilsMod.error(
                    "Exception when creating tooltips for item "
                            + gearItem.getGearProfile().getDisplayName(),
                    e);
            WynntilsMod.warn("This item has been disabled from ItemStatInfoFeature: " + gearItem);
            McUtils.sendMessageToClient(Component.literal("Wynntils error: Problem showing tooltip for item "
                            + gearItem.getGearProfile().getDisplayName())
                    .withStyle(ChatFormatting.RED));

            if (brokenItems.size() > 10) {
                // Give up and disable feature
                throw new RuntimeException(e);
            }
        }
    }

    private MutableComponent getPercentageTextComponent(float percentage) {
        return ColorScaleUtils.getPercentageTextComponent(percentage, colorLerp, decimalPlaces);
    }

    private MutableComponent getPerfectName(String itemName) {
        return ComponentUtils.makeRainbowStyle("Perfect " + itemName);
    }

    private MutableComponent getDefectiveName(String itemName) {
        return ComponentUtils.makeObfuscated("Defective " + itemName, obfuscationChanceStart, obfuscationChanceEnd);
    }
}
