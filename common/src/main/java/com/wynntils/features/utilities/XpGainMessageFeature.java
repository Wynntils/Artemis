/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.utilities;

import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.notifications.MessageContainer;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.characterstats.event.CombatXpGainEvent;
import com.wynntils.models.profession.event.ProfessionXpGainEvent;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.models.worlds.event.WorldStateEvent;
import java.util.EnumMap;
import java.util.Map;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.UTILITIES)
public class XpGainMessageFeature extends Feature {
    @Persisted
    public final Config<Boolean> combat = new Config<>(true);

    @Persisted
    public final Config<Boolean> professions = new Config<>(true);

    @Persisted
    public final Config<Float> secondDelay = new Config<>(10f);

    @Persisted
    public final Config<Boolean> filterChat = new Config<>(true);

    private long lastCombatXpDisplayTime = 0;
    private final Map<ProfessionType, Long> lastProfessionXpDisplayTimes = new EnumMap<>(ProfessionType.class);

    private MessageContainer lastCombatMessage = null;
    private float lastRawCombatXpGain = 0;
    private float currentCombatXpPercentage = 0;

    private final Map<ProfessionType, MessageContainer> lastProfessionMessages = new EnumMap<>(ProfessionType.class);
    private final Map<ProfessionType, Float> lastRawProfessionXpGains = new EnumMap<>(ProfessionType.class);

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        lastCombatXpDisplayTime = 0;
        for (ProfessionType profession : ProfessionType.values()) {
            lastProfessionXpDisplayTimes.put(profession, 0L);
        }
    }

    @SubscribeEvent
    public void onCombatXpGain(CombatXpGainEvent event) {
        if (!combat.get() || !Models.WorldState.onWorld()) return;
        if (lastCombatMessage != null && shouldEditOldMessage(lastCombatXpDisplayTime)) {
            Managers.Notification.editMessage(
                    lastCombatMessage,
                    getCombatXpGainMessage(
                            lastRawCombatXpGain + event.getGainedXpRaw(),
                            currentCombatXpPercentage + event.getGainedXpPercentage()));

            lastRawCombatXpGain += event.getGainedXpRaw();
            currentCombatXpPercentage += event.getGainedXpPercentage();
        } else {
            lastRawCombatXpGain = event.getGainedXpRaw();
            currentCombatXpPercentage = event.getGainedXpPercentage();

            lastCombatMessage = Managers.Notification.queueMessage(
                    getCombatXpGainMessage(event.getGainedXpRaw(), event.getGainedXpPercentage()));
        }

        lastCombatXpDisplayTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onProfessionXpGain(ProfessionXpGainEvent event) {
        if (!professions.get() || !Models.WorldState.onWorld()) return;
        ProfessionType profession = event.getProfession();
        float lastRawXpGain = lastRawProfessionXpGains.getOrDefault(profession, 0F);
        float currentXpPercentage = event.getCurrentXpPercentage();

        if (lastProfessionMessages.containsKey(profession)
                && shouldEditOldMessage(lastProfessionXpDisplayTimes.get(profession))) {
            lastRawXpGain += event.getGainedXpRaw();
            Managers.Notification.editMessage(
                    lastProfessionMessages.get(profession),
                    getProfessionXpGainMessage(profession, lastRawXpGain, currentXpPercentage));
        } else {
            lastRawXpGain = event.getGainedXpRaw();
            lastProfessionMessages.put(
                    profession,
                    Managers.Notification.queueMessage(
                            getProfessionXpGainMessage(profession, lastRawXpGain, currentXpPercentage)));
        }
        lastRawProfessionXpGains.put(profession, lastRawXpGain);
        lastProfessionXpDisplayTimes.put(profession, System.currentTimeMillis());

        if (filterChat.get()) {
            event.setCanceled(true);
        }
    }

    private boolean shouldEditOldMessage(long lastGainTime) {
        return System.currentTimeMillis() - lastGainTime < secondDelay.get() * 1000;
    }

    private static StyledText getProfessionXpGainMessage(
            ProfessionType profession, float rawXpGain, float currentXpPercentage) {
        return StyledText.fromString(String.format(
                "§2+%.0f %s XP (§6%.2f%%§2)", rawXpGain, profession.getProfessionIconChar(), currentXpPercentage));
    }

    private static StyledText getCombatXpGainMessage(float rawXpGain, float currentXpPercentage) {
        return StyledText.fromString(String.format("§2+%.0f XP (§6%.2f%%§2)", rawXpGain, currentXpPercentage));
    }
}
