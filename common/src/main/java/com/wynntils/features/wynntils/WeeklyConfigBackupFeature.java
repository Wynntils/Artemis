/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.wynntils;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.mc.event.TitleScreenInitEvent;
import com.wynntils.utils.FileUtils;
import java.io.File;
import java.util.concurrent.TimeUnit;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.WYNNTILS)
public class WeeklyConfigBackupFeature extends Feature {
    private static final File BACKUPS_DIR = WynntilsMod.getModStorageDir("backups");
    private static final int BACKUP_INTERVAL_DAYS = 7;

    @Persisted
    private final Storage<Long> lastBackup = new Storage<>(0L);

    @SubscribeEvent
    public void onTitleScreenInit(TitleScreenInitEvent.Post event) {
        long currentTime = System.currentTimeMillis();
        long lastBackupTime = lastBackup.get();

        if (currentTime - lastBackupTime >= TimeUnit.DAYS.toMillis(BACKUP_INTERVAL_DAYS)) {
            FileUtils.mkdir(BACKUPS_DIR);

            File userConfigFile = Managers.Config.getUserConfigFile();

            if (userConfigFile == null) {
                WynntilsMod.warn("Failed to create backup of user config file: user config file is null");
                return;
            }

            File configBackupFile =
                    new File(BACKUPS_DIR, userConfigFile.getName() + "-backup-" + currentTime + ".json");

            try {
                FileUtils.copyFile(userConfigFile, configBackupFile);
            } catch (Exception e) {
                WynntilsMod.warn("Failed to create backup of user config file: " + e.getMessage());
                return;
            }

            WynntilsMod.info("Created backup of user config file: " + configBackupFile.getName());

            File userStorageFile = Managers.Storage.getUserStorageFile();

            if (userStorageFile == null) {
                WynntilsMod.warn("Failed to create backup of user storage file: user storage file is null");
                return;
            }

            File storageBackupFile =
                    new File(BACKUPS_DIR, userStorageFile.getName() + "-backup-" + currentTime + ".json");

            try {
                FileUtils.copyFile(userStorageFile, storageBackupFile);
            } catch (Exception e) {
                WynntilsMod.warn("Failed to create backup of user storage file: " + e.getMessage());
                return;
            }

            WynntilsMod.info("Created backup of user storage file: " + storageBackupFile.getName());

            // Save the time of the backup after we've finished saving the backup
            lastBackup.store(currentTime);
        }
    }
}
