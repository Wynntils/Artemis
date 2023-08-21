/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.itemfilter.filters;

import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.properties.ProfessionItemProperty;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.services.itemfilter.type.ItemFilter;
import com.wynntils.services.itemfilter.type.ItemFilterInstance;
import com.wynntils.utils.type.ErrorOr;
import java.util.List;

public class ProfessionItemFilter extends ItemFilter {
    public ProfessionItemFilter() {
        super(List.of("prof"));
    }

    @Override
    public ErrorOr<ItemFilterInstance> createInstance(String inputString) {
        ProfessionType profession = ProfessionType.fromString(inputString);
        if (profession == null) {
            return ErrorOr.error(getTranslation("invalidProfession", inputString));
        } else {
            return ErrorOr.of(new ProfessionItemFilterInstance(profession));
        }
    }

    private static class ProfessionItemFilterInstance implements ItemFilterInstance {
        private final ProfessionType profession;

        protected ProfessionItemFilterInstance(ProfessionType profession) {
            this.profession = profession;
        }

        @Override
        public boolean matches(WynnItem wynnItem) {
            if (profession == null) return false;

            if (wynnItem instanceof ProfessionItemProperty professionItemProperty) {
                return professionItemProperty.getProfessionTypes().contains(profession);
            }

            return false;
        }
    }
}
