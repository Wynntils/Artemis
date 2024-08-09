/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.mixin;

import com.wynntils.utils.SystemUtils;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Credits to
// https://github.com/comp500/ScreenshotToClipboard/blob/1.18-arch/common/src/main/java/link/infra/screenshotclipboard/common/mixin/AWTHackMixin.java
@Mixin(Main.class)
public abstract class MainMixin {
    // Inject as early as possible (but after Main statics execute), and disable java.awt.headless on non-macOS systems
    @Inject(method = "main([Ljava/lang/String;)V", at = @At("HEAD"), remap = false)
    private static void awtHack(CallbackInfo ci) {
        // A bit dangerous, but shouldn't technically cause any issues on most platforms - headless mode just disables
        // the awt API
        // Minecraft usually has this enabled because it's using GLFW rather than AWT/Swing
        // Also causes problems on macOS, see:
        // https://github.com/MinecraftForge/MinecraftForge/pull/5591#issuecomment-470805491

        // This uses a Mixin because this must be done as early as possible - before other mods load that use AWT
        // see https://github.com/BuiltBrokenModding/SBM-SheepMetal/issues/2
        if (!SystemUtils.isMac()) {
            // Do NOT use logger here. If we reference the WynntilsMod class, we will crash.
            System.out.println("[Wynntils] Setting java.awt.headless to false");
            System.setProperty("java.awt.headless", "false");

            // Test that the mixin was run properly
            // Ensure AWT is loaded by forcing loadLibraries() to be called, will cause a HeadlessException if someone
            // else already loaded AWT
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard();
            } catch (HeadlessException e) {
                // Do NOT use logger here. If we reference the WynntilsMod class, we will crash.
                System.out.println("[Wynntils] java.awt.headless property was not set properly!");
            }
        }
    }
}
