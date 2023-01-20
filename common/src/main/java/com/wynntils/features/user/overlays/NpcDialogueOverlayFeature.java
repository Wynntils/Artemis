/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.features.overlays.OverlayPosition;
import com.wynntils.core.features.overlays.annotations.OverlayInfo;
import com.wynntils.core.features.overlays.sizes.GuiScaledOverlaySize;
import com.wynntils.core.features.properties.FeatureCategory;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.gui.render.FontRenderer;
import com.wynntils.gui.render.HorizontalAlignment;
import com.wynntils.gui.render.TextRenderSetting;
import com.wynntils.gui.render.TextRenderTask;
import com.wynntils.gui.render.TextShadow;
import com.wynntils.gui.render.VerticalAlignment;
import com.wynntils.gui.render.buffered.BufferedFontRenderer;
import com.wynntils.gui.render.buffered.BufferedRenderUtils;
import com.wynntils.handlers.chat.NpcDialogueType;
import com.wynntils.handlers.chat.event.NpcDialogEvent;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.CommonColors;
import com.wynntils.utils.MathUtils;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@FeatureInfo(category = FeatureCategory.OVERLAYS)
public class NpcDialogueOverlayFeature extends UserFeature {
    private static final Pattern NEW_QUEST_STARTED = Pattern.compile("^§r§6§lNew Quest Started: §r§e§l(.*)§r$");

    private final ScheduledExecutorService autoProgressExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledAutoProgressKeyPress = null;

    private final List<ConfirmationlessDialogue> confirmationlessDialogues = new ArrayList<>();
    private List<String> currentDialogue;
    private NpcDialogueType dialogueType;
    private boolean isProtected;

    @Config
    public boolean autoProgress = false;

    @Config
    public int dialogAutoProgressDefaultTime = 1600; // Milliseconds

    @Config
    public int dialogAutoProgressAdditionalTimePerWord = 300; // Milliseconds

    @RegisterKeyBind
    public final KeyBind cancelAutoProgressKeybind =
            new KeyBind("Cancel Dialog Auto Progress", GLFW.GLFW_KEY_Y, false, this::cancelAutoProgress);

    private void cancelAutoProgress() {
        if (scheduledAutoProgressKeyPress == null) return;

        scheduledAutoProgressKeyPress.cancel(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onNpcDialogue(NpcDialogEvent e) {
        List<String> msg =
                e.getChatMessage().stream().map(ComponentUtils::getCoded).toList();

        // Print dialogue to the system log
        msg.forEach(s -> WynntilsMod.info("[NPC] " + s));

        if (e.getType() == NpcDialogueType.CONFIRMATIONLESS) {
            // The same message can be repeating before we have finished removing the old
            // Just remove the old and add the new with an updated remove time
            confirmationlessDialogues.removeIf(d -> d.text.equals(msg));
            ConfirmationlessDialogue dialogue =
                    new ConfirmationlessDialogue(msg, System.currentTimeMillis() + calculateMessageReadTime(msg));
            confirmationlessDialogues.add(dialogue);
            return;
        }

        currentDialogue = msg;
        dialogueType = e.getType();
        isProtected = e.isProtected();

        if (!msg.isEmpty() && NEW_QUEST_STARTED.matcher(msg.get(0)).find()) {
            // TODO: Show nice banner notification instead
            // but then we'd also need to confirm it with a sneak
            Managers.Notification.queueMessage(msg.get(0));
        }

        if (e.getType() == NpcDialogueType.SELECTION && e.isProtected()) {
            // This is a bit of a workaround to be able to select the options
            MutableComponent clickMsg = Component.literal("Open chat and click on the option to select it.")
                    .withStyle(ChatFormatting.AQUA);
            e.getChatMessage()
                    .forEach(line -> clickMsg.append(Component.literal("\n").append(line)));
            McUtils.sendMessageToClient(clickMsg);
        }

        if (scheduledAutoProgressKeyPress != null) {
            scheduledAutoProgressKeyPress.cancel(true);

            // Release sneak key if currently pressed
            McUtils.sendPacket(new ServerboundPlayerCommandPacket(
                    McUtils.player(), ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));

            scheduledAutoProgressKeyPress = null;
        }

        if (autoProgress && dialogueType == NpcDialogueType.NORMAL) {
            // Schedule a new sneak key press if this is not the end of the dialogue
            if (!msg.isEmpty()) {
                scheduledAutoProgressKeyPress = scheduledSneakPress(msg);
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        long now = System.currentTimeMillis();
        confirmationlessDialogues.removeIf(dialogue -> now >= dialogue.removeTime);
    }

    private ScheduledFuture<?> scheduledSneakPress(List<String> msg) {
        long delay = calculateMessageReadTime(msg);

        return autoProgressExecutor.schedule(
                () -> McUtils.sendPacket(new ServerboundPlayerCommandPacket(
                        McUtils.player(), ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY)),
                delay,
                TimeUnit.MILLISECONDS);
    }

    private long calculateMessageReadTime(List<String> msg) {
        int words = String.join(" ", msg).split(" ").length;
        long delay = dialogAutoProgressDefaultTime + ((long) words * dialogAutoProgressAdditionalTimePerWord);
        return delay;
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent e) {
        currentDialogue = List.of();
        confirmationlessDialogues.clear();
        cancelAutoProgress();
    }

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final Overlay npcDialogueOverlay = new NpcDialogueOverlay();

    public class NpcDialogueOverlay extends Overlay {
        @Config
        public TextShadow textShadow = TextShadow.NORMAL;

        @Config
        public float backgroundOpacity = 0.2f;

        @Config
        public boolean stripColors = false;

        @Config
        public boolean showHelperTexts = true;

        private TextRenderSetting renderSetting;

        protected NpcDialogueOverlay() {
            super(
                    new OverlayPosition(
                            0,
                            0,
                            VerticalAlignment.Top,
                            HorizontalAlignment.Center,
                            OverlayPosition.AnchorSection.BottomMiddle),
                    new GuiScaledOverlaySize(400, 50),
                    HorizontalAlignment.Center,
                    VerticalAlignment.Middle);
            updateTextRenderSettings();
        }

        private void updateTextRenderSettings() {
            renderSetting = TextRenderSetting.DEFAULT
                    .withMaxWidth(this.getWidth() - 5)
                    .withHorizontalAlignment(this.getRenderHorizontalAlignment())
                    .withTextShadow(textShadow);
        }

        @Override
        protected void onConfigUpdate(ConfigHolder configHolder) {
            updateDialogExtractionSettings();
            updateTextRenderSettings();
        }

        private void updateDialogExtractionSettings() {
            if (isEnabled()) {
                Handlers.Chat.addNpcDialogExtractionDependent(NpcDialogueOverlayFeature.this);
            } else {
                Handlers.Chat.removeNpcDialogExtractionDependent(NpcDialogueOverlayFeature.this);
                currentDialogue = List.of();
                confirmationlessDialogues.clear();
            }
        }

        private void renderDialogue(
                PoseStack poseStack,
                MultiBufferSource.BufferSource bufferSource,
                List<String> currentDialogue,
                NpcDialogueType dialogueType) {
            List<TextRenderTask> dialogueRenderTasks = currentDialogue.stream()
                    .map(s -> new TextRenderTask(s, renderSetting))
                    .toList();

            if (stripColors) {
                dialogueRenderTasks.forEach(dialogueRenderTask ->
                        dialogueRenderTask.setText(ComponentUtils.stripColorFormatting(dialogueRenderTask.getText())));
            }

            float textHeight = (float) dialogueRenderTasks.stream()
                    .map(dialogueRenderTask -> FontRenderer.getInstance()
                            .calculateRenderHeight(
                                    dialogueRenderTask.getText(),
                                    dialogueRenderTask.getSetting().maxWidth()))
                    .mapToDouble(f -> f)
                    .sum();

            // Draw a translucent background
            float rectHeight = textHeight + 10;
            float rectRenderY =
                    switch (this.getRenderVerticalAlignment()) {
                        case Top -> this.getRenderY();
                        case Middle -> this.getRenderY() + (this.getHeight() - rectHeight) / 2f;
                        case Bottom -> this.getRenderY() + this.getHeight() - rectHeight;
                    };
            int colorAlphaRect = Math.round(MathUtils.clamp(255 * backgroundOpacity, 0, 255));
            BufferedRenderUtils.drawRect(
                    poseStack,
                    bufferSource,
                    CommonColors.BLACK.withAlpha(colorAlphaRect),
                    this.getRenderX(),
                    rectRenderY,
                    0,
                    this.getWidth(),
                    rectHeight);

            // Render the message
            BufferedFontRenderer.getInstance()
                    .renderTextsWithAlignment(
                            poseStack,
                            bufferSource,
                            this.getRenderX(),
                            this.getRenderY(),
                            dialogueRenderTasks,
                            this.getWidth(),
                            this.getHeight(),
                            this.getRenderHorizontalAlignment(),
                            this.getRenderVerticalAlignment());

            if (showHelperTexts) {
                // Render "To continue" message
                List<TextRenderTask> renderTaskList = new LinkedList<>();
                String protection = isProtected ? "§f<protected> §r" : "";
                if (dialogueType == NpcDialogueType.NORMAL) {
                    TextRenderTask pressSneakMessage =
                            new TextRenderTask(protection + "§cPress SNEAK to continue", renderSetting);
                    renderTaskList.add(pressSneakMessage);
                } else if (dialogueType == NpcDialogueType.SELECTION) {
                    TextRenderTask pressSneakMessage =
                            new TextRenderTask(protection + "§cSelect an option to continue", renderSetting);
                    renderTaskList.add(pressSneakMessage);
                }

                if (scheduledAutoProgressKeyPress != null && !scheduledAutoProgressKeyPress.isCancelled()) {
                    long timeUntilProgress = scheduledAutoProgressKeyPress.getDelay(TimeUnit.MILLISECONDS);
                    TextRenderTask autoProgressMessage = new TextRenderTask(
                            ChatFormatting.GREEN + "Auto-progress: "
                                    + Math.max(0, Math.round(timeUntilProgress / 1000f))
                                    + " seconds (Press "
                                    + ComponentUtils.getUnformatted(cancelAutoProgressKeybind
                                            .getKeyMapping()
                                            .getTranslatedKeyMessage())
                                    + " to cancel)",
                            renderSetting);
                    renderTaskList.add(autoProgressMessage);
                }

                BufferedFontRenderer.getInstance()
                        .renderTextsWithAlignment(
                                poseStack,
                                bufferSource,
                                this.getRenderX() + 5,
                                this.getRenderY() + 20 + textHeight,
                                renderTaskList,
                                this.getWidth() - 30,
                                this.getHeight() - 30,
                                this.getRenderHorizontalAlignment(),
                                this.getRenderVerticalAlignment());
            }
        }

        @Override
        public void render(
                PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float partialTicks, Window window) {
            if (currentDialogue.isEmpty() && confirmationlessDialogues.isEmpty()) return;

            LinkedList<String> allDialogues = new LinkedList<>(currentDialogue);
            confirmationlessDialogues.forEach(d -> {
                allDialogues.add("");
                allDialogues.addAll(d.text());
            });

            if (currentDialogue.isEmpty()) {
                // Remove the initial blank line in that case
                allDialogues.removeFirst();
            }
            renderDialogue(poseStack, bufferSource, allDialogues, dialogueType);
        }

        @Override
        public void renderPreview(
                PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float partialTicks, Window window) {
            List<String> fakeDialogue = List.of(
                    "§7[1/1] §r§2Random Citizen: §r§aDid you know that Wynntils is the best Wynncraft mod you'll probably find?§r");
            // we have to force update every time
            updateTextRenderSettings();

            renderDialogue(poseStack, bufferSource, fakeDialogue, NpcDialogueType.NORMAL);
        }
    }

    public record ConfirmationlessDialogue(List<String> text, long removeTime) {}
}
