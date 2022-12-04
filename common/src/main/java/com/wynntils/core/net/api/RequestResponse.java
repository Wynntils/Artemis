/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.net.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RequestResponse {
    private final byte[] blob;

    public RequestResponse(byte[] blob) {
        this.blob = blob;
    }

    public void handleJsonObject(Predicate<JsonObject> handler) {}

    public void handleJsonArray(Predicate<JsonArray> handler) {}

    public void handleBytes(Predicate<byte[]> handler) {}

    public void handleJsonArray(Predicate<JsonArray> handler, Consumer<Void> errorHandler) {}
}
