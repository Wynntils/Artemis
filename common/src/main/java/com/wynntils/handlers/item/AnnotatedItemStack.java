/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.item;

public interface AnnotatedItemStack {
    ItemAnnotation getAnnotation();

    void setAnnotation(ItemAnnotation annotation);
}
