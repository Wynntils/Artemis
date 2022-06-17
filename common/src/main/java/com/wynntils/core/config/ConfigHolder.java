/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.config;

import com.wynntils.core.Reference;
import com.wynntils.core.features.Feature;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.lang3.reflect.FieldUtils;

public class ConfigHolder {
    private final Feature parent;
    private final Field field;
    private final Class<?> fieldType;

    private final String category;
    private final Config metadata;

    private final Object defaultValue;

    public ConfigHolder(Feature parent, Field field, String category, Config metadata) {
        this.parent = parent;
        this.field = field;
        this.fieldType = field.getType();

        this.category = category;
        this.metadata = metadata;

        // save default value to enable easy resetting
        this.defaultValue = getValue();
    }

    public Class<?> getType() {
        return fieldType;
    }

    public Field getField() {
        return field;
    }

    public String getFieldName() {
        return field.getName();
    }

    public String getJsonName() {
        return parent.getClass().getSimpleName() + "." + field.getName();
    }

    public String getCategory() {
        return category;
    }

    public Config getMetadata() {
        return metadata;
    }

    public Object getValue() {
        try {
            return FieldUtils.readField(field, parent, true);
        } catch (IllegalAccessException e) {
            Reference.LOGGER.error("Unable to get field " + getJsonName());
            e.printStackTrace();
            return null;
        }
    }

    public boolean setValue(Object value) {
        try {
            FieldUtils.writeField(field, parent, value, true);
            parent.updateConfigOption(this);
            return true;
        } catch (IllegalAccessException e) {
            Reference.LOGGER.error("Unable to set field " + getJsonName());
            e.printStackTrace();
            return false;
        }
    }

    public boolean isDefault() {
        return defaultValue.equals(getValue());
    }

    public void reset() {
        setValue(defaultValue);
    }

    public Object tryParseStringValue(String value) {
        try {
            return fieldType.getConstructor(String.class).newInstance(value);
        } catch (NumberFormatException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ignored) {
        }

        // couldn't parse value
        return null;
    }
}
