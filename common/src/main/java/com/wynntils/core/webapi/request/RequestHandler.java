/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.webapi.request;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wynntils.core.webapi.LoadingPhase;
import com.wynntils.utils.Utils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class RequestHandler {
    /** If set to true, will not make HTTP requests. */
    public static boolean cacheOnly = false;

    public RequestHandler() {}

    private final ExecutorService pool =
            Executors.newFixedThreadPool(
                    4,
                    new ThreadFactoryBuilder()
                            .setNameFormat("wynntils-web-request-pool-%d")
                            .build());
    private final List<Request> requests = new ArrayList<>();
    private int maxParallelGroup = 0;
    private int dispatchId = 0;

    /** Enqueue a new {@link Request Request} */
    public void addRequest(Request req) {
        synchronized (this) {
            for (Request request : requests) {
                if (req.id.equals(request.id)) {
                    return;
                }
            }

            requests.add(req);

            if (req.parallelGroup > maxParallelGroup) {
                maxParallelGroup = req.parallelGroup;
            }
        }
    }

    /** Send all enqueued requests and wait until complete */
    public void dispatch() {
        dispatch(false);
    }

    /** Enqueue a new {@link Request} and dispatches it */
    public void addAndDispatch(Request req, boolean async) {
        addRequest(req);
        dispatch(async);
    }

    /** Enqueue a new {@link Request} and dispatches it */
    public void addAndDispatch(Request req) {
        addRequest(req);
        dispatch(false);
    }

    /** Send all enqueued requests inside of a new thread and return that thread */
    public Thread dispatchAsync() {
        return dispatch(true);
    }

    public Thread dispatch(boolean async) {
        List<Request>[] groupedRequests;
        boolean anyRequests = false;
        int thisDispatch;

        synchronized (this) {
            groupedRequests = (ArrayList<Request>[]) new ArrayList[maxParallelGroup + 1];

            for (int i = 0; i < maxParallelGroup + 1; ++i) {
                groupedRequests[i] = new ArrayList<>();
            }

            for (Request request : requests) {
                if (request.currentlyHandling != LoadingPhase.UNLOADED) continue;

                anyRequests = true;
                request.currentlyHandling = LoadingPhase.TO_LOAD;
                groupedRequests[request.parallelGroup].add(request);
            }

            maxParallelGroup = 0;
            thisDispatch = ++dispatchId;
        }

        if (anyRequests) {
            if (!async) {
                handleDispatch(thisDispatch, groupedRequests, 0);
                return null;
            } else {
                Thread t =
                        new Thread(
                                () -> handleDispatch(thisDispatch, groupedRequests, 0),
                                "wynntils-webrequesthandler");
                t.start();
                return t;
            }
        }

        return null;
    }

    private void handleDispatch(
            int dispatchId, List<Request>[] groupedRequests, int currentGroupIndex) {
        List<Request> currentGroup = groupedRequests[currentGroupIndex];
        if (currentGroup.size() == 0) {
            nextDispatch(dispatchId, groupedRequests, currentGroupIndex);
            return;
        }

        List<Callable<Void>> tasks = new ArrayList<>(currentGroup.size());
        for (Request req : currentGroup) {
            tasks.add(
                    () -> {
                        if (req.cacheValidator != null) {
                            try {
                                byte[] cachedData = FileUtils.readFileToByteArray(req.cacheFile);
                                if (req.cacheValidator.test(cachedData)) {
                                    try {
                                        if (req.handler.test(null, cachedData)) {
                                            return null;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    Utils.logUnknown(
                                            req.id
                                                    + ": Error using cached data that passed"
                                                    + " validator!");
                                    FileUtils.deleteQuietly(req.cacheFile);
                                } else {
                                    Utils.logUnknown(
                                            "Cache for "
                                                    + req.id
                                                    + " at "
                                                    + req.cacheFile.getPath()
                                                    + " could not be validated");
                                }
                            } catch (FileNotFoundException ignore) {
                            } catch (Exception e) {
                                Utils.logUnknown(
                                        "Error occurred whilst trying to use validated cache for "
                                                + req.id
                                                + " at "
                                                + req.cacheFile.getPath());
                                e.printStackTrace();
                            }
                        }

                        if (!cacheOnly) {
                            try {
                                HttpURLConnection st = req.establishConnection();
                                if (st.getResponseCode() != 200) {
                                    if (!req.onError(st.getResponseCode()))
                                        req.currentlyHandling = LoadingPhase.LOADED;
                                    st.disconnect();
                                    return null;
                                }

                                try {
                                    byte[] data = IOUtils.toByteArray(st.getInputStream());

                                    if (req.handler != null) {
                                        if (req.handler.test(st, data)) {
                                            if (req.cacheFile != null) {
                                                try {
                                                    FileUtils.writeByteArrayToFile(
                                                            req.cacheFile, data);
                                                } catch (Exception e) {
                                                    Utils.logUnknown(
                                                            "Error occurred whilst writing cache"
                                                                    + " for "
                                                                    + req.id);
                                                    e.printStackTrace();
                                                    FileUtils.deleteQuietly(req.cacheFile);
                                                }
                                            }
                                        } else {
                                            Utils.logUnknown(
                                                    "Error occurred whilst fetching "
                                                            + req.id
                                                            + " from "
                                                            + req.url);
                                        }
                                    }
                                } catch (IOException e) {
                                    Utils.logUnknown(
                                            "Error occurred whilst fetching "
                                                    + req.id
                                                    + " from "
                                                    + req.url
                                                    + ": "
                                                    + (e instanceof SocketTimeoutException
                                                            ? "Socket timeout (server may be down)"
                                                            : e.getMessage()));
                                }
                            } catch (Exception e) {
                                Utils.logUnknown(
                                        "Error occurred whilst fetching "
                                                + req.id
                                                + " from "
                                                + req.url);
                                e.printStackTrace();
                            }
                        }

                        req.currentlyHandling = LoadingPhase.LOADED;
                        return null;
                    });
        }

        try {
            pool.invokeAll(tasks);
        } catch (InterruptedException e) {
            Set<String> completedIds = new HashSet<>();
            Set<String> interruptedIds = new HashSet<>();
            for (List<Request> requests : groupedRequests)
                for (Request request : requests) {
                    (request.currentlyHandling == LoadingPhase.LOADED
                                    ? completedIds
                                    : interruptedIds)
                            .add(request.id);
                }
            synchronized (this) {
                requests.removeIf(
                        req -> {
                            if (completedIds.contains(req.id)) {
                                return true;
                            }
                            if (interruptedIds.contains(req.id)) {
                                req.currentlyHandling = LoadingPhase.LOADED;
                            }

                            return false;
                        });
            }

            return;
        }

        nextDispatch(dispatchId, groupedRequests, currentGroupIndex);
    }

    private void nextDispatch(
            int dispatchId, List<Request>[] groupedRequests, int currentGroupIndex) {
        if (currentGroupIndex != groupedRequests.length - 1) {
            handleDispatch(dispatchId, groupedRequests, currentGroupIndex + 1);
            return;
        }

        // Last group; Remove handled requests
        Set<String> ids = new HashSet<>();
        for (List<Request> requests : groupedRequests)
            for (Request request : requests) {
                if (request.currentlyHandling == LoadingPhase.LOADED) ids.add(request.id);
            }
        synchronized (this) {
            requests.removeIf(req -> ids.contains(req.id));
        }
    }
}
