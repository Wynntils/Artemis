/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.framework.Subscriber;
import com.wynntils.framework.feature.Feature;
import com.wynntils.framework.feature.GameplayImpact;
import com.wynntils.framework.feature.PerformanceImpact;
import com.wynntils.mc.event.TitleScreenInitEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

public class WynncraftButtonFeature extends Feature {

    @Subscriber
    public static void onTitleScreenInit(TitleScreenInitEvent e) {
        ServerData wynncraftServer = new ServerData("Wynncraft", "play.wynncraft.com", false);
        WynncraftButton wynncraftButton =
                new WynncraftButton(
                        e.getTitleScreen(),
                        wynncraftServer,
                        e.getTitleScreen().width / 2 + 104,
                        e.getTitleScreen().height / 4 + 48 + 24);
        e.getAddButton().accept(wynncraftButton);
    }

    @Override
    public PerformanceImpact getPerformanceImpact() {
        return PerformanceImpact.Small;
    }

    @Override
    public GameplayImpact getGameplayImpactImpact() {
        return GameplayImpact.Medium;
    }

    @Override
    public int getCreationPriority() {
        return 0;
    }

    private static class WynncraftButton extends Button {
        public static final ResourceLocation WYNNCRAFT_SERVER_ICON =
                new ResourceLocation("textures/misc/unknown_server.png");
        private final Screen backScreen;
        private final ServerData serverData;

        WynncraftButton(Screen backScreen, ServerData serverData, int x, int y) {
            super(x, y, 20, 20, new TranslatableComponent(""), WynncraftButton::onPress);
            this.backScreen = backScreen;
            this.serverData = serverData;
        }

        @Override
        public void renderButton(PoseStack matrices, int mouseX, int mouseY, float partialTicks) {
            super.renderButton(matrices, mouseX, mouseY, partialTicks);

            Minecraft.getInstance().getTextureManager().bind(WYNNCRAFT_SERVER_ICON);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

            this.blit(matrices, this.x, this.y, 0, 0, this.width, this.height);
        }

        public static void onPress(Button button) {
            if (button instanceof WynncraftButton wynncraftButton) {
                Minecraft.getInstance()
                        .setScreen(
                                new ConnectScreen(
                                        wynncraftButton.backScreen,
                                        Minecraft.getInstance(),
                                        wynncraftButton.serverData));
            }
        }
    }
}
