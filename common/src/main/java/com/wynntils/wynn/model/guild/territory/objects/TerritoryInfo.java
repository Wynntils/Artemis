/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.guild.territory.objects;

import com.wynntils.mc.objects.CustomColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TerritoryInfo {
    private static final Pattern GENERATOR_PATTERN =
            Pattern.compile("(.\s)?\\+([0-9]*) (Emeralds|Ore|Wood|Fish|Crops) per Hour");
    private static final Pattern STORAGE_PATTERN = Pattern.compile("(.\s)?([0-9]+)\\/([0-9]+) stored");
    private static final Pattern DEFENSE_PATTERN = Pattern.compile("Territory Defences: (.+)");
    private static final Pattern TREASURY_PATTERN = Pattern.compile("✦ Treasury: (.+)");

    final HashMap<GuildResource, TerritoryStorage> storage = new HashMap<>();
    final HashMap<GuildResource, Integer> generators = new HashMap<>();
    final List<String> tradingRoutes = new ArrayList<>();

    GuildResourceValues treasury;
    GuildResourceValues defences;

    final boolean headquarters;
    final CustomColor color;

    /**
     * Holds and generates data based on the Achievement values gave by Wynncraft
     * Example of lore parsed here:
     * <p>
     * Celestial Tigers [TGR]
     * <p>
     * +12150 Emeralds per Hour   ---> represents a generator
     * 222/10000 stored           ---> represents a storage
     * Ⓑ 6/500 stored
     * Ⓒ 3/500 stored
     * Ⓚ +28800 Fish per Hour
     * Ⓚ 447/500 stored
     * Ⓙ 3/500 stored
     * <p>
     * ✦ Treasury: High          ---> represents the treasury value (High)
     * Territory Defences: Low   ---> represents the territory defence (Low)
     * <p>
     * Trading Routes:
     * - Tree Island              ---> represents a trading route
     * - Pirate Town
     * - Volcano Upper
     *
     * @param raw the input achievement description without colors
     * @param colored the input achievement description with colors
     */
    public TerritoryInfo(String[] raw, String[] colored, boolean headquarters) {
        this.headquarters = headquarters;

        for (int i = 0; i < raw.length; i++) {
            String unformatted = raw[i];
            String formatted = colored[i];

            // initial trading route parsing
            if (unformatted.startsWith("-")) {
                tradingRoutes.add(unformatted.substring(2));
                continue;
            }

            // treasury parsing
            Matcher treasureMatcher = TREASURY_PATTERN.matcher(unformatted);
            if (treasureMatcher.matches()) {
                treasury = GuildResourceValues.fromString(treasureMatcher.group(1));
                continue;
            }

            // defence parsing
            Matcher defenseMatcher = DEFENSE_PATTERN.matcher(unformatted);
            if (defenseMatcher.matches()) {
                defences = GuildResourceValues.fromString(defenseMatcher.group(1));
                continue;
            }

            // finding the resource type
            GuildResource resource = null;
            for (GuildResource type : GuildResource.values()) {
                if (!formatted.contains(type.getColor().toString())) continue;

                resource = type;
                break;
            }

            if (resource == null) continue;

            // generator
            if (unformatted.contains("per Hour")) {
                Matcher m = GENERATOR_PATTERN.matcher(unformatted);
                if (!m.matches()) continue;

                generators.put(resource, Integer.parseInt(m.group(2)));
                continue;
            }

            // storage
            Matcher m = STORAGE_PATTERN.matcher(unformatted);
            if (!m.matches()) continue;

            storage.put(resource, new TerritoryStorage(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
        }

        float h = 0;
        float s = 0.6f;
        float v = 0.9f;

        double sum = generators.entrySet().stream()
                .filter(c -> c.getKey() != GuildResource.EMERALD)
                .map(Map.Entry::getValue)
                .mapToInt(Integer::intValue)
                .sum();

        for (Map.Entry<GuildResource, Integer> generator : generators.entrySet()) {
            switch (generator.getKey()) {
                case ORE:
                    v = 1f;
                    s = 0.3f;
                    break;
                case FISH:
                    h += 180 * (generator.getValue() / sum);
                    break;
                case WOOD:
                    h += 120 * (generator.getValue() / sum);
                    break;
                case CROPS:
                    h += 60 * (generator.getValue() / sum);
                    break;
                case EMERALD:
                    break;
            }
        }

        color = CustomColor.fromHSV(h / 360f, s, v, 1);
    }

    public Map<GuildResource, Integer> getGenerators() {
        return generators;
    }

    public Map<GuildResource, TerritoryStorage> getStorage() {
        return storage;
    }

    public List<String> getTradingRoutes() {
        return tradingRoutes;
    }

    public Integer getGeneration(GuildResource resource) {
        return generators.getOrDefault(resource, 0);
    }

    public TerritoryStorage getStorage(GuildResource resource) {
        return storage.get(resource);
    }

    public GuildResourceValues getTreasury() {
        return treasury;
    }

    public GuildResourceValues getDefences() {
        return defences;
    }

    public CustomColor getColor() {
        return color;
    }

    public boolean isHeadquarters() {
        return headquarters;
    }
}
