/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.objects;

import java.util.Objects;

/**
 * The Pair Type Holds 1 field of type T and 1 field of type J
 */
public class MutablePair<T, J> {
    public T a;
    public J b;

    public MutablePair(T a, J b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return a.toString() + ", " + b.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof MutablePair<?, ?> other)) {
            return false;
        }

        return Objects.deepEquals(a, other.a) && Objects.deepEquals(b, other.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }
}
