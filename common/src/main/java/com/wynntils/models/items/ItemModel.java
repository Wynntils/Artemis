/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Model;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemAnnotator;
import com.wynntils.handlers.item.ItemHandler;
import com.wynntils.models.items.annotators.game.AmplifierAnnotator;
import com.wynntils.models.items.annotators.game.AspectAnnotator;
import com.wynntils.models.items.annotators.game.CharmAnnotator;
import com.wynntils.models.items.annotators.game.CraftedConsumableAnnotator;
import com.wynntils.models.items.annotators.game.CraftedGearAnnotator;
import com.wynntils.models.items.annotators.game.DungeonKeyAnnotator;
import com.wynntils.models.items.annotators.game.EmeraldAnnotator;
import com.wynntils.models.items.annotators.game.EmeraldPouchAnnotator;
import com.wynntils.models.items.annotators.game.GatheringToolAnnotator;
import com.wynntils.models.items.annotators.game.GearAnnotator;
import com.wynntils.models.items.annotators.game.GearBoxAnnotator;
import com.wynntils.models.items.annotators.game.HorseAnnotator;
import com.wynntils.models.items.annotators.game.IngredientAnnotator;
import com.wynntils.models.items.annotators.game.InsulatorAnnotator;
import com.wynntils.models.items.annotators.game.MaterialAnnotator;
import com.wynntils.models.items.annotators.game.MiscAnnotator;
import com.wynntils.models.items.annotators.game.MultiHealthPotionAnnotator;
import com.wynntils.models.items.annotators.game.PotionAnnotator;
import com.wynntils.models.items.annotators.game.PowderAnnotator;
import com.wynntils.models.items.annotators.game.RuneAnnotator;
import com.wynntils.models.items.annotators.game.SimulatorAnnotator;
import com.wynntils.models.items.annotators.game.TeleportScrollAnnotator;
import com.wynntils.models.items.annotators.game.TomeAnnotator;
import com.wynntils.models.items.annotators.game.TrinketAnnotator;
import com.wynntils.models.items.annotators.game.UnknownGearAnnotator;
import com.wynntils.models.items.annotators.gui.AbilityTreeAnnotator;
import com.wynntils.models.items.annotators.gui.ActivityAnnotator;
import com.wynntils.models.items.annotators.gui.ArchetypeAbilitiesAnnotator;
import com.wynntils.models.items.annotators.gui.CharacterAnnotator;
import com.wynntils.models.items.annotators.gui.CosmeticTierAnnotator;
import com.wynntils.models.items.annotators.gui.DailyRewardMultiplierAnnotator;
import com.wynntils.models.items.annotators.gui.IngredientPouchAnnotator;
import com.wynntils.models.items.annotators.gui.SeaskipperDestinationAnnotator;
import com.wynntils.models.items.annotators.gui.ServerAnnotator;
import com.wynntils.models.items.annotators.gui.SkillCrystalAnnotator;
import com.wynntils.models.items.annotators.gui.SkillPointAnnotator;
import com.wynntils.models.items.annotators.gui.TerritoryAnnotator;
import com.wynntils.models.items.annotators.gui.TerritoryUpgradeAnnotator;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public class ItemModel extends Model {
    public ItemModel() {
        super(List.of());

        // GameItemAnnotators
        // For efficiency, register these annotators first
        Handlers.Item.registerAnnotator(new GearAnnotator());
        Handlers.Item.registerAnnotator(new GearBoxAnnotator());
        Handlers.Item.registerAnnotator(new TomeAnnotator());
        Handlers.Item.registerAnnotator(new CharmAnnotator());
        Handlers.Item.registerAnnotator(new IngredientAnnotator());
        Handlers.Item.registerAnnotator(new MaterialAnnotator());

        // Then alphabetically
        Handlers.Item.registerAnnotator(new AmplifierAnnotator());
        Handlers.Item.registerAnnotator(new AspectAnnotator());
        Handlers.Item.registerAnnotator(new CraftedConsumableAnnotator());
        Handlers.Item.registerAnnotator(new CraftedGearAnnotator());
        Handlers.Item.registerAnnotator(new DungeonKeyAnnotator());
        Handlers.Item.registerAnnotator(new EmeraldAnnotator());
        Handlers.Item.registerAnnotator(new EmeraldPouchAnnotator());
        Handlers.Item.registerAnnotator(new GatheringToolAnnotator());
        Handlers.Item.registerAnnotator(new HorseAnnotator());
        Handlers.Item.registerAnnotator(new InsulatorAnnotator());
        Handlers.Item.registerAnnotator(new MultiHealthPotionAnnotator());
        Handlers.Item.registerAnnotator(new PotionAnnotator());
        Handlers.Item.registerAnnotator(new PowderAnnotator());
        Handlers.Item.registerAnnotator(new RuneAnnotator());
        Handlers.Item.registerAnnotator(new SimulatorAnnotator());
        Handlers.Item.registerAnnotator(new TeleportScrollAnnotator());
        Handlers.Item.registerAnnotator(new TrinketAnnotator());

        // GuiItemAnnotators
        Handlers.Item.registerAnnotator(new AbilityTreeAnnotator());
        Handlers.Item.registerAnnotator(new ActivityAnnotator());
        Handlers.Item.registerAnnotator(new ArchetypeAbilitiesAnnotator());
        Handlers.Item.registerAnnotator(new CharacterAnnotator());
        Handlers.Item.registerAnnotator(new CosmeticTierAnnotator());
        Handlers.Item.registerAnnotator(new DailyRewardMultiplierAnnotator());
        Handlers.Item.registerAnnotator(new IngredientPouchAnnotator());
        Handlers.Item.registerAnnotator(new SeaskipperDestinationAnnotator());
        Handlers.Item.registerAnnotator(new ServerAnnotator());
        Handlers.Item.registerAnnotator(new SkillCrystalAnnotator());
        Handlers.Item.registerAnnotator(new SkillPointAnnotator());
        Handlers.Item.registerAnnotator(new TerritoryAnnotator());
        Handlers.Item.registerAnnotator(new TerritoryUpgradeAnnotator());

        // ItemAnnotators
        // This must be done last
        Handlers.Item.registerAnnotator(new UnknownGearAnnotator());
        Handlers.Item.registerAnnotator(new MiscAnnotator());
        Handlers.Item.registerAnnotator(new FallbackAnnotator());
    }

    public Optional<WynnItem> getWynnItem(ItemStack itemStack) {
        Optional<ItemAnnotation> annotationOpt = ItemHandler.getItemStackAnnotation(itemStack);
        if (annotationOpt.isEmpty()) return Optional.empty();
        if (!(annotationOpt.get() instanceof WynnItem wynnItem)) return Optional.empty();

        return Optional.of(wynnItem);
    }

    public <T extends WynnItem> Optional<T> asWynnItem(ItemStack itemStack, Class<T> clazz) {
        Optional<ItemAnnotation> annotationOpt = ItemHandler.getItemStackAnnotation(itemStack);
        if (annotationOpt.isEmpty()) return Optional.empty();
        if (!(annotationOpt.get() instanceof WynnItem wynnItem)) return Optional.empty();
        if (wynnItem.getClass() != clazz) return Optional.empty();

        return Optional.of((T) wynnItem);
    }

    public <T> Optional<T> asWynnItemProperty(ItemStack itemStack, Class<T> clazz) {
        Optional<ItemAnnotation> annotationOpt = ItemHandler.getItemStackAnnotation(itemStack);
        if (annotationOpt.isEmpty()) return Optional.empty();
        if (!(annotationOpt.get() instanceof WynnItem wynnItem)) return Optional.empty();
        if (!clazz.isAssignableFrom(wynnItem.getClass())) return Optional.empty();

        return Optional.of((T) wynnItem);
    }

    public static final class FallbackAnnotator implements ItemAnnotator {
        @Override
        public ItemAnnotation getAnnotation(ItemStack itemStack, StyledText name) {
            return new WynnItem();
        }
    }
}
