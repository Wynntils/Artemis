/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.storage;

import com.wynntils.core.components.Managers;

public class Storage<T> {
    private final Class<? extends T> type;
    private T value;

    public Storage(T value) {
        this.value = value;
        type = (Class<? extends T>) value.getClass();
    }

    public T get() {
        return value;
    }

    public void store(T value) {
        this.value = value;
        Managers.Storage.persist(this);
    }

    // This must only be called by StorageManager when restoring value from disk
    void set(T value) {
        this.value = value;
    }

    Class<? extends T> getType() {
        return type;
    }
}
