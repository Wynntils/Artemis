/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.resourcepack;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.mc.McUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

/**
 * A {@link RepositorySource} that provides the Wynncraft resource pack.
 * <p> The provided pack is the same as the one downloaded by Minecraft when connecting to the Wynncraft server.
 * This pack is provided by Wynntils to avoid downloading it every time the player connects to the server.
 * <p> The pack is loaded from the {@code .minecraft/downloads} folder.
 * As we don't know which file is the latest WynnResourcePack, we check all files in the folder.
 * It's the responsibility of {@link ResourcePackService} to handle which pack is actually loaded, as well as
 * handling the case where the pack is not found, a different pack is loaded, or the pack is outdated.
 */
public final class WynntilsResourceProvider implements RepositorySource {
    private static final String PRELOADED_PACK_PREFIX = "wynntils_preloaded/";
    // §7WynnResourcePack - 2.0.4\n§a(1.21, Jun. 11, 2024)
    private static final Pattern WYNNCRAFT_PACK_PATTERN =
            Pattern.compile("§(#.{8}|.)Wynncraft Resource Pack §7.+", Pattern.DOTALL);

    private static final Path MINECRAFT_RESOURCE_PACKS_PATH =
            McUtils.mc().gameDirectory.toPath().resolve("downloads");
    private static final PackSelectionConfig WYNNTILS_PRELOADED_PACK_CONFIG =
            new PackSelectionConfig(false, Pack.Position.TOP, false);
    private static final PackSource WYNNTILS_PRELOADED_PACK_SOURCE =
            PackSource.create(component -> component.copy().append(Component.literal(" (Wynntils)")), true);

    @Override
    public void loadPacks(Consumer<Pack> onLoad) {
        try {
            // Check all subdirectories of the folder, but don't go deeper
            try (Stream<Path> paths = Files.walk(MINECRAFT_RESOURCE_PACKS_PATH, 2)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    String fileName = path.getFileName().toString();

                    // Check if the file is a valid resource pack,
                    // we do this by checking if the file name is an SHA-1 hash
                    if (DownloadedPackSource.tryParseSha1Hash(fileName) == null) return;

                    PackLocationInfo packLocationInfo = new PackLocationInfo(
                            PRELOADED_PACK_PREFIX + fileName,
                            Component.literal("Wynncraft Pack"),
                            WYNNTILS_PRELOADED_PACK_SOURCE,
                            Optional.empty());
                    FilePackResources.FileResourcesSupplier resourcesSupplier =
                            new FilePackResources.FileResourcesSupplier(path);

                    Pack pack = Pack.readMetaAndCreate(
                            packLocationInfo,
                            resourcesSupplier,
                            PackType.CLIENT_RESOURCES,
                            WYNNTILS_PRELOADED_PACK_CONFIG);

                    if (pack == null) {
                        WynntilsMod.warn("Failed to load resource pack: " + fileName);
                        return;
                    }

                    // Load the pack description and check if it matches the Wynncraft pack pattern
                    StyledText packDescription = StyledText.fromComponent(pack.getDescription());
                    if (packDescription.matches(WYNNCRAFT_PACK_PATTERN)) {
                        onLoad.accept(pack);
                    }
                });
            }
        } catch (IOException e) {
            WynntilsMod.error("Failed to load resource pack", e);
        }
    }
}
