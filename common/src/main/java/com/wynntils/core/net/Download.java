/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.net;

import com.wynntils.core.WynntilsMod;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.apache.commons.io.FileUtils;

public class Download {
    static Map<HttpRequest, CompletableFuture<HttpResponse<Path>>> downloadFutures = new HashMap<>();
    static Map<HttpRequest, CompletableFuture<Void>> processFutures = new HashMap<>();

    HttpRequest request;
    private final File localFile;

    // Saved since we might need to get timestamps from the HttpResponse
    CompletableFuture<HttpResponse<Path>> future;

    public Download(File localFile) {
        this.localFile = localFile;
    }

    public Download(File localFile, HttpRequest request) {
        this.localFile = localFile;
        this.request = request;
    }

    private CompletableFuture<HttpResponse<Path>> downloadToCacheAsync() {
        FileUtils.deleteQuietly(localFile);
        localFile.getParentFile().mkdirs();
        future = NetManager.HTTP_CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.ofFile(localFile.toPath()))
                .whenComplete((ignored, exc) -> {
                    downloadFutures.remove(request);
                });
        // in case of failure:
        //        FileUtils.deleteQuietly(cacheFile);
        downloadFutures.put(request, future);
        return future;
    }

    private CompletableFuture<InputStream> getInputStreamAsync() {
        if (request == null) {
            try {
                InputStream inputStream = new FileInputStream(localFile);
                return CompletableFuture.supplyAsync(() -> inputStream);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            return downloadToCacheAsync().thenApply(response -> {
                try {
                    return new FileInputStream(response.body().toFile());
                } catch (FileNotFoundException e) {
                    // FIXME
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void doHandle(Consumer<InputStream> onCompletion, Consumer<Throwable> onError) {
        CompletableFuture newFuture;
        newFuture = getInputStreamAsync()
                .thenAccept((is) -> onCompletion.accept(is))
                .exceptionally(e -> {
                    // FIXME: fix error handling correctly!
                    onError.accept(e);
                    return null;
                });
        storeProcessFuture(newFuture);
    }

    private void storeProcessFuture(CompletableFuture<Void> processFuture) {
        CompletableFuture<Void> newFuture = processFuture.whenComplete((ignored, exc) -> {
            processFutures.remove(request);
        });
        processFutures.put(request, newFuture);
    }

    public void handleInputStream(Consumer<InputStream> onCompletion, Consumer<Throwable> onError) {
        doHandle(onCompletion, onError);
    }

    public void handleInputStream(Consumer<InputStream> onCompletion) {
        handleInputStream(onCompletion, onError -> {
            WynntilsMod.warn("Error while reading resource");
        });
    }

    public void handleReader(Consumer<Reader> onCompletion) {
        handleInputStream(is -> onCompletion.accept(new InputStreamReader(is)));
    }

    public long getTimestamp() {
        // FIXME: handle case if we read from cache as fallback!
        try {
            HttpHeaders headers = future.get().headers();
            OptionalLong a = headers.firstValueAsLong("timestamp");
            if (a.isEmpty()) return System.currentTimeMillis();
            return a.getAsLong();
        } catch (InterruptedException | ExecutionException e) {
            WynntilsMod.warn("Cannot retrieve http header timestamp");
            return System.currentTimeMillis();
        }
    }
}
