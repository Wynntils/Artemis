/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import com.wynntils.core.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyHolder;
import com.wynntils.mc.event.InventoryKeyPressEvent;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.mc.render.FontRenderer;
import com.wynntils.mc.render.RenderUtils;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wc.custom.item.GearItemStack;
import com.wynntils.wc.utils.ChatItemUtils;
import com.wynntils.wc.utils.WynnItemUtils;
import com.wynntils.wc.utils.WynnUtils;
import java.awt.image.BufferedImage;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@FeatureInfo(stability = Stability.INVARIABLE)
public class ItemScreenshotFeature extends UserFeature {

    @RegisterKeyBind
    private final KeyHolder itemScreenshotKeybind =
            new KeyHolder("Screenshot Item", GLFW.GLFW_KEY_F4, "Wynntils", true, () -> {});

    private Slot screenshotSlot = null;

    @SubscribeEvent
    public void onKeyPress(InventoryKeyPressEvent e) {
        if (itemScreenshotKeybind.getKeybind().matches(e.getKeyCode(), e.getScanCode())) {
            screenshotSlot = e.getHoveredSlot();
        }
    }

    @SubscribeEvent
    public void render(ItemTooltipRenderEvent.Pre e) {
        if (screenshotSlot == null) return;

        // has to be called during a render period
        takeScreenshot(screenshotSlot);
        screenshotSlot = null;
    }

    private static void takeScreenshot(Slot hoveredSlot) {
        if (!WynnUtils.onWorld()) return;

        Screen screen = McUtils.mc().screen;
        if (!(screen instanceof AbstractContainerScreen<?>)) return;
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return;

        ItemStack stack = hoveredSlot.getItem();
        List<Component> tooltip = stack.getTooltipLines(null, TooltipFlag.Default.NORMAL);
        WynnItemUtils.removeLoreTooltipLines(tooltip);

        Font font = FontRenderer.getInstance().getFont();
        int width = 0;
        int height = 16;

        // width calculation
        for (Component c : tooltip) {
            int w = font.width(c.getString());
            if (w > width) {
                width = w;
            }
        }
        width += 8;

        // height calculation
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

        BufferedImage bi = RenderUtils.createScreenshot(fb);
        RenderUtils.copyImageToClipboard(bi);

        McUtils.sendMessageToClient(
                new TranslatableComponent("feature.wynntils.itemScreenshot.message", stack.getHoverName())
                        .withStyle(ChatFormatting.GREEN));

        // chat item prompt
        if (stack instanceof GearItemStack gearItem) {
            String encoded = ChatItemUtils.encodeItem(gearItem);

            McUtils.sendMessageToClient(new TranslatableComponent("feature.wynntils.itemScreenshot.chatItemMessage")
                    .withStyle(ChatFormatting.DARK_GREEN)
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, encoded)))
                    .withStyle(s -> s.withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TranslatableComponent("feature.wynntils.itemScreenshot.chatItemTooltip")
                                    .withStyle(ChatFormatting.DARK_AQUA)))));
        }
    }
}
