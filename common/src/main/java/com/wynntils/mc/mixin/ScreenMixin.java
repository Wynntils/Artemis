/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.events.MixinHelper;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.mc.event.PauseMenuInitEvent;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.mc.event.ScreenRenderEvent;
import com.wynntils.mc.event.TitleScreenInitEvent;
import com.wynntils.mc.extension.ScreenExtension;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin implements ScreenExtension {
    @Unique
    private TextInputBoxWidget wynntilsFocusedTextInput;

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("HEAD"))
    private void initPre(Minecraft client, int width, int height, CallbackInfo info) {
        Screen screen = (Screen) (Object) this;
        if (!(screen instanceof TitleScreen titleScreen)) return;

        MixinHelper.postAlways(new TitleScreenInitEvent.Pre(titleScreen));
    }

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("RETURN"))
    private void initPost(Minecraft client, int width, int height, CallbackInfo info) {
        Screen screen = (Screen) (Object) this;

        if (screen instanceof TitleScreen titleScreen) {
            MixinHelper.postAlways(new TitleScreenInitEvent.Post(titleScreen));
        } else if (screen instanceof PauseScreen pauseMenuScreen) {
            MixinHelper.post(new PauseMenuInitEvent(pauseMenuScreen));
        }
    }

    @WrapOperation(
            method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/Screen;renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;Ljava/util/Optional;II)V"))
    private void renderTooltipPre(
            Screen instance,
            PoseStack poseStack,
            List<Component> tooltips,
            Optional<TooltipComponent> visualTooltipComponent,
            int mouseX,
            int mouseY,
            Operation<Void> original,
            @Local(argsOnly = true) ItemStack itemStack) {
        ItemTooltipRenderEvent.Pre event =
                new ItemTooltipRenderEvent.Pre(poseStack, itemStack, tooltips, mouseX, mouseY);
        MixinHelper.post(event);
        if (event.isCanceled()) return;

        original.call(
                instance,
                event.getPoseStack(),
                event.getTooltips(),
                event.getItemStack().getTooltipImage(),
                event.getMouseX(),
                event.getMouseY());
    }

    @Inject(
            method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("RETURN"))
    private void renderTooltipPost(PoseStack poseStack, ItemStack itemStack, int mouseX, int mouseY, CallbackInfo ci) {
        MixinHelper.post(new ItemTooltipRenderEvent.Post(poseStack, itemStack, mouseX, mouseY));
    }

    @Inject(
            method = "rebuildWidgets()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init()V"))
    private void onScreenInit(CallbackInfo ci) {
        MixinHelper.post(new ScreenInitEvent((Screen) (Object) this));
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", at = @At("RETURN"))
    private void onScreenRenderPost(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        MixinHelper.post(new ScreenRenderEvent((Screen) (Object) this, poseStack, mouseX, mouseY, partialTick));
    }

    @Override
    @Unique
    public TextInputBoxWidget getFocusedTextInput() {
        return wynntilsFocusedTextInput;
    }

    @Override
    @Unique
    public void setFocusedTextInput(TextInputBoxWidget focusedTextInput) {
        this.wynntilsFocusedTextInput = focusedTextInput;
    }
}
