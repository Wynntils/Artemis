/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.objects.bvh;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.wynntils.utils.objects.IBoundingBox;
import com.wynntils.utils.objects.Pair;
import com.wynntils.utils.objects.Referenceable;
import com.wynntils.utils.objects.bvh.ISplitStrategy.StrategyFactory;
import com.wynntils.utils.objects.minQueue.FibonacciHeapMinQueue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.world.phys.Vec3;

/**
 * A data-structure to efficiently find elements in 3D space based on spatial sorting.
 * <p>
 * This class is thread-safe: Any amount of threads may read and write.<br />
 * For every operation, a "happened before" relationship is inferred.
 * </p>
 *
 * @author Kepler-17c
 *
 * @param <T>
 *            type of the 3D objects in this hierarchy
 */
public class BoundingVolumeHierarchy<T extends IBoundingBox> implements Set<T> {
    /**
     * Default upper limit of leaf elements in lowest tree nodes.
     */
    private static final int MINIMUM_LEAF_COUNT = 16;
    /**
     * Default strategy to sort elements spatially.
     */
    private static final ISplitStrategy DEFAULT_SPLIT_STRATEGY = new OctTreeStrategy();
    /**
     * Tolerated number of dirty operations, until a rebuild is triggered.
     */
    private static final int DIRTINESS_THRESHOLD = 16;

    // ┌──────────┐
    // │ BVH Data │
    // └──────────┘

    /**
     * Synchronisation lock for BVH-rebuilding.
     */
    private final Object dataLock = new Object();
    /**
     * State of the parallel rebuilding operation.
     */
    private boolean rebuildInProgress = false;
    /**
     * Buffer to collect incoming elements prior to and during rebuild.
     * <p>
     * Elements in this buffer must not be in the {@link #tree} or {@link #pendingDeletes}.
     * </p>
     * <p>
     * Instead of editing the stored buffer object, a new modified version must be created and assigned instead. This is
     * to ensure transaction behaviour with concurrent reads and writes.
     * </p>
     */
    private Set<T> pendingInserts = new HashSet<>();
    /**
     * Buffer to collect deletion requests prior to and during rebuild.
     * <p>
     * Elements in this buffer must be in the {@link #tree} and not be in {@link #pendingInserts}.
     * </p>
     * <p>
     * Instead of editing the stored buffer object, a new modified version must be created and assigned instead. This is
     * to ensure transaction behaviour with concurrent reads and writes.
     * </p>
     */
    private Set<T> pendingDeletes = new HashSet<>();
    /**
     * Spatial tree of the BVH.
     */
    private BvhNode<T> tree;
    /**
     * Factory to get the concrete split strategy.
     */
    private StrategyFactory splitStrategyFactory;
    /**
     * Optional settings for the split strategy.
     */
    private Map<String, String> splitStrategySettings;
    /**
     * Strategy to construct the tree.
     */
    private ISplitStrategy splitStrategy;
    /**
     * Maximum number of leaves a node can have.
     */
    private int leafCount;
    /**
     * Index for direct non-traversal access of elements.
     */
    private Map<T, BvhNode<T>> objectCache;

    // ┌──────────────┐
    // │ Constructors │
    // └──────────────┘

    /**
     * Empty BVH with default split setting and leaf count.
     */
    public BoundingVolumeHierarchy() {
        this(Collections.emptyList());
    }

    /**
     * Preinitialised BVH with default split setting and leaf count.
     *
     * @param elements
     *            to build the tree from
     */
    public BoundingVolumeHierarchy(final Iterable<T> elements) {
        this(elements, DEFAULT_SPLIT_STRATEGY);
    }

    /**
     * Preinitialised BVH with custom split setting and default leaf count.
     *
     * @param elements
     *            to build the tree from
     * @param splitStrategy
     *            setting for branching behaviour
     */
    public BoundingVolumeHierarchy(final Iterable<T> elements, final ISplitStrategy splitStrategy) {
        this(elements, splitStrategy, MINIMUM_LEAF_COUNT);
    }

    /**
     * Preinitialised BVH with custom split setting and leaf count.
     *
     * @param elements
     *            to build the tree from
     * @param splitStrategy
     *            setting for branching behaviour
     * @param leafCount
     *            setting for max allowed leaves at the end of a branch
     */
    public BoundingVolumeHierarchy(
            final Iterable<T> elements, final ISplitStrategy splitStrategy, final int leafCount) {
        if (elements != null) {
            elements.forEach(this.pendingInserts::add);
        }
        this.tree = new BvhNode<>();
        this.splitStrategy = splitStrategy != null ? splitStrategy : DEFAULT_SPLIT_STRATEGY;
        final int buckets = this.splitStrategy.bucketCount();
        this.leafCount = Math.max(Math.max(MINIMUM_LEAF_COUNT, leafCount), buckets * buckets);
        this.objectCache = new HashMap<>();
        // force rebuild
        this.forceRebuild();
    }

    // ┌────────────────┐
    // │ Public Methods │
    // └────────────────┘

    @Override
    public boolean add(final T element) {
        if (element != null) {
            return this.addAll(Collections.singleton(element));
        }
        return false;
    }

    @Override
    public boolean addAll(final Collection<? extends T> elements) {
        boolean changed = false;
        if (elements != null) {
            synchronized (this.dataLock) {
                this.pendingDeletes = new HashSet<>(this.pendingDeletes);
                this.pendingInserts = new HashSet<>(this.pendingInserts);
                for (final T e : elements) {
                    if (e == null) {
                        return false;
                    }
                    if (this.pendingDeletes.contains(e)) {
                        this.pendingDeletes.remove(e);
                        changed = true;
                    } else if (!this.objectCache.containsKey(e) && !this.pendingInserts.contains(e)) {
                        this.pendingInserts.add(e);
                        changed = true;
                    }
                }
            }
            this.requestRebuild();
        }
        return changed;
    }

    @Override
    public boolean remove(final Object element) {
        return this.removeAll(Collections.singleton(element));
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        boolean changed = false;
        final Set<T> changedInserts = new HashSet<>(this.pendingInserts);
        final Set<T> changedDeletes = new HashSet<>(this.pendingDeletes);
        for (final Object element : c) {
            if (element instanceof IBoundingBox) {
                synchronized (this.dataLock) {
                    if (this.objectCache.containsKey(element) && !this.pendingDeletes.contains(element)) {
                        try {
                            @SuppressWarnings("unchecked")
                            final T e = (T) element;
                            changedDeletes.add(e);
                        } catch (final ClassCastException e) {
                            e.printStackTrace();
                        }
                        changed = true;
                    } else if (this.pendingInserts.contains(element)) {
                        changedInserts.remove(element);
                        changed = true;
                    }
                }
            }
        }
        this.pendingInserts = changedInserts;
        this.pendingDeletes = changedDeletes;
        return changed;
    }

    @Override
    public int size() {
        synchronized (this.dataLock) {
            return this.tree.getLeafCount() + this.pendingInserts.size() - this.pendingDeletes.size();
        }
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public boolean contains(final Object o) {
        synchronized (this.dataLock) {
            return (this.objectCache.containsKey(o) && !this.pendingDeletes.contains(o))
                    || this.pendingInserts.contains(o);
        }
    }

    @Override
    public Object[] toArray() {
        return this.stream().toArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E[] toArray(E[] a) {
        int size;
        Iterator<T> iterator;
        synchronized (this.dataLock) {
            size = this.size();
            iterator = this.iterator();
        }
        if (a.length < size) {
            a = (E[]) Array.newInstance(a.getClass(), this.size());
        }
        for (int i = 0; i < size; i++) {
            a[i++] = (E) iterator.next();
        }
        if (size < a.length) {
            a[size] = null;
        }
        return a;
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        Map<T, BvhNode<T>> cache;
        synchronized (this.dataLock) {
            cache = this.objectCache;
        }
        return !c.stream().anyMatch(e -> !cache.containsKey(e));
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        boolean changed = false;
        synchronized (this.dataLock) {
            this.pendingInserts = new HashSet<>(this.pendingInserts);
            for (final T elem : this.pendingInserts) {
                if (!c.contains(elem)) {
                    this.pendingInserts.remove(elem);
                    changed = true;
                }
            }
            this.pendingDeletes = new HashSet<>(this.pendingDeletes);
            for (final T elem : this.objectCache.keySet()) {
                if (!c.contains(elem)) {
                    this.pendingDeletes.add(elem);
                }
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        synchronized (this.dataLock) {
            this.tree = new BvhNode<>();
            this.pendingInserts = Collections.emptySet();
            this.pendingDeletes = Collections.emptySet();
        }
    }

    /**
     * Finds the nearest element to {@code location} stored in the BVH.
     *
     * @param location
     *            to compare the distance to.
     * @return The element closest to {@code location} or {@code null} if the BVH is empty.
     */
    public T findNearest(final Vec3 location) {
        // prepare traversal queues
        T nearest = null;
        final Queue<BvhNode<T>> nodeQueue = new FibonacciHeapMinQueue<>(getDistanceComparator(location));
        final Comparator<T> leafComparator = getDistanceComparator(location);
        // get initial data
        Set<T> inserts;
        Set<T> deletes;
        synchronized (this.dataLock) {
            inserts = this.pendingInserts;
            deletes = this.pendingDeletes;
            nodeQueue.add(this.tree);
        }
        nearest = inserts.stream().sorted(leafComparator).findFirst().orElse(null);
        // traverse tree
        while (!nodeQueue.isEmpty()
                && (nearest == null
                        || nearest.squareDistance(location) > nodeQueue.peek().squareDistance(location))) {
            final BvhNode<T> treeNode = nodeQueue.poll();
            for (final T leaf : treeNode.getLeaves()) {
                if (!deletes.contains(leaf) && (nearest == null || leafComparator.compare(leaf, nearest) < 0)) {
                    nearest = leaf;
                }
            }
            nodeQueue.addAll(treeNode.getChildNodes());
        }
        return nearest;
    }

    /**
     * Tell the BVH to update bounds after changing the position or size of an element.
     *
     * @param element
     *            that has been changed.
     */
    public void updateBounds(final T element) { // TODO: find better alternative (if possible)
        synchronized (this.dataLock) {
            final BvhNode<T> node = this.objectCache.get(element);
            if (node != null) {
                node.updateOwnBounds();
                node.propagateBoundsUpwards();
                this.forceRebuild(); // TODO: should be requestRebuild() with dirtiness metric
            }
        }
    }

    /**
     * Changes the split strategy and triggers a rebuild if updated successfully.
     * <p>
     * This is a convenience method and equivalent to calling {@link #changeStrategy(StrategyFactory, Map)} without
     * settings:<br />
     * {@code changeStrategy(factory, null)}
     * </p>
     *
     * @param factory
     *            to create the strategy.
     * @return Whether the strategy was updated successfully.
     */
    public boolean changeStrategy(final StrategyFactory factory) {
        return this.changeStrategy(factory, null);
    }

    /**
     * Changes the split strategy and triggers a rebuild if updated successfully.
     *
     * @param factory
     *            to create the strategy.
     * @param settings
     *            of the strategy.
     * @return Whether the strategy was updated successfully.
     */
    public boolean changeStrategy(final StrategyFactory factory, final Map<String, String> settings) {
        synchronized (this.dataLock) {
            boolean changed;
            if (factory == this.splitStrategyFactory) {
                changed = this.changeStrategySettings(settings);
                // rebuild is already triggered when settings change
            } else {
                this.splitStrategyFactory = factory;
                this.splitStrategySettings = settings;
                this.splitStrategy = factory.run(settings);
                this.forceRebuild();
                changed = true;
            }
            return changed;
        }
    }

    /**
     * Changes the split strategy's settings and triggers a rebuild if updated successfully.
     * <p>
     * The input is treated as a diff and will update the present configuration:
     * <ul>
     * <li>missing values are set</li>
     * <li>present values are replaced</li>
     * <li>setting {@code null} reverts the value to default</li>
     * </ul>
     * </p>
     *
     * @param settings
     *            to update to.
     * @return Whether the settings were updated successfully.
     */
    public boolean changeStrategySettings(final Map<String, String> settings) {
        synchronized (this.dataLock) {
            boolean changed = false;
            final Map<String, String> updatedSettings = new HashMap<>(this.splitStrategySettings);
            for (final Entry<String, String> e : settings.entrySet()) {
                if (updatedSettings.get(e.getKey()) != e.getValue()) {
                    changed = true;
                    if (e.getValue() == null) {
                        updatedSettings.remove(e.getKey());
                    } else {
                        updatedSettings.put(e.getKey(), e.getValue());
                    }
                }
            }
            if (changed) {
                this.forceRebuild();
            }
            return changed;
        }
    }

    @Override
    public Iterator<T> iterator() {
        synchronized (this.dataLock) {
            return this.pendingInserts.isEmpty() && this.pendingDeletes.isEmpty()
                    ? this.tree.iterator()
                    : new BvhIterator<T>(this.tree, this.pendingInserts, this.pendingDeletes);
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        synchronized (this.dataLock) {
            return this.pendingInserts.isEmpty() && this.pendingDeletes.isEmpty()
                    ? this.tree.spliterator()
                    : new BvhSpliterator<T>(this.tree, this.pendingInserts, this.pendingDeletes);
        }
    }

    // ┌─────────────────┐
    // │ Private Methods │
    // └─────────────────┘

    /**
     * Checks the BVH's dirtiness and initiates a parallel rebuild if necessary.
     */
    private void requestRebuild() {
        synchronized (this.dataLock) {
            if (this.pendingInserts.size() + this.pendingDeletes.size() >= DIRTINESS_THRESHOLD) {
                this.forceRebuild();
            }
        }
    }

    /**
     * Performs a parallel rebuild of the tree and resolves pending operations.
     */
    private void forceRebuild() {
        BvhNode<T> oldTree;
        Set<T> oldPendingInserts;
        Set<T> oldPendingDeletes;
        ISplitStrategy strategy;
        int leafCount;
        synchronized (this.dataLock) {
            if (this.rebuildInProgress) {
                return;
            }
            this.rebuildInProgress = true;
            oldTree = this.tree;
            oldPendingInserts = this.pendingInserts;
            oldPendingDeletes = this.pendingDeletes;
            strategy = this.splitStrategy;
            leafCount = this.leafCount;
        }
        new Thread(() -> {
                    // build tree
                    final Pair<BvhNode<T>, Map<T, BvhNode<T>>> rebuiltStructure =
                            buildTree(oldTree, oldPendingInserts, oldPendingDeletes, strategy, leafCount);
                    // apply updated tree
                    synchronized (this.dataLock) {
                        this.rebuildInProgress = false;
                        this.tree = rebuiltStructure.a;
                        this.objectCache = rebuiltStructure.b;
                        // sort out changes to pending operations during rebuild
                        final Set<T> filteredPendingInserts = new HashSet<>();
                        final Set<T> filteredPendingDeletes = new HashSet<>();
                        for (final T i : oldPendingInserts) {
                            // pending element deleted during rebuild
                            if (!this.pendingInserts.contains(i)) {
                                filteredPendingDeletes.add(i);
                            }
                        }
                        for (final T i : this.pendingInserts) {
                            // new element added during rebuild
                            if (!oldPendingInserts.contains(i)) {
                                filteredPendingInserts.add(i);
                            }
                        }
                        for (final T d : oldPendingDeletes) {
                            // deletion-element added back during rebuild
                            if (!this.pendingDeletes.contains(d)) {
                                filteredPendingInserts.add(d);
                            }
                        }
                        for (final T d : this.pendingDeletes) {
                            // tree element deleted during rebuild
                            if (!oldPendingDeletes.contains(d)) {
                                filteredPendingDeletes.add(d);
                            }
                        }
                        this.pendingInserts = filteredPendingInserts;
                        this.pendingDeletes = filteredPendingDeletes;
                    }
                    // check if another rebuild is necessary due to modifications during rebuild
                    this.requestRebuild();
                })
                .start();
    }

    /**
     * Builds a new bounding-volume-hierarchy tree with object-cache.
     *
     * @param <E>
     *            element type of the tree.
     * @param elements
     *            to build the tree from.
     * @param inserts
     *            to the tree.
     * @param deletes
     *            on the tree.
     * @param splitStrategy
     *            for bucket-sorting the elements.
     * @param leafCount
     *            is the maximum amount of leaves a node may have.
     * @return the new tree and object-cache for the given elements.
     */
    private static <E extends IBoundingBox> Pair<BvhNode<E>, Map<E, BvhNode<E>>> buildTree(
            final Iterable<E> elements,
            final Collection<E> inserts,
            final Collection<E> deletes,
            final ISplitStrategy splitStrategy,
            final int leafCount) {
        // prepare result containers
        final BvhNode<E> tree = new BvhNode<>(StreamSupport.stream(elements.spliterator(), true)
                .filter(e -> !deletes.contains(e))
                .collect(Collectors.toList()));
        inserts.forEach(tree::addLeaf);
        final Map<E, BvhNode<E>> objectCache = new ConcurrentHashMap<>();
        // prepare multi-threading
        final Queue<BvhNode<E>> queue = new ConcurrentLinkedQueue<>();
        queue.add(tree);
        final List<Thread> workers = new ArrayList<>();
        // create workers
        final int processors = Runtime.getRuntime().availableProcessors();
        final AtomicInteger queuedNodesCount = new AtomicInteger(1);
        for (int i = 0; i < processors; i++) {
            workers.add(
                    new TreeBuildingWorker<>(queue, queuedNodesCount, workers, leafCount, splitStrategy, objectCache));
        }
        // start and collect threads
        workers.forEach(Thread::start);
        workers.forEach(t -> {
            try {
                t.join();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        });
        tree.updateBounds();
        return new Pair<>(tree, objectCache);
    }

    /**
     * Helper function to generate generic distance comparator functions for {@link IBoundingBox} implementing classes.
     *
     * @param <E>
     *            type safety argument.
     * @param location
     *            to compare the distance to.
     * @return The comparator function based on the given location.
     */
    private static <E extends IBoundingBox> Comparator<E> getDistanceComparator(final Vec3 location) {
        return Comparator.comparingDouble(a -> a.squareDistance(location));
    }

    // ┌─────────────────────────┐
    // │ Internal Helper Classes │
    // └─────────────────────────┘

    /**
     * Worker thread for parallel BVH tree construction.
     *
     * @author Kepler-17c
     *
     * @param <T>
     *            type of the elements in the tree.
     */
    private static class TreeBuildingWorker<T extends IBoundingBox> extends Thread {
        /**
         * Nodes to be processed.
         */
        private final Queue<BvhNode<T>> queue;
        /**
         * Number of nodes that haven't been fully processed yet.
         */
        private final AtomicInteger queuedNodesCount;
        /**
         * Wait/notify communication object.
         */
        private final Object workers;
        /**
         * Maximum number of leaves on a branch.
         */
        private final int leafCount;
        /**
         * Strategy for grouping elements and splitting nodes.
         */
        private final ISplitStrategy splitStrategy;
        /**
         * New object cache for the BVH.
         */
        private final Map<T, BvhNode<T>> objectCache;

        /**
         * Constructs the worker from references to the necessary data structures.
         *
         * @param queue
         *            of nodes to process.
         * @param queuedNodesCount
         *            counts nodes that need processing.
         * @param workers
         *            is the list of all workers and notifier object for them.
         * @param leafCount
         *            for nodes to terminate splitting.
         * @param splitStrategy
         *            for splitting the nodes.
         * @param objectCache
         *            to register terminating nodes to their leaves.
         */
        public TreeBuildingWorker(
                final Queue<BvhNode<T>> queue,
                final AtomicInteger queuedNodesCount,
                final Object workers,
                final int leafCount,
                final ISplitStrategy splitStrategy,
                final Map<T, BvhNode<T>> objectCache) {
            this.queue = queue;
            this.queuedNodesCount = queuedNodesCount;
            this.workers = workers;
            this.leafCount = leafCount;
            this.splitStrategy = splitStrategy;
            this.objectCache = objectCache;
        }

        @Override
        public void run() {
            BvhNode<T> nextNode;
            while (true) {
                // wait for a node, ready to be processed
                while ((nextNode = this.queue.poll()) == null) {
                    if (this.queuedNodesCount.get() == 0) {
                        // all nodes have been processed - wake up all workers to let them return
                        synchronized (this.workers) {
                            this.workers.notifyAll();
                        }
                        return;
                    }
                    // couldn't get a node - wait for new tasks to be submitted
                    try {
                        synchronized (this.workers) {
                            this.workers.wait();
                        }
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // create fixed reference for access in lambdas
                final BvhNode<T> activeNode = nextNode;
                // split the node if necessary - then submit child-nodes as new tasks
                if (activeNode.getLeaves().size() > this.leafCount) {
                    this.splitStrategy.split(activeNode.getLeaves()).stream()
                            .map(BvhNode::new)
                            .forEach(activeNode::addChildNode);
                    activeNode.clearLeaves();
                    activeNode.getChildNodes().forEach(node -> node.setParent(activeNode));
                    activeNode.getChildNodes().forEach(this.queue::add);
                    synchronized (this.workers) {
                        this.workers.notifyAll();
                    }
                } else {
                    activeNode.getLeaves().forEach(element -> this.objectCache.put(element, activeNode));
                }
                // update the task counter: new tasks, minus the now finished one
                this.queuedNodesCount.addAndGet(activeNode.getChildNodes().size() - 1);
            }
        }
    }

    /**
     * Special iterator for the internal BVH structure, taking pending inserts and deletes into account.
     *
     * @author Kepler-17c
     *
     * @param <T>
     *            type of the elements in the BVH.
     */
    private static class BvhIterator<T extends IBoundingBox> implements Iterator<T> {
        /**
         * Elements from processed nodes.
         */
        private final Queue<T> elementQueue;
        /**
         * Nodes to be processed. Uses a stack for depth-first order.
         */
        private final Stack<BvhNode<T>> nodeStack;
        /**
         * Elements pending for deletion. They need to be filtered out while traversing the tree.
         */
        private final Collection<T> pendingDeletes;

        /**
         * Creates the iterator from the BVH's internal state objects.
         *
         * @param tree
         *            of the BVH.
         * @param pendingInserts
         *            on the tree.
         * @param pendingDeletes
         *            on the tree.
         */
        public BvhIterator(
                final BvhNode<T> tree, final Collection<T> pendingInserts, final Collection<T> pendingDeletes) {
            this.elementQueue = new LinkedList<>(pendingInserts);
            this.nodeStack = new Stack<>();
            this.nodeStack.add(tree);
            this.pendingDeletes = pendingDeletes;
            this.updateQueue();
        }

        /**
         * Fills {@link #elementQueue} with elements from the tree.
         * <p>
         * If the queue isn't empty, nothing will be changed. Same goes for an empty {@link #nodeStack}.
         * </p>
         */
        private void updateQueue() {
            while (this.elementQueue.isEmpty() && !this.nodeStack.isEmpty()) {
                final BvhNode<T> node = this.nodeStack.pop();
                this.nodeStack.addAll(node.getChildNodes());
                for (final T elem : node.getLeaves()) {
                    if (this.pendingDeletes.contains(elem)) {
                        continue;
                    }
                    this.elementQueue.add(elem);
                }
            }
        }

        @Override
        public boolean hasNext() {
            return !this.elementQueue.isEmpty();
        }

        @Override
        public T next() {
            final T elem = this.elementQueue.poll();
            this.updateQueue();
            return elem;
        }
    }

    /**
     * Special spliterator for the BVH to take pending inserts and deletes into account.
     *
     * @author Kepler-17c
     *
     * @param <T>
     *            type of the elements in the BVH.
     */
    private static class BvhSpliterator<T extends IBoundingBox> implements Spliterator<T> {
        /**
         * Elements from processed nodes.
         */
        private final Queue<T> elementQueue;
        /**
         * Nodes to be processed. Uses a stack for depth-first order.
         */
        private final Stack<BvhNode<T>> nodeStack;
        /**
         * Elements to be deleted have to be filtered out while processing.
         */
        private final Set<T> pendingDeletes;

        /**
         * Builds the spliterator from references to the internal state objects of the BVH.
         *
         * @param tree
         *            of the BVH.
         * @param pendingInserts
         *            on the tree.
         * @param pendingDeletes
         *            on the tree.
         */
        public BvhSpliterator(final BvhNode<T> tree, final Set<T> pendingInserts, final Set<T> pendingDeletes) {
            this.elementQueue = new LinkedList<>(pendingInserts);
            this.nodeStack = new Stack<>();
            this.nodeStack.add(tree);
            this.pendingDeletes = pendingDeletes;
        }

        /**
         * Internal constructor for {@link #trySplit()}.
         *
         * @param nodeStack
         *            for this split.
         * @param pendingDeletes
         *            to be filtered out.
         */
        private BvhSpliterator(final Stack<BvhNode<T>> nodeStack, final Set<T> pendingDeletes) {
            this.elementQueue = new LinkedList<>();
            this.nodeStack = nodeStack;
            this.pendingDeletes = pendingDeletes;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super T> action) {
            while (this.elementQueue.isEmpty() && !this.nodeStack.isEmpty()) {
                final BvhNode<T> node = this.nodeStack.pop();
                this.nodeStack.addAll(node.getChildNodes());
                for (final T elem : node.getLeaves()) {
                    if (!this.pendingDeletes.contains(elem)) {
                        this.elementQueue.add(elem);
                    }
                }
            }
            if (this.elementQueue.isEmpty()) {
                return false;
            } else {
                action.accept(this.elementQueue.poll());
                return true;
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            while (!this.nodeStack.isEmpty() && this.nodeStack.size() < 2) {
                final BvhNode<T> node = this.nodeStack.pop();
                this.nodeStack.addAll(node.getChildNodes());
                for (final T elem : node.getLeaves()) {
                    if (!this.pendingDeletes.contains(elem)) {
                        this.elementQueue.add(elem);
                    }
                }
            }
            if (this.nodeStack.isEmpty()) {
                return null;
            } else {
                final int halfSize = this.nodeStack.size() / 2;
                final Stack<BvhNode<T>> halfOfNodes = new Stack<>();
                for (int i = 0; i < halfSize; i++) {
                    halfOfNodes.add(this.nodeStack.pop());
                }
                return new BvhSpliterator<>(halfOfNodes, this.pendingDeletes);
            }
        }

        @Override
        public long estimateSize() {
            return this.nodeStack.stream().mapToInt(BvhNode::getLeafCount).sum() + this.elementQueue.size();
        }

        @Override
        public int characteristics() {
            return DISTINCT | SIZED | NONNULL | IMMUTABLE | CONCURRENT | SUBSIZED;
        }
    }

    // ┌───────────────────────────────────┐
    // │ Serialisation and Deserialisation │
    // └───────────────────────────────────┘

    /**
     * Helper function to generate the required type adapters for BVH (de)serialisation.
     *
     * @param <T>
     *            type of the elements in the BVH.
     * @param elementsMap
     *            to resolve references during deserialisation.
     * @param objectCache
     *            to be built during deserialisation.
     * @return The type adapters in a list.
     */
    private static <T extends IBoundingBox & Referenceable> List<Pair<Class<?>, Object>> generateBaseTypeAdapters(
            final Map<UUID, T> elementsMap, final Map<T, BvhNode<T>> objectCache) {
        return Arrays.asList(
                new Pair<>(BoundingVolumeHierarchy.class, new BvhSerialiser<>()),
                new Pair<>(BvhNode.class, new BvhNode.NodeSerialiser<>(elementsMap, objectCache)),
                new Pair<>(UUID.class, new UuidSerialiser()),
                new Pair<>(Vec3.class, new PointSerialiser()));
    }

    /**
     * Applies a list of type adapters to a {@link GsonBuilder} and returns the updated builder object.
     *
     * @param base
     *            builder to add the type adapters to.
     * @param typeAdapters
     *            to be added to the builder.
     * @return The updated builder.
     */
    private static GsonBuilder registerTypeAdapters(
            final GsonBuilder base, final List<Pair<Class<?>, Object>> typeAdapters) {
        GsonBuilder result = base;
        for (final Pair<Class<?>, Object> adapter : typeAdapters) {
            result = result.registerTypeHierarchyAdapter(adapter.a, adapter.b);
        }
        return result;
    }

    /**
     * Serialises and saves the BVH.
     * <p>
     * It won't save if a rebuild is currently in progress, or if inserts are pending.
     * </p>
     *
     * @param file
     *            to save to.
     * @return Whether saving succeeded.
     */
    public boolean toJson(final File file) {
        synchronized (this.dataLock) {
            if (this.rebuildInProgress || !this.pendingInserts.isEmpty()) {
                return false;
            }
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder = registerTypeAdapters(gsonBuilder, generateBaseTypeAdapters(null, null));
            FileWriter writer;
            try {
                writer = new FileWriter(file);
            } catch (final IOException e) {
                e.printStackTrace();
                return false;
            }
            gsonBuilder.create().toJson(this, writer);
            return true;
        }
    }

    /**
     * Deserialises/loads a BVH from a file.
     *
     * @param <T>
     *            type of the 3D objects in the hierarchy.
     * @param file
     *            to load the BVH from.
     * @param elementsMap
     *            for resolving serialisation IDs.
     * @return The deserialised BVH, or {@code null} for IO exceptions or malformed JSON.
     */
    public static <T extends IBoundingBox & Referenceable> BoundingVolumeHierarchy<T> fromJson(
            final File file, final Map<UUID, T> elementsMap) {
        FileReader reader;
        try {
            reader = new FileReader(file);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        GsonBuilder gsonBuilder = new GsonBuilder();
        final Map<T, BvhNode<T>> objectCache = new HashMap<>();
        gsonBuilder = registerTypeAdapters(gsonBuilder, generateBaseTypeAdapters(elementsMap, objectCache));
        final Type type = new TypeToken<BoundingVolumeHierarchy<T>>() {}.getType();
        BoundingVolumeHierarchy<T> result;
        try {
            result = gsonBuilder.create().fromJson(reader, type);
        } catch (final Exception e) {
            /*-
             * Parsing the JSON can result in several different exceptions, which include:
             * - IllegalStateException (wrong type of a JsonElement)
             * - ArrayOutOfBoundsException (JsonArray too short)
             * - ClassCastException (wrong type of a JsonElement)
             */
            e.printStackTrace();
            return null;
        }
        result.objectCache = objectCache;
        return result;
    }

    /**
     * Serialisation type adapter for the {@link BoundingVolumeHierarchy} class.
     *
     * @author Kepler-17c
     *
     * @param <T>
     *            type of the elements in the BVH.
     */
    private static class BvhSerialiser<T extends IBoundingBox>
            implements JsonSerializer<BoundingVolumeHierarchy<T>>, JsonDeserializer<BoundingVolumeHierarchy<T>> {
        /**
         * JSON field name for maximum leaves on a branch.
         */
        private static final String LEAF_COUNT_FIELD = "leafCount";
        /**
         * JSON field name for the tree.
         */
        private static final String TREE_FIELD = "tree";
        /**
         * JSON field name for the split strategy.
         */
        private static final String STRATEGY_FACTORY_FIELD = "splitStrategy";
        /**
         * JSON field name for the split strategy settings.
         */
        private static final String STRATEGY_SETTINGS_FIELD = "splitSettings";

        @Override
        public BoundingVolumeHierarchy<T> deserialize(
                final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                throws JsonParseException {
            final BoundingVolumeHierarchy<T> result = new BoundingVolumeHierarchy<>();
            final JsonObject serialisedBvh = json.getAsJsonObject();
            result.leafCount = serialisedBvh.get(LEAF_COUNT_FIELD).getAsInt();
            result.tree = context.deserialize(serialisedBvh.get(TREE_FIELD), new TypeToken<BvhNode<T>>() {}.getType());
            result.splitStrategyFactory = StrategyFactory.fromString(
                    serialisedBvh.get(STRATEGY_FACTORY_FIELD).getAsString());
            result.splitStrategySettings =
                    serialisedBvh.get(STRATEGY_SETTINGS_FIELD).getAsJsonObject().entrySet().stream()
                            .collect(Collectors.toMap(
                                    Entry::getKey, e -> e.getValue().getAsString()));
            return result;
        }

        @Override
        public JsonElement serialize(
                final BoundingVolumeHierarchy<T> src, final Type typeOfSrc, final JsonSerializationContext context) {
            // serialise nodes
            final JsonElement tree = context.serialize(src.tree);
            // add variables
            final JsonElement serialisedLeafCount = new JsonPrimitive(src.leafCount);
            final JsonElement splitStrategyFactory = new JsonPrimitive(src.splitStrategyFactory.name());
            final JsonObject splitStrategySettings = new JsonObject();
            src.splitStrategySettings.entrySet().forEach(e -> {
                splitStrategySettings.add(e.getKey(), new JsonPrimitive(e.getValue()));
            });
            // put everything together
            final JsonObject serialisedBvh = new JsonObject();
            serialisedBvh.add(LEAF_COUNT_FIELD, serialisedLeafCount);
            serialisedBvh.add(STRATEGY_FACTORY_FIELD, splitStrategyFactory);
            serialisedBvh.add(STRATEGY_SETTINGS_FIELD, splitStrategySettings);
            serialisedBvh.add(TREE_FIELD, tree);
            return serialisedBvh;
        }
    }

    /**
     * Serialisation type adapter for the {@link UUID} class.
     *
     * @author Kepler-17c
     */
    private static class UuidSerialiser implements JsonSerializer<UUID>, JsonDeserializer<UUID> {
        @Override
        public UUID deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                throws JsonParseException {
            return UUID.fromString(json.getAsString());
        }

        @Override
        public JsonElement serialize(final UUID src, final Type typeOfSrc, final JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    /**
     * Serialisation type adapter for the {@link Vec3} class.
     *
     * @author Kepler-17c
     */
    private static class PointSerialiser implements JsonSerializer<Vec3>, JsonDeserializer<Vec3> {
        @Override
        public Vec3 deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                throws JsonParseException {
            final JsonArray coordinates = json.getAsJsonArray();
            if (coordinates.size() != 3) {
                throw new IllegalStateException(
                        "Wrong dimension: 3 ordinates expected, but " + coordinates.size() + "were given.");
            }
            final double x = coordinates.get(0).getAsDouble();
            final double y = coordinates.get(1).getAsDouble();
            final double z = coordinates.get(2).getAsDouble();
            return new Vec3(x, y, z);
        }

        @Override
        public JsonElement serialize(final Vec3 src, final Type typeOfSrc, final JsonSerializationContext context) {
            final JsonArray serialisedCoordinates = new JsonArray();
            serialisedCoordinates.add(src.x);
            serialisedCoordinates.add(src.y);
            serialisedCoordinates.add(src.z);
            return serialisedCoordinates;
        }
    }
}
