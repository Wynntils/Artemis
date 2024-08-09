/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.net;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.utils.FileUtils;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.type.Pair;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Manages the URLs used by the mod.
 *
 * <h2>URL sources:</h2>
 * <ol>
 *     <li>Resource bundled with the mod</li>
 *     <li>Local cache</li>
 *     <li>Online source</li>
 * </ol>
 *
 * <p>
 *     The sources are loaded in the order listed above, and the first valid source is used.
 *     If a source is loaded later, it can override the previous sources, if the version is higher.
 * </p>
 * <p>
 *     The URLs are stored in a JSON file, which is read and parsed into a map of id - url pairs.
 * </p>
 *
 * <h3>URL override</h3>
 * <p>
 *     It is possible to override the URL list by setting the java property `wynntils.url.list.override.link` to a valid URL.
 *     This will override the bundled URL list. This is useful for regions that block Github CDN or for private purposes.
 * </p>
 * <p>
 *     It is also possible to ignore the cache by setting the java property `wynntils.url.list.ignore.cache` to true.
 *     This will force the mod to download the URL list from the online source, even if the cache is valid.
 *     This is useful for development purposes, or for regions where the url list tends to change frequently.
 * </p>
 */
public final class UrlManager extends Manager {
    // -Dwynntils.url.list.override.link=https://example.com/urls.json
    private static final String URLS_OVERRIDE_ENV = "wynntils.url.list.override.link";
    // -Dwynntils.url.list.ignore.cache=true
    private static final String URLS_IGNORE_CACHE_ENV = "wynntils.url.list.ignore.cache";

    private final URI urlListOverrideUri;
    private final boolean ignoreCache;

    private Map<UrlId, UrlInfo> urlMap = Map.of();
    private int version = -1;

    public UrlManager(NetManager netManager) {
        super(List.of(netManager));

        // Read the url override from environment
        String urlListOverride = System.getProperty(URLS_OVERRIDE_ENV);
        URI parsedOverrideUri;
        try {
            parsedOverrideUri = URI.create(urlListOverride);
        } catch (Exception e) {
            parsedOverrideUri = null;
            WynntilsMod.error("Invalid URL list override link: " + urlListOverride);
        }
        urlListOverrideUri = parsedOverrideUri;

        // Read the ignore cache from environment
        ignoreCache = Boolean.parseBoolean(System.getProperty(URLS_IGNORE_CACHE_ENV));

        // Log the settings
        if (urlListOverrideUri == null) {
            WynntilsMod.info(
                    "Loading urls.json in the normal mode. Url cache is " + (ignoreCache ? "ignored" : "used") + ".");
        } else {
            WynntilsMod.info("Loading urls.json from " + urlListOverrideUri + ". Url cache is "
                    + (ignoreCache ? "ignored" : "used") + ".");
        }

        loadUrls();
    }

    public UrlInfo getUrlInfo(UrlId urlId) {
        return urlMap.get(urlId);
    }

    public String getUrl(UrlId urlId) {
        UrlInfo urlInfo = urlMap.get(urlId);

        // This is only valid for POST URLs, or GET URLs with no arguments
        assert (urlInfo.method() == Method.POST || urlInfo.arguments().isEmpty());

        return urlInfo.url();
    }

    public String buildUrl(UrlId urlId, Map<String, String> arguments) {
        UrlInfo urlInfo = urlMap.get(urlId);

        return buildUrl(urlInfo, arguments);
    }

    public String buildUrl(UrlInfo urlInfo, Map<String, String> arguments) {
        // Verify that arguments match with what is specified
        assert (arguments.keySet().equals(new HashSet<>(urlInfo.arguments())))
                : "Arguments mismatch for " + urlInfo.url + ", expected: " + urlInfo.arguments() + " got: "
                        + arguments.keySet();

        // Replace %{argKey} with arg value in URL string
        return arguments.keySet().stream()
                .reduce(
                        urlInfo.url(),
                        (str, argKey) -> str.replaceAll(
                                "%\\{" + argKey + "\\}",
                                // First encode with specified encoder (if any), then finish by
                                // always url encoding arguments
                                StringUtils.encodeUrl(urlInfo.encoding().encode(arguments.get(argKey)))));
    }

    @Override
    public void reloadData() {
        loadUrls();
    }

    private void loadUrls() {
        // Figure out where to load the URLs from initially

        // If we don't have an override, read the URLs in a normal way
        if (urlListOverrideUri == null) {
            // Start by reading the URLs from the resource embedded in the mod, so we have something to rely on
            readEmbeddedUrls();

            if (!ignoreCache) {
                // Then try to read the URLs from the local cache
                readLocalUrlCache();
            }

            // Then trigger a (re-)download from the net to the cache
            // We need to do the urlInfo lookup ourself, since we might have
            // a embryonic netManager which can't do much.
            UrlInfo urlInfo = getUrlInfo(UrlId.DATA_STATIC_URLS);
            if (urlInfo == null) {
                WynntilsMod.error("ERROR: Failed to load baseline URL list. Try deleting Wynntils cache.");
                throw new RuntimeException("Missing DATA_STATIC_URLS from cached and bundled urls.json");
            }
            URI uri = URI.create(urlInfo.url());
            downloadAndReadRemoteUrls(uri);
        } else {
            if (!ignoreCache) {
                // Try to read the URLs from the local cache
                readLocalUrlCache();
            }

            // Then trigger a (re-)download from the net to the cache
            downloadAndReadRemoteUrls(urlListOverrideUri);
        }
    }

    private void readEmbeddedUrls() {
        try {
            readInputStreamForUrl(getBundledInputStream());
        } catch (IOException | JsonSyntaxException e) {
            // We can't load the url list bundled. This is a catastrophic failure.
            // Nothing will work after this, so just abort.
            throw new RuntimeException(
                    "ERROR: Bundled JSON has invalid syntax or is malformed, or it could not be read. This might be because of a corrupt download. Try updating Wynntils.",
                    e);
        }
    }

    private void readLocalUrlCache() {
        Pair<FileInputStream, File> localCache = getLocalCacheInputStreams();
        if (localCache != null) {
            try {
                readInputStreamForUrl(localCache.key());
            } catch (IOException | JsonSyntaxException e) {
                // The local cache is corrupt. Delete it and try again.
                WynntilsMod.warn("Problem reading URL list from local cache, deleting it.", e);
                FileUtils.deleteFile(localCache.value());
            }
        }
    }

    private void downloadAndReadRemoteUrls(URI uri) {
        String localFileName = UrlId.DATA_STATIC_URLS.getId();

        Download dl = Managers.Net.download(uri, localFileName);
        dl.handleInputStream(inputStream -> {
            try {
                Pair<Integer, Map<UrlId, UrlInfo>> tryMap = readUrls(inputStream);
                tryUpdateUrlMap(tryMap, true);
            } catch (IOException e) {
                WynntilsMod.warn("Problem updating URL list from online source", e);
            }
        });
    }

    private void readInputStreamForUrl(InputStream tryStream) throws JsonSyntaxException, IOException {
        try (InputStream inputStream = tryStream) {
            Pair<Integer, Map<UrlId, UrlInfo>> tryMap = readUrls(inputStream);
            tryUpdateUrlMap(tryMap, false);
        } catch (MalformedJsonException e) {
            // This is handled by the caller, so just rethrow
            throw new MalformedJsonException(e);
        }
    }

    private void tryUpdateUrlMap(Pair<Integer, Map<UrlId, UrlInfo>> tryMap, boolean forceUpdateOnSameVersion) {
        // Even if the version is the same, we might want to update the URLs if the urls are downloaded from the
        // internet since the URLs might have changed. (local cache and bundled URLs are not always updated)
        if (tryMap.a() > version || (forceUpdateOnSameVersion && tryMap.a() == version)) {
            urlMap = tryMap.b();
            version = tryMap.a();
        }
    }

    private InputStream getBundledInputStream() {
        return WynntilsMod.getModResourceAsStream("urls.json");
    }

    private Pair<FileInputStream, File> getLocalCacheInputStreams() {
        InputStream bundledStream = WynntilsMod.getModResourceAsStream("urls.json");

        // First check if there is a copy in the local cache
        File cacheFile = Managers.Net.getCacheFile(UrlId.DATA_STATIC_URLS.getId());
        if (cacheFile.exists() && cacheFile.length() > 0) {
            // Yes, we have a cache. Use it to populate the map
            try {
                return Pair.of(new FileInputStream(cacheFile), cacheFile);
            } catch (FileNotFoundException e) {
                // This should not happens since we just checked, but fall through
                // to bundled case if so
            }
        }

        // No usable cache.
        return null;
    }

    private Pair<Integer, Map<UrlId, UrlInfo>> readUrls(InputStream inputStream)
            throws IOException, JsonSyntaxException {
        byte[] data = inputStream.readAllBytes();
        String json = new String(data, StandardCharsets.UTF_8);
        Type type = new TypeToken<List<UrlProfile>>() {}.getType();
        List<UrlProfile> urlProfiles = WynntilsMod.GSON.fromJson(json, type);

        Map<UrlId, UrlInfo> newMap = new HashMap<>();

        int version = 0;
        for (UrlProfile urlProfile : urlProfiles) {
            if (urlProfile.version != 0) {
                // This is the special version record
                version = urlProfile.version;
                continue;
            }
            List<String> arguments = urlProfile.arguments == null ? List.of() : urlProfile.arguments;
            Optional<UrlId> urlId = UrlId.from(urlProfile.id);

            if (urlId.isEmpty()) {
                // This is a URL we don't know about. Ignore it.
                continue;
            }

            newMap.put(
                    urlId.get(),
                    new UrlInfo(
                            urlProfile.url,
                            arguments,
                            Method.from(urlProfile.method),
                            Encoding.from(urlProfile.encoding),
                            Optional.ofNullable(urlProfile.md5)));
        }

        // Sanity check that we got all ids
        for (UrlId urlId : UrlId.values()) {
            if (!newMap.containsKey(urlId)) {
                WynntilsMod.warn("Missing URL in urls.json: " + urlId);
                return Pair.of(-1, Map.of());
            }
        }

        return Pair.of(version, newMap);
    }

    public enum Method {
        GET,
        POST;

        private static Method from(String str) {
            if (str == null || str.isEmpty()) return GET; // GET is default if unspecified
            return Method.valueOf(str.toUpperCase(Locale.ROOT));
        }
    }

    public enum Encoding {
        NONE(s -> s),
        CARGO(s -> "'" + s.replace("'", "\\'") + "'"),
        WIKI(s -> s.replace(" ", "_"));

        private final Function<String, String> encoder;

        Encoding(Function<String, String> encoder) {
            this.encoder = encoder;
        }

        private static Encoding from(String str) {
            if (str == null || str.isEmpty()) return NONE; // NONE is default if unspecified
            return Encoding.valueOf(str.toUpperCase(Locale.ROOT));
        }

        String encode(String input) {
            return encoder.apply(input);
        }
    }

    public record UrlInfo(String url, List<String> arguments, Method method, Encoding encoding, Optional<String> md5) {}

    private static final class UrlProfile {
        int version;
        String id;
        String url;
        String method;
        List<String> arguments;
        String md5;
        String encoding;
    }
}
