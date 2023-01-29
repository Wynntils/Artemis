/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import com.wynntils.core.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.SystemUtils;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.wynn.WynnItemMatchers;
import com.wynntils.utils.wynn.WynnUtils;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@FeatureInfo(stability = Stability.INVARIABLE)
public class ItemScreenshotFeature extends UserFeature {
    @RegisterKeyBind
    private final KeyBind itemScreenshotKeyBind =
            new KeyBind("Screenshot Item", GLFW.GLFW_KEY_F4, true, null, this::onInventoryPress);

    private Slot screenshotSlot = null;

    private void onInventoryPress(Slot hoveredSlot) {
        screenshotSlot = hoveredSlot;
    }

    // All other features (besides scaling) must be able to update the tooltip first
    @SubscribeEvent(priority = EventPriority.LOW)
    public void render(ItemTooltipRenderEvent.Pre e) {
        if (!Models.WorldState.onWorld()) return;
        if (screenshotSlot == null || !screenshotSlot.hasItem()) return;

        Screen screen = McUtils.mc().screen;
        if (!(screen instanceof AbstractContainerScreen<?>)) return;

        // has to be called during a render period
        takeScreenshot(screen, screenshotSlot, e.getTooltips());
        makeChatPrompt(screenshotSlot);
        screenshotSlot = null;
    }

    private static void takeScreenshot(Screen screen, Slot hoveredSlot, List<Component> itemTooltip) {
        ItemStack stack = hoveredSlot.getItem();
        List<Component> tooltip = new ArrayList<>(itemTooltip);
        removeLoreTooltipLines(tooltip);

        Font font = FontRenderer.getInstance().getFont();

        // width calculation
        int width = 0;
        for (Component c : tooltip) {
            int w = font.width(c.getString());
            if (w > width) {
                width = w;
            }
        }
        width += 8;

        // height calculation

        int height = 16;
        if (tooltip.size() > 1) {
            height += 2 + (tooltip.size() - 1) * 10;
        }

        // calculate tooltip size to fit to framebuffer
        float scaleh = (float) screen.height / height;
        float scalew = (float) screen.width / width;

        // draw tooltip to framebuffer, create image
        McUtils.mc().getMainRenderTarget().unbindWrite();

        PoseStack poseStack = new PoseStack();
        RenderTarget fb = new MainTarget(width * 2, height * 2);
        fb.setClearColor(1f, 1f, 1f, 0f);
        fb.createBuffers(width * 2, height * 2, false);
        fb.bindWrite(false);
        poseStack.pushPose();
        poseStack.scale(scalew, scaleh, 1);
        RenderUtils.drawTooltip(poseStack, tooltip, font, true);
        poseStack.popPose();
        fb.unbindWrite();
        McUtils.mc().getMainRenderTarget().bindWrite(true);

        BufferedImage bi = SystemUtils.createScreenshot(fb);

        // First try to save it to disk
        String itemNameForFile = WynnUtils.normalizeBadString(
                        ComponentUtils.stripFormatting(stack.getHoverName().getString()))
                .replaceAll("[/ ]", "_");
        File screenshotDir = new File(McUtils.mc().gameDirectory, "screenshots");
        String filename = Util.getFilenameFormattedDateTime() + "-" + itemNameForFile + ".png";
        try {
            File outputfile = new File(screenshotDir, filename);
            ImageIO.write(bi, "png", outputfile);

            McUtils.sendMessageToClient(Component.translatable(
                            "feature.wynntils.itemScreenshot.save.message",
                            stack.getHoverName(),
                            Component.literal(outputfile.getName())
                                    .withStyle(ChatFormatting.UNDERLINE)
                                    .withStyle(style -> style.withClickEvent(
                                            new ClickEvent(ClickEvent.Action.OPEN_FILE, outputfile.getAbsolutePath()))))
                    .withStyle(ChatFormatting.GREEN));
        } catch (IOException e) {
            WynntilsMod.error("Failed to save image to disk", e);
            McUtils.sendMessageToClient(
                    Component.translatable("feature.wynntils.itemScreenshot.save.error", stack.getHoverName(), filename)
                            .withStyle(ChatFormatting.RED));
        }

        // Then try to send a copy to the clipboard
        if (SystemUtils.isMac()) {
            McUtils.sendMessageToClient(Component.translatable("feature.wynntils.itemScreenshot.copy.mac")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        try {
            SystemUtils.copyImageToClipboard(bi);
            McUtils.sendMessageToClient(Component.translatable("feature.wynntils.itemScreenshot.copy.message")
                    .withStyle(ChatFormatting.GREEN));
        } catch (HeadlessException ex) {
            WynntilsMod.error("Failed to copy image to clipboard", ex);
            McUtils.sendMessageToClient(Component.translatable("feature.wynntils.itemScreenshot.copy.error")
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void makeChatPrompt(Slot hoveredSlot) {
        // chat item prompt
        Optional<GearItem> gearItemOpt = Models.Item.asWynnItem(hoveredSlot.getItem(), GearItem.class);
        if (gearItemOpt.isEmpty()) return;

        GearItem gearItem = gearItemOpt.get();
        if (gearItem.isUnidentified()) {
            // FIXME: better error to user
            WynntilsMod.warn("Cannot take screenshot of unidentified gear!");
            return;
        }
        String encoded = Models.Gear.toEncodedString(gearItem);

        McUtils.sendMessageToClient(Component.translatable("feature.wynntils.itemScreenshot.chatItemMessage")
                .withStyle(ChatFormatting.DARK_GREEN)
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, encoded)))
                .withStyle(s -> s.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("feature.wynntils.itemScreenshot.chatItemTooltip")
                                .withStyle(ChatFormatting.DARK_AQUA)))));
    }

    /**
     * Create a list of ItemIdentificationContainer corresponding to the given GearProfile, formatted for item guide items
     *
     * @param item the profile of the item
     * @return a list of appropriately formatted ItemIdentificationContainer
     */
    private static void removeLoreTooltipLines(List<Component> tooltip) {
        int loreStart = -1;
        for (int i = 0; i < tooltip.size(); i++) {
            // only remove text after the item type indicator
            if (WynnItemMatchers.rarityLineMatcher(tooltip.get(i)).find()) {
                loreStart = i + 1;
                break;
            }
        }

        // type indicator was found
        if (loreStart != -1) {
            tooltip.subList(loreStart, tooltip.size()).clear();
        }
    }
}
