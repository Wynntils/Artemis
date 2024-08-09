/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.profession.type;

import com.wynntils.utils.type.Pair;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;

public final class MaterialProfile {
    private static final Map<MaterialType, List<SourceMaterial>> SOURCE_MATERIALS = Map.of(
            MaterialType.ORE,
            List.of(
                    new SourceMaterial("Copper", 1),
                    new SourceMaterial("Granite", 10),
                    new SourceMaterial("Gold", 20),
                    new SourceMaterial("Sandstone", 30),
                    new SourceMaterial("Iron", 40),
                    new SourceMaterial("Silver", 50),
                    new SourceMaterial("Cobalt", 60),
                    new SourceMaterial("Kanderstone", 70),
                    new SourceMaterial("Diamond", 80),
                    new SourceMaterial("Molten", 90),
                    new SourceMaterial("Voidstone", 100),
                    new SourceMaterial("Dernic", 110)),
            MaterialType.LOG,
            List.of(
                    new SourceMaterial("Oak", 1),
                    new SourceMaterial("Birch", 10),
                    new SourceMaterial("Willow", 20),
                    new SourceMaterial("Acacia", 30),
                    new SourceMaterial("Spruce", 40),
                    new SourceMaterial("Jungle", 50),
                    new SourceMaterial("Dark", 60),
                    new SourceMaterial("Light", 70),
                    new SourceMaterial("Pine", 80),
                    new SourceMaterial("Avo", 90),
                    new SourceMaterial("Sky", 100),
                    new SourceMaterial("Dernic", 110)),
            MaterialType.CROP,
            List.of(
                    new SourceMaterial("Wheat", 1),
                    new SourceMaterial("Barley", 10),
                    new SourceMaterial("Oat", 20),
                    new SourceMaterial("Malt", 30),
                    new SourceMaterial("Hops", 40),
                    new SourceMaterial("Rye", 50),
                    new SourceMaterial("Millet", 60),
                    new SourceMaterial("Decay", 70),
                    new SourceMaterial("Rice", 80),
                    new SourceMaterial("Sorghum", 90),
                    new SourceMaterial("Hemp", 100),
                    new SourceMaterial("Dernic", 110)),
            MaterialType.FISH,
            List.of(
                    new SourceMaterial("Gudgeon", 1),
                    new SourceMaterial("Trout", 10),
                    new SourceMaterial("Salmon", 20),
                    new SourceMaterial("Carp", 30),
                    new SourceMaterial("Icefish", 40),
                    new SourceMaterial("Piranha", 50),
                    new SourceMaterial("Koi", 60),
                    new SourceMaterial("Gylia", 70),
                    new SourceMaterial("Bass", 80),
                    new SourceMaterial("Molten", 90),
                    new SourceMaterial("Starfish", 100),
                    new SourceMaterial("Dernic", 110)));

    private final ResourceType resourceType;
    private final SourceMaterial sourceMaterial;
    private final int tier;

    private MaterialProfile(ResourceType resourceType, SourceMaterial sourceMaterial, int tier) {
        this.resourceType = resourceType;
        this.sourceMaterial = sourceMaterial;
        this.tier = tier;
    }

    public static MaterialProfile lookup(String sourceMaterialName, String resourceTypeName, int tier) {
        SourceMaterial sourceMaterial = SOURCE_MATERIALS.values().stream()
                .flatMap(Collection::stream)
                .filter(material -> material.name().equals(sourceMaterialName))
                .findFirst()
                .orElse(null);
        if (sourceMaterial == null) return null;

        ResourceType resourceType = ResourceType.fromString(resourceTypeName);
        if (resourceType == null) return null;

        return new MaterialProfile(resourceType, sourceMaterial, tier);
    }

    public static Optional<Pair<MaterialType, SourceMaterial>> findByMaterialName(
            String name, ChatFormatting labelColor) {
        return SOURCE_MATERIALS.entrySet().stream()
                .filter(entry -> entry.getKey().getLabelColor() == labelColor)
                .flatMap(entry -> entry.getValue().stream().map(material -> new Pair<>(entry.getKey(), material)))
                .filter(pair -> pair.value().name().equals(name))
                .findFirst();
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public SourceMaterial getSourceMaterial() {
        return sourceMaterial;
    }

    public int getTier() {
        return tier;
    }

    public enum MaterialType {
        ORE(ProfessionType.MINING, ChatFormatting.WHITE),
        LOG(ProfessionType.WOODCUTTING, ChatFormatting.GOLD),
        CROP(ProfessionType.FARMING, ChatFormatting.YELLOW),
        FISH(ProfessionType.FISHING, ChatFormatting.AQUA);

        private final ProfessionType professionType;
        private final ChatFormatting labelColor;

        MaterialType(ProfessionType professionType, ChatFormatting labelColor) {
            this.professionType = professionType;
            this.labelColor = labelColor;
        }

        public ProfessionType getProfessionType() {
            return professionType;
        }

        public ChatFormatting getLabelColor() {
            return labelColor;
        }
    }

    public enum ResourceType {
        INGOT(MaterialType.ORE),
        GEM(MaterialType.ORE),
        WOOD(MaterialType.LOG),
        PAPER(MaterialType.LOG),
        STRING(MaterialType.CROP),
        GRAINS(MaterialType.CROP),
        OIL(MaterialType.FISH),
        MEAT(MaterialType.FISH);

        private final MaterialType materialType;

        ResourceType(MaterialType materialType) {
            this.materialType = materialType;
        }

        private static ResourceType fromString(String str) {
            try {
                return ResourceType.valueOf(str.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        public MaterialType getMaterialType() {
            return materialType;
        }
    }

    @Override
    public String toString() {
        return "MaterialProfile{" + "resourceType="
                + resourceType + ", sourceMaterial="
                + sourceMaterial + ", tier="
                + tier + '}';
    }

    public record SourceMaterial(String name, int level) {}
}
