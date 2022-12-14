/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.net;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ApiResponse extends NetResult {
    public ApiResponse(HttpRequest request) {
        super(request);
    }

    protected CompletableFuture<InputStream> getInputStreamFuture() {
        CompletableFuture<InputStream> future = NetManager.HTTP_CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpResponse::body);

        return future;
    }
}
