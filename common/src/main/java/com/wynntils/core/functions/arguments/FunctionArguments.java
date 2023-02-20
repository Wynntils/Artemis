/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.functions.arguments;

import com.wynntils.utils.type.ErrorOr;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// FIXME: Expose this to user and add i18n
public final class FunctionArguments {
    private final List<Argument> arguments;
    private final Map<String, Argument> lookupMap;

    private FunctionArguments(List<Argument> arguments) {
        this.arguments = arguments;

        this.lookupMap =
                this.arguments.stream().collect(Collectors.toMap(argument -> argument.name, argument -> argument));
    }

    public Argument getArgument(String name) {
        return this.lookupMap.get(name);
    }

    public static final class Builder {
        public static final Builder EMPTY = new Builder(List.of());

        private List<Argument> arguments;

        public Builder(List<Argument> arguments) {
            this.arguments = arguments;
        }

        public FunctionArguments buildWithDefaults() {
            return new FunctionArguments(this.arguments);
        }

        public ErrorOr<FunctionArguments> buildWithValues(List<Object> values) {
            if (values.size() != this.arguments.size()) {
                return ErrorOr.error("Invalid number of arguments.");
            }

            for (int i = 0; i < this.arguments.size(); i++) {
                Argument argument = this.arguments.get(i);

                if (!argument.getType().equals(values.get(i).getClass())) {
                    return ErrorOr.error("Invalid argument type: \"%s\" is not a %s."
                            .formatted(
                                    values.get(i).toString(), argument.getType().getSimpleName()));
                }

                argument.setValue(values.get(i));
            }

            return ErrorOr.of(new FunctionArguments(this.arguments));
        }
    }

    public static final class Argument<T> {
        private static final List<Class<?>> SUPPORTED_ARGUMENT_TYPES =
                List.of(String.class, Integer.class, Double.class, Boolean.class);

        private final String name;
        private final Class<T> type;
        private final T defaultValue;

        private T value;

        public Argument(String name, Class<T> type, T defaultValue) {
            if (!SUPPORTED_ARGUMENT_TYPES.contains(type)) {
                throw new IllegalArgumentException("Unsupported argument type: " + type);
            }

            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
        }

        public void setValue(Object value) {
            if (this.value != null) {
                throw new IllegalStateException("Tried setting argument value twice.");
            }

            this.value = (T) value;
        }

        public Class<T> getType() {
            return type;
        }

        public T getValue() {
            return this.value == null ? this.defaultValue : this.value;
        }

        public Boolean getBooleanValue() {
            return (Boolean) this.getValue();
        }

        public Integer getIntegerValue() {
            return (Integer) this.getValue();
        }

        public Double getDoubleValue() {
            return (Double) this.getValue();
        }

        public String getStringValue() {
            return (String) this.getValue();
        }
    }
}
