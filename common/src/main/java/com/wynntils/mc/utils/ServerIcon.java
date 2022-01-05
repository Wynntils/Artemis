/*
 * Copyright © Wynntils 2018-2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.utils;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.wynntils.utils.Utils;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;

/** Provides the icon for a server in the form of a {@link ResourceLocation} with utility methods */
public class ServerIcon {
    public static final ResourceLocation FALLBACK;

    private final ServerStatusPinger pinger = new ServerStatusPinger();

    private final ServerData server;
    private ResourceLocation serverIconLocation;
    private final Consumer<ServerIcon> onDone;

    static {
        FALLBACK = new ResourceLocation("textures/misc/unknown_server.png");
    }
    /**
     * @param server {@link ServerData} of server
     * @param allowStale flag whether if an already existing icon should be used
     * @param onDone consumer when done, can be null if none
     */
    public ServerIcon(ServerData server, boolean allowStale, Consumer<ServerIcon> onDone) {
        this.server = server;
        this.onDone = onDone;
        this.serverIconLocation = FALLBACK;

        // Try default
        ResourceLocation destination =
                new ResourceLocation(
                        "servers/" + Hashing.sha1().hashUnencodedChars(server.ip) + "/icon");

        // If someone converts this to get the actual ServerData used by the gui, check
        // ServerData#pinged here and
        // set it later
        if (allowStale && getServerIcon() != null) {
            System.out.println("Accepted stale server icon");
            onDone();
            return;
        }

        System.out.println("Trying to ping server");
        try {
            pinger.pingServer(
                    server,
                    () -> {
                        System.out.println("Pinged server");
                        loadServerIcon(destination);
                        onDone();
                    });
        } catch (Exception e) {
            System.out.println("Failed to ping server");
            onDone();
        }
        System.out.println("Constructor done");
    }

    public ServerIcon(ServerData server, boolean allowStale) {
        this(server, allowStale, null);
    }

    /**
     * Binds the icon to the {@link net.minecraft.client.renderer.texture.TextureManager} if found,
     * else unknown server texture
     */
    public synchronized void bind() {
        Minecraft.getInstance().getTextureManager().bind(serverIconLocation);
    }

    /** Returns the {@link DynamicTexture} form of the icon */
    public synchronized DynamicTexture getServerIcon() {
        return (DynamicTexture)
                Minecraft.getInstance().getTextureManager().getTexture(serverIconLocation);
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
    public synchronized ResourceLocation getServerIconLocation() {
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
            Utils.logUnknown("Unable to load icon", null);
            serverIconLocation = FALLBACK;
            return;
        }

        NativeImage nativeImage;
        try {
            nativeImage = NativeImage.fromBase64(iconString);
        } catch (IOException e) {
            e.printStackTrace();
            Utils.logUnknown("Unable to convert image from base64", iconString);
            serverIconLocation = FALLBACK;
            return;
        }

        Validate.validState(nativeImage.getWidth() == 64, "Must be 64 pixels wide");
        Validate.validState(nativeImage.getHeight() == 64, "Must be 64 pixels high");

        System.out.println("Finished loading icon");
        Minecraft.getInstance()
                .getTextureManager()
                .register((serverIconLocation = destination), new DynamicTexture(nativeImage));
    }
}
