/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.google.common.collect.ImmutableList;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.chat.RecipientType;
import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.StartDisabled;
import com.wynntils.core.managers.Model;
import com.wynntils.core.services.TranslationModel;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wynn.event.ChatMessageReceivedEvent;
import com.wynntils.wynn.event.NpcDialogEvent;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@StartDisabled
public class TranslationFeature extends UserFeature {
    public static TranslationFeature INSTANCE;

    @Config
    public String languageName = "";

    @Config
    public boolean translateTrackedQuest = true;

    @Config
    public boolean translateNpc = true;

    @Config
    public boolean translateInfo = true;

    @Config
    public boolean translatePlayerChat = false;

    @Config
    public boolean keepOriginal = true;

    @Config
    public TranslationModel.TranslationServices translationService = TranslationModel.TranslationServices.GOOGLEAPI;

    @Override
    protected void onInit(
            ImmutableList.Builder<Condition> conditions, ImmutableList.Builder<Class<? extends Model>> dependencies) {
        dependencies.add(TranslationModel.class);
    }

    @SubscribeEvent
    public void onChat(ChatMessageReceivedEvent e) {
        if (e.getRecipientType() != RecipientType.INFO && !translatePlayerChat) return;
        if (e.getRecipientType() == RecipientType.INFO && !translateInfo) return;

        String origCoded = e.getCodedMessage();
        String wrapped = wrapCoding(origCoded);
        TranslationModel.getTranslator().translate(wrapped, languageName, translatedMsg -> {
            String messageToSend;
            if (translatedMsg != null) {
                messageToSend = unwrapCoding(translatedMsg);
            } else {
                if (keepOriginal) return;

                // We failed to get a translation; send the original message so it's not lost
                messageToSend = origCoded;
            }
            McUtils.mc().doRunTask(() -> {
                McUtils.sendMessageToClient(new TextComponent(messageToSend));
            });
        });
        if (!keepOriginal) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onNpcDialgue(NpcDialogEvent e) {
        if (!translateNpc) return;
        if (e instanceof TranslatedNpcDialogEvent) return;

        String origCoded = e.getCodedDialog();
        if (origCoded != null) {
            String wrapped = wrapCoding(origCoded);
            TranslationModel.getTranslator().translate(wrapped, languageName, translatedMsg -> {
                String unwrapped = unwrapCoding(translatedMsg);
                McUtils.mc().doRunTask(() -> {
                    NpcDialogEvent translatedEvent = new TranslatedNpcDialogEvent(unwrapped, e.isNeedsConfirmation());
                    WynntilsMod.postEvent(translatedEvent);
                });
            });
        } else {
            // We must also pass on the null event to clear the dialogue
            NpcDialogEvent translatedEvent = new TranslatedNpcDialogEvent(null, e.isNeedsConfirmation());
            WynntilsMod.postEvent(translatedEvent);
        }
        if (!keepOriginal) {
            e.setCanceled(true);
        }
    }

    private String unwrapCoding(String origCoded) {
        return origCoded.replaceAll("\\{ ?§ ?([0-9a-fklmnor]) ?\\}", "§$1");
    }

    private String wrapCoding(String origCoded) {
        return origCoded.replaceAll("(§[0-9a-fklmnor])", "{$1}");
    }

    private static class TranslatedNpcDialogEvent extends NpcDialogEvent {
        public TranslatedNpcDialogEvent(String codedDialog, boolean autoProgressing) {
            super(codedDialog, autoProgressing);
        }
    }
}
