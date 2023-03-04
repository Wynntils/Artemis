/*
 * Copyright © Wynntils 2021-2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.ui;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.UserFeature;
import com.wynntils.mc.event.TitleScreenInitEvent;
import com.wynntils.utils.render.Texture;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.Validate;

@ConfigCategory(Category.UI)
public class WynncraftButtonFeature extends UserFeature {
    private static final String GAME_SERVER = "play.wynncraft.com";
    private static final String LOBBY_SERVER = "lobby.wynncraft.com";

    @Config
    public boolean connectToLobby = false;

    @SubscribeEvent
    public void onTitleScreenInit(TitleScreenInitEvent.Post e) {
        ServerData wynncraftServer = new ServerData("Wynncraft", connectToLobby ? LOBBY_SERVER : GAME_SERVER, false);
        wynncraftServer.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);

        WynncraftButton wynncraftButton = new WynncraftButton(
                e.getTitleScreen(),
                wynncraftServer,
                e.getTitleScreen().width / 2 + 104,
                e.getTitleScreen().height / 4 + 48 + 24);
        e.getAddButton().accept(wynncraftButton);
    }

    private static class WynncraftButton extends Button {
        private final Screen backScreen;
        private final ServerData serverData;
        private final ServerIcon serverIcon;

        // TODO tooltip
        WynncraftButton(Screen backScreen, ServerData serverData, int x, int y) {
            super(x, y, 20, 20, Component.translatable(""), WynncraftButton::onPress, Button.DEFAULT_NARRATION);
            this.serverData = serverData;
            this.backScreen = backScreen;

            this.serverIcon = new ServerIcon(serverData);
            this.serverIcon.loadResource(false);
        }

        @Override
        public void renderButton(PoseStack matrices, int mouseX, int mouseY, float partialTicks) {
            super.renderButton(matrices, mouseX, mouseY, partialTicks);

            if (serverIcon == null || serverIcon.getServerIconLocation() == null) {
                return;
            }

            RenderSystem.setShaderTexture(0, serverIcon.getServerIconLocation());

            // Insets the icon by 3
            blit(matrices, this.getX() + 3, this.getY() + 3, this.width - 6, this.height - 6, 0, 0, 64, 64, 64, 64);
        }

        protected static void onPress(Button button) {
            if (!(button instanceof WynncraftButton wynncraftButton)) return;

            ConnectScreen.startConnecting(
                    wynncraftButton.backScreen,
                    Minecraft.getInstance(),
                    ServerAddress.parseString(wynncraftButton.serverData.ip),
                    wynncraftButton.serverData);
        }
    }

    /** Provides the icon for a server in the form of a {@link ResourceLocation} with utility methods */
    private static final class ServerIcon {
        private static final ResourceLocation FALLBACK;

        private final ServerData server;
        private ResourceLocation serverIconLocation;
        private final Consumer<ServerIcon> onDone;

        static {
            FALLBACK = Texture.WYNNCRAFT_ICON.resource();
        }

        /**
         * @param server {@link ServerData} of server
         * @param onDone consumer when done, can be null if none
         */
        private ServerIcon(ServerData server, Consumer<ServerIcon> onDone) {
            this.server = server;
            this.onDone = onDone;
            this.serverIconLocation = FALLBACK;
        }

        private void loadResource(boolean allowStale) {
            // Try default
            ResourceLocation destination =
                    new ResourceLocation("servers/" + Hashing.sha1().hashUnencodedChars(server.ip) + "/icon");

            // If someone converts this to get the actual ServerData used by the gui, check
            // ServerData#pinged here and
            // set it later
            if (allowStale && Minecraft.getInstance().getTextureManager().getTexture(destination, null) != null) {
                serverIconLocation = destination;
                onDone();
                return;
            }

            try {
                ServerStatusPinger pinger = new ServerStatusPinger();
                // FIXME: DynamicTexture issues in loadServerIcon
                //        loadServerIcon(destination);
                pinger.pingServer(server, this::onDone);
            } catch (Exception e) {
                WynntilsMod.warn("Failed to ping server", e);
                onDone();
            }
        }

        private ServerIcon(ServerData server) {
            this(server, null);
        }

        /** Returns whether getting the icon has succeeded. */
        public boolean isSuccess() {
            return !FALLBACK.equals(serverIconLocation);
        }

        /** Returns the {@link ServerData} used to get the icon */
        public ServerData getServer() {
            return server;
        }

        /** Returns the icon as a {@link ResourceLocation} if found, else unknown server texture */
        private synchronized ResourceLocation getServerIconLocation() {
            return serverIconLocation;
        }

        private void onDone() {
            if (onDone != null) onDone.accept(this);
        }

        // Modified from
        // net.minecraft.client.gui.screens.multiplayer.ServerSelectionList#uploadServerIcon
        private synchronized void loadServerIcon(ResourceLocation destination) {
            String iconString = server.getIconB64();
            // failed to ping server or icon wasn't sent
            if (iconString == null) {
                WynntilsMod.warn("Unable to load icon");
                serverIconLocation = FALLBACK;
                return;
            }

            try (NativeImage nativeImage = NativeImage.fromBase64(iconString)) {
                Validate.validState(nativeImage.getWidth() == 64, "Must be 64 pixels wide");
                Validate.validState(nativeImage.getHeight() == 64, "Must be 64 pixels high");

                synchronized (this) {
                    RenderSystem.recordRenderCall(() -> {
                        Minecraft.getInstance()
                                .getTextureManager()
                                .register(destination, new DynamicTexture(nativeImage));
                        serverIconLocation = destination;
                    });
                }
            } catch (IOException e) {
                WynntilsMod.error("Unable to convert image from base64: " + iconString, e);
                serverIconLocation = FALLBACK;
            }
        }
    }
}
