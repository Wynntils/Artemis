/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.consumers.functions.arguments;

import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.ErrorOr;
import com.wynntils.utils.type.NamedValue;
import com.wynntils.utils.type.RangedValue;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class FunctionArguments {
    private final List<Argument<?>> arguments;
    private final Map<String, Argument<?>> lookupMap;

    private FunctionArguments(List<Argument<?>> arguments) {
        this.arguments = arguments;

        this.lookupMap =
                this.arguments.stream().collect(Collectors.toMap(argument -> argument.name, argument -> argument));
    }

    @SuppressWarnings("unchecked")
    public <T> Argument<T> getArgument(String name) {
        return (Argument<T>) this.lookupMap.get(name);
    }

    public abstract static class Builder {
        protected final List<Argument<?>> arguments;

        protected Builder(List<Argument<?>> arguments) {
            this.arguments = arguments;
        }

        public ErrorOr<FunctionArguments> buildWithValues(List<Object> values) {
            if (arguments.stream()
                            .filter(argument -> argument instanceof ListArgument<?>)
                            .count()
                    > 1) {
                throw new IllegalArgumentException("Only one list argument is allowed.");
            }

            boolean hasListArgument = arguments.stream().anyMatch(argument -> argument instanceof ListArgument<?>);
            if (hasListArgument && !(arguments.getLast() instanceof ListArgument<?>)) {
                throw new IllegalArgumentException("List argument needs to be the last argument.");
            }

            if (!hasListArgument && values.size() != this.arguments.size()) {
                return ErrorOr.error("Invalid number of arguments");
            }

            for (int i = 0; i < this.arguments.size(); i++) {
                Argument<?> argument = this.arguments.get(i);

                if (argument instanceof ListArgument<?> listArgument) {
                    List<Object> listValues = values.subList(i, values.size());

                    Optional<Object> nonMatchingValue = listValues.stream()
                            .filter(value -> !argument.getType().isAssignableFrom(value.getClass()))
                            .findFirst();
                    if (nonMatchingValue.isPresent()) {
                        return ErrorOr.error("Invalid argument type in list argument: \"%s\" is not a %s"
                                .formatted(
                                        nonMatchingValue.get(),
                                        argument.getType().getSimpleName()));
                    }

                    listArgument.setValues(listValues);
                    break;
                }

                if (!argument.getType().isAssignableFrom(values.get(i).getClass())) {
                    return ErrorOr.error("Invalid argument type: \"%s\" is not a %s"
                            .formatted(
                                    values.get(i).toString(), argument.getType().getSimpleName()));
                }

                argument.setValue(values.get(i));
            }

            return ErrorOr.of(new FunctionArguments(this.arguments));
        }

        public String getArgumentNamesString() {
            return arguments.stream().map(Argument::getName).collect(Collectors.joining("; "));
        }

        public List<Argument<?>> getArguments() {
            return Collections.unmodifiableList(arguments);
        }

        public int getArgumentCount() {
            return arguments.size();
        }
    }

    // A builder for functions with required arguments
    public static class RequiredArgumentBuilder extends Builder {
        public static final Builder EMPTY = new RequiredArgumentBuilder(List.of());

        public RequiredArgumentBuilder(List<Argument<?>> arguments) {
            super(arguments);
        }
    }

    // A builder for functions with optional arguments
    public static class OptionalArgumentBuilder extends Builder {
        public static final Builder EMPTY = new OptionalArgumentBuilder(List.of());

        public OptionalArgumentBuilder(List<Argument<?>> arguments) {
            super(arguments);

            if (arguments.stream().anyMatch(argument -> argument instanceof ListArgument<?>)) {
                throw new IllegalArgumentException("List arguments are not supported for optional arguments.");
            }
        }

        public List<Object> getDefaults() {
            return this.arguments.stream().map(Argument::getDefaultValue).collect(Collectors.toList());
        }
    }

    public static class Argument<T> {
        private static final List<Class<?>> SUPPORTED_ARGUMENT_TYPES = List.of(
                String.class,
                Boolean.class,
                Integer.class,
                Long.class,
                Double.class,
                Number.class,
                CappedValue.class,
                RangedValue.class,
                NamedValue.class,
                Location.class);

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

        @SuppressWarnings("unchecked")
        protected void setValue(Object value) {
            this.value = (T) value;
        }

        public String getName() {
            return name;
        }

        public Class<T> getType() {
            return type;
        }

        public T getValue() {
            return this.value == null ? this.defaultValue : this.value;
        }

        public T getDefaultValue() {
            return this.defaultValue;
        }

        public Boolean getBooleanValue() {
            return (Boolean) this.getValue();
        }

        public Integer getIntegerValue() {
            if (this.type == Number.class) {
                return ((Number) this.getValue()).intValue();
            }

            return (Integer) this.getValue();
        }

        public Long getLongValue() {
            if (this.type == Number.class) {
                return ((Number) this.getValue()).longValue();
            }

            return (Long) this.getValue();
        }

        public Double getDoubleValue() {
            if (this.type == Number.class) {
                return ((Number) this.getValue()).doubleValue();
            }

            return (Double) this.getValue();
        }

        public CappedValue getCappedValue() {
            return (CappedValue) this.getValue();
        }

        public RangedValue getRangedValue() {
            return (RangedValue) this.getValue();
        }

        public NamedValue getNamedValue() {
            return (NamedValue) this.getValue();
        }

        public Location getLocation() {
            return (Location) this.getValue();
        }

        public String getStringValue() {
            return (String) this.getValue();
        }

        public ListArgument<T> asList() {
            return (ListArgument<T>) this;
        }
    }

    public static class ListArgument<T> extends Argument<T> {
        public ListArgument(String name, Class<T> type) {
            super(name, type, null);
        }

        @SuppressWarnings("unchecked")
        protected void setValues(List<Object> values) {
            this.setValue(values.stream().map(value -> (T) value).collect(Collectors.toList()));
        }

        @SuppressWarnings("unchecked")
        public List<T> getValues() {
            return (List<T>) this.getValue();
        }
    }
}
