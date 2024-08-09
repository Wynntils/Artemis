/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.profession;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.storage.Storage;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.labels.event.TextDisplayChangedEvent;
import com.wynntils.mc.event.ContainerSetSlotEvent;
import com.wynntils.models.items.items.game.MaterialItem;
import com.wynntils.models.profession.event.ProfessionNodeGatheredEvent;
import com.wynntils.models.profession.event.ProfessionXpGainEvent;
import com.wynntils.models.profession.label.GatheringNodeLabelParser;
import com.wynntils.models.profession.label.GatheringStationLabelParser;
import com.wynntils.models.profession.type.HarvestInfo;
import com.wynntils.models.profession.type.ProfessionProgress;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.PosUtils;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.TimedSet;
import com.wynntils.utils.type.TimedValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.Position;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class ProfessionModel extends Model {
    // §7x1 [+3952§f Ⓒ§7 Woodcutting XP] §6[14.64%]
    private static final Pattern PROFESSION_NODE_EXPERIENCE_PATTERN = Pattern.compile(
            "(§.x[\\d\\.]+ )?(§.)?\\[\\+(§d)?(?<gain>\\d+)§f [ⓀⒸⒷⒿⒺⒹⓁⒶⒼⒻⒾⒽ]§7 (?<name>.+) XP\\] §6\\[(?<current>[\\d.]+)%\\]");

    // §a+1§2 Dernic Wood§6 [§e✫§8✫✫§6]
    private static final Pattern PROFESSION_NODE_HARVEST_PATTERN =
            Pattern.compile("§a\\+\\d+§2 (?<type>.+) (?<material>.+)§6 \\[§e✫((?:§8)?✫(?:§8)?)✫§6\\]");

    // §dx2.0 §7[+§d28 §fⒺ §7Scribing XP] §6[56%]
    private static final Pattern PROFESSION_CRAFT_PATTERN = Pattern.compile(
            "(§dx[\\d\\.]+ )?§7\\[\\+(§d)?(?<gain>\\d+) §f[ⓀⒸⒷⒿⒺⒹⓁⒶⒼⒻⒾⒽ] §7(?<name>.+) XP\\] §6\\[(?<current>[\\d.]+)%\\]");

    private static final Pattern PROFESSION_LEVELUP_PATTERN =
            Pattern.compile("§e\\s+You are now level (?<level>\\d+) in §f[ⓀⒸⒷⒿⒺⒹⓁⒶⒼⒻⒾⒽ]§e (?<name>.+)");

    private static final Pattern INFO_MENU_PROFESSION_LORE_PATTERN =
            Pattern.compile("§6- §7[ⓀⒸⒷⒿⒺⒹⓁⒶⒼⒻⒾⒽ] Lv. (\\d+) (.+)§8 \\[([\\d.]+)%\\]");

    private static final int MAX_HARVEST_LABEL_AGE = 4000;

    @Persisted
    private final Storage<Integer> professionDryStreak = new Storage<>(0);

    private Pair<Long, MaterialItem> lastHarvestItemGain = Pair.of(0L, null);
    private HarvestInfo lastHarvest;

    private final TimedSet<Position> gatheredNodes = new TimedSet<>(10, TimeUnit.SECONDS, true);
    private Map<ProfessionType, ProfessionProgress> professionProgressMap = new ConcurrentHashMap<>();
    private final Map<ProfessionType, TimedSet<Float>> rawXpGainInLastMinute = new HashMap<>();

    private final TimedValue<StyledText> lastProfessionLabel =
            new TimedValue<>(MAX_HARVEST_LABEL_AGE, TimeUnit.MILLISECONDS);

    public ProfessionModel() {
        super(List.of());

        Handlers.Label.registerParser(new GatheringNodeLabelParser());
        Handlers.Label.registerParser(new GatheringStationLabelParser());

        for (ProfessionType pt : ProfessionType.values()) {
            rawXpGainInLastMinute.put(pt, new TimedSet<>(1, TimeUnit.MINUTES, true));
        }
    }

    @SubscribeEvent
    public void onContainerSetSlot(ContainerSetSlotEvent.Post event) {
        Optional<MaterialItem> materialItem = Models.Item.asWynnItem(event.getItemStack(), MaterialItem.class);

        if (materialItem.isEmpty()) return;

        lastHarvestItemGain = Pair.of(System.currentTimeMillis(), materialItem.get());
    }

    @SubscribeEvent
    public void onLabelSpawn(TextDisplayChangedEvent.Text event) {
        StyledText label = event.getText();

        // Profession labels are 1-text, multi-line
        StyledText[] lines = label.split("\n");

        // We only care about multi-line labels
        if (lines.length < 2) return;

        for (StyledText line : lines) {
            Matcher professionNodeExperienceMatcher = line.getMatcher(PROFESSION_NODE_EXPERIENCE_PATTERN);
            if (professionNodeExperienceMatcher.matches()) {
                Vec3 entityPosition = event.getTextDisplay().position();

                if (gatheredNodes.stream().anyMatch(position -> PosUtils.isSame(position, entityPosition))) {
                    // We already recorded this XP gain, ignore it.
                    continue;
                }

                ProfessionType profession = ProfessionType.fromString(professionNodeExperienceMatcher.group("name"));

                // Woodcutting labels can move during "display", so position based checks don't always work
                if (profession == ProfessionType.WOODCUTTING && lastProfessionLabel.matches(line)) {
                    continue;
                }

                lastProfessionLabel.set(line);
                gatheredNodes.put(entityPosition);

                WynntilsMod.postEvent(new ProfessionXpGainEvent(
                        profession,
                        Float.parseFloat(professionNodeExperienceMatcher.group("gain")),
                        Float.parseFloat(professionNodeExperienceMatcher.group("current"))));

                ProfessionNodeGatheredEvent.LabelShown gatherEvent = new ProfessionNodeGatheredEvent.LabelShown();
                WynntilsMod.postEvent(gatherEvent);

                continue;
            }

            Matcher professionNodeHarvestMatcher = line.getMatcher(PROFESSION_NODE_HARVEST_PATTERN);
            if (professionNodeHarvestMatcher.matches()) {
                if (lastHarvestItemGain.a() + MAX_HARVEST_LABEL_AGE >= System.currentTimeMillis()
                        && lastHarvestItemGain.b() != null) {
                    MaterialItem materialItem = lastHarvestItemGain.b();
                    lastHarvest = new HarvestInfo(lastHarvestItemGain.a(), materialItem.getMaterialProfile());
                    lastHarvestItemGain = Pair.of(0L, null);

                    if (lastHarvest.materialProfile().getTier() == 3) {
                        professionDryStreak.store(0);
                    } else {
                        professionDryStreak.store(professionDryStreak.get() + 1);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onChatMessage(ChatMessageReceivedEvent event) {
        StyledText message = event.getOriginalStyledText();

        Matcher craftMatcher = message.getMatcher(PROFESSION_CRAFT_PATTERN);
        if (craftMatcher.matches()) {
            ProfessionXpGainEvent xpGainEvent = new ProfessionXpGainEvent(
                    ProfessionType.fromString(craftMatcher.group("name")),
                    Float.parseFloat(craftMatcher.group("gain")),
                    Float.parseFloat(craftMatcher.group("current")));
            WynntilsMod.postEvent(xpGainEvent);
            if (xpGainEvent.isCanceled()) {
                event.setCanceled(true);
            }
            return;
        }

        Matcher levelUpMatcher = message.getMatcher(PROFESSION_LEVELUP_PATTERN);
        if (levelUpMatcher.matches()) {
            updateLevel(
                    ProfessionType.fromString(levelUpMatcher.group("name")),
                    Integer.parseInt(levelUpMatcher.group("level")));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onXpGain(ProfessionXpGainEvent event) {
        ProfessionType profession = event.getProfession();
        ProfessionProgress oldValue = professionProgressMap.getOrDefault(profession, ProfessionProgress.NO_PROGRESS);

        // We leveled up, but we don't know how many times.
        // Set the progress, level will be parsed from other messages.
        float newPercentage = event.getCurrentXpPercentage();
        if (newPercentage == 100) {
            newPercentage = 0;
        }

        professionProgressMap.put(profession, new ProfessionProgress(oldValue.level(), newPercentage));

        rawXpGainInLastMinute.get(profession).put(event.getGainedXpRaw());
    }

    public void resetValueFromItem(ItemStack professionInfoItem) {
        Map<ProfessionType, ProfessionProgress> levels = new ConcurrentHashMap<>();
        List<StyledText> professionLore = LoreUtils.getLore(professionInfoItem);
        for (StyledText line : professionLore) {
            Matcher matcher = line.getMatcher(INFO_MENU_PROFESSION_LORE_PATTERN);

            if (matcher.matches()) {
                // NOTE: When writing this, progress was quite a bit off in this lore. Still, parse it and use it while
                // we don't have accurate info.
                levels.put(
                        ProfessionType.fromString(matcher.group(2)),
                        new ProfessionProgress(Integer.parseInt(matcher.group(1)), Float.parseFloat(matcher.group(3))));
            }
        }

        for (ProfessionType value : ProfessionType.values()) {
            levels.putIfAbsent(value, ProfessionProgress.NO_PROGRESS);
        }

        professionProgressMap = levels;
    }

    private void updateLevel(ProfessionType type, int newLevel) {
        ProfessionProgress oldValue = professionProgressMap.getOrDefault(type, ProfessionProgress.NO_PROGRESS);

        professionProgressMap.put(type, new ProfessionProgress(newLevel, oldValue.progress()));
    }

    public int getLevel(ProfessionType type) {
        return professionProgressMap
                .getOrDefault(type, ProfessionProgress.NO_PROGRESS)
                .level();
    }

    public double getProgress(ProfessionType type) {
        return professionProgressMap
                .getOrDefault(type, ProfessionProgress.NO_PROGRESS)
                .progress();
    }

    public Optional<HarvestInfo> getLastHarvest() {
        return Optional.ofNullable(lastHarvest);
    }

    public Map<ProfessionType, TimedSet<Float>> getRawXpGainInLastMinute() {
        return Collections.unmodifiableMap(rawXpGainInLastMinute);
    }

    public int getProfessionDryStreak() {
        return professionDryStreak.get();
    }
}
