/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.objects.bvh;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.wynntils.utils.objects.IBoundingBox;
import com.wynntils.utils.objects.Referenceable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.Stack;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.world.phys.Vec3;

/**
 * Single node of the internal tree structure.
 * <p>
 * Each node hold references to it's parent, child-nodes and leaves.
 * </p>
 *
 * @param <T>
 *            type of the tree's leaves.
 */
class BvhNode<T extends IBoundingBox> implements IBoundingBox, Iterable<T> {

    /**
     * Parent node - or {@code null} for the root.
     */
    private BvhNode<T> parent;
    /**
     * A list of all child-nodes. Never {@code null}.
     */
    private final Set<BvhNode<T>> childNodes;
    /**
     * A list of all leaves. Never {@code null}.
     */
    private final Set<T> leaves;
    /**
     * Bounds over all elements under this node.
     */
    private AxisAlignedBoundingBox bounds;
    /**
     * Sum of all leaves in this node or child-nodes.
     */
    private int leafCount;

    /**
     * Root-like ({@code parent == null}), empty node.
     */
    public BvhNode() {
        this(Collections.emptyList());
    }

    /**
     * Root-like ({@code parent == null}) node, preinitialised with leaves.
     *
     * @param elements
     */
    public BvhNode(final Collection<T> elements) {
        this.parent = null;
        this.childNodes = new HashSet<>();
        this.leaves = new HashSet<>(elements);
        this.updateOwnBounds();
        this.updateLeafCount();
    }

    /**
     * Updates the number of leaves in this node and its child nodes.
     * <p>
     * This is <b>not</b> a recursive call and only updates this node.
     * </p>
     */
    private void updateLeafCount() {
        this.leafCount =
                this.childNodes.stream().mapToInt(BvhNode::getLeafCount).sum() + this.leaves.size();
    }

    /**
     * Gets the number of leaves under this node.
     *
     * @return the number of leaves.
     */
    public int getLeafCount() {
        return this.leafCount;
    }

    /**
     * Adds a child node.
     *
     * @param child
     *            to add.
     */
    public void addChildNode(final BvhNode<T> child) {
        this.childNodes.add(child);
        this.updateLeafCount();
    }

    /**
     * Gets an unmodifiable view on the child-nodes.
     *
     * @return the child-nodes.
     */
    public Set<BvhNode<T>> getChildNodes() {
        return Collections.unmodifiableSet(this.childNodes);
    }

    /**
     * Adds a new leaf to this node.
     *
     * @param leaf
     *            to add.
     */
    public void addLeaf(final T leaf) {
        this.leaves.add(leaf);
        this.updateLeafCount();
    }

    /**
     * Gets an unmodifiable view on the leaves.
     *
     * @return the leaves.
     */
    public Set<T> getLeaves() {
        return Collections.unmodifiableSet(this.leaves);
    }

    /**
     * Clears all leaves from this node.
     */
    public void clearLeaves() {
        this.leaves.clear();
        this.updateLeafCount();
        this.updateBounds();
    }

    /**
     * Sets a new parent.
     *
     * @param parent
     *            for this node.
     */
    public void setParent(final BvhNode<T> parent) {
        this.parent = parent;
    }

    /**
     * Recurses through the child-tree to update it's bounds and propagates it up to all parents.
     *
     * This call is equivalent to calling {@link #updateBoundsRecursive()} and {@link #propagateBoundsUpwards()} in this
     * order.
     */
    public void updateBounds() {
        this.updateBoundsRecursive();
        this.propagateBoundsUpwards();
    }

    /**
     * Combines bounds of all child-nodes and leaves to update this node's bounds.
     */
    public void updateOwnBounds() {
        this.bounds = AxisAlignedBoundingBox.mergeBounds(
                this.childNodes.stream()
                        .map(IBoundingBox::getBounds)
                        .reduce(new AxisAlignedBoundingBox(), AxisAlignedBoundingBox::mergeBounds),
                this.leaves.stream()
                        .map(IBoundingBox::getBounds)
                        .reduce(new AxisAlignedBoundingBox(), AxisAlignedBoundingBox::mergeBounds));
    }

    /**
     * Depth-first tree traversal to call {@link #updateOwnBounds()} on every node, starting at the bottom of the tree.
     */
    public void updateBoundsRecursive() {
        this.childNodes.forEach(BvhNode::updateBoundsRecursive);
        this.updateOwnBounds();
    }

    /**
     * Propagates bounds changes through all parent nodes to the top.
     *
     * The call is equivalent to calling {@link #updateOwnBounds()} on every parent in succession.
     */
    public void propagateBoundsUpwards() {
        if (this.parent != null) {
            this.parent.updateOwnBounds();
            this.parent.propagateBoundsUpwards();
        }
    }

    @Override
    public AxisAlignedBoundingBox getBounds() {
        return this.bounds;
    }

    @Override
    public Iterator<T> iterator() {
        return new TreeIterator<>(this);
    }

    @Override
    public Spliterator<T> spliterator() {
        return new TreeSpliterator<>(this);
    }

    /**
     * Iterator for the internal BVH tree.
     *
     * @param <E>
     *            type of the elements in this iterator.
     */
    private static class TreeIterator<E extends IBoundingBox> implements Iterator<E> {
        /**
         * Elements encountered while traversing the tree.
         */
        private final Queue<E> queuedElements;
        /**
         * Nodes waiting to be processed.
         */
        private final Stack<BvhNode<E>> nodeStack;
        /**
         * The element to be returned on {@link #next()}.
         */
        private E nextElement;

        /**
         * Start the iterator on the (sub-)tree of a node.
         *
         * @param startNode
         *            where the iterator should start traversing.
         */
        public TreeIterator(final BvhNode<E> startNode) {
            this.queuedElements = new LinkedList<>();
            this.nodeStack = new Stack<>();
            this.nodeStack.add(startNode);
            this.loadNextElement();
        }

        /**
         * Traverse the tree and load the next element to {@link #nextElement}.
         */
        private void loadNextElement() {
            while (this.queuedElements.isEmpty() && !this.nodeStack.isEmpty()) {
                final BvhNode<E> activeNode = this.nodeStack.pop();
                this.queuedElements.addAll(activeNode.leaves);
                this.nodeStack.addAll(activeNode.childNodes);
            }
            this.nextElement = this.queuedElements.isEmpty() ? null : this.queuedElements.poll();
        }

        @Override
        public boolean hasNext() {
            return this.nextElement != null;
        }

        @Override
        public E next() {
            final E result = this.nextElement;
            this.loadNextElement();
            return result;
        }
    }

    /**
     * Spliterator for traversal of a {@link BvhNode} tree.
     *
     * @author Kepler-17c
     *
     * @param <E>
     *            type of the elements in the tree.
     */
    private static class TreeSpliterator<E extends IBoundingBox> implements Spliterator<E> {
        /**
         * Elements from processed nodes.
         */
        private final Queue<E> queuedElements;
        /**
         * Nodes to be processed. Uses a stack for depth-first traversal.
         */
        private final Stack<BvhNode<E>> nodeStack;

        /**
         * Creates the spliterator from a root node.
         *
         * @param root
         *            to start traversal at.
         */
        public TreeSpliterator(final BvhNode<E> root) {
            this.nodeStack = new Stack<>();
            this.nodeStack.add(root);
            this.queuedElements = new LinkedList<>();
        }

        /**
         * Internal constructor for {@link #trySplit()}.
         *
         * @param nodes
         *            for this split.
         */
        private TreeSpliterator(final Collection<BvhNode<E>> nodes) {
            this.nodeStack = new Stack<>();
            this.nodeStack.addAll(nodes);
            this.queuedElements = new LinkedList<>();
        }

        @Override
        public boolean tryAdvance(final Consumer<? super E> action) {
            while (this.queuedElements.isEmpty() && !this.nodeStack.isEmpty()) {
                final BvhNode<E> node = this.nodeStack.pop();
                this.nodeStack.addAll(node.childNodes);
                this.queuedElements.addAll(node.leaves);
            }
            if (this.queuedElements.isEmpty()) {
                return false;
            }
            action.accept(this.queuedElements.poll());
            return true;
        }

        @Override
        public Spliterator<E> trySplit() {
            while (this.nodeStack.size() < 2 && !this.nodeStack.isEmpty()) {
                final BvhNode<E> node = this.nodeStack.pop();
                node.childNodes.forEach(this.nodeStack::add);
                node.leaves.forEach(this.queuedElements::add);
            }
            if (this.nodeStack.size() < 2) {
                return null;
            }
            final int splitSize = this.nodeStack.size() / 2;
            final List<BvhNode<E>> nodes = new ArrayList<>();
            for (int i = 0; i < splitSize; i++) {
                nodes.add(this.nodeStack.pop());
            }
            return new TreeSpliterator<>(nodes);
        }

        @Override
        public long estimateSize() {
            return this.nodeStack.stream().mapToInt(n -> n.leafCount).sum() + this.queuedElements.size();
        }

        @Override
        public int characteristics() {
            return DISTINCT | SIZED | NONNULL | IMMUTABLE | CONCURRENT | SUBSIZED;
        }
    }

    /**
     * Serialisation type adapter for the {@link BvhNode} class.
     *
     * @author Kepler-17c
     *
     * @param <T>
     *            type of the elements in the BVH.
     */
    static class NodeSerialiser<T extends IBoundingBox & Referenceable>
            implements JsonSerializer<BvhNode<T>>, JsonDeserializer<BvhNode<T>> {
        /**
         * JSON field name for the child nodes.
         */
        private static final String CHILD_NODES_FIELD = "childNodes";
        /**
         * JSON field name for leaves on the node.
         */
        private static final String LEAVES_FIELD = "leaves";
        /**
         * JSON field name for the contained elements' bounds.
         */
        private static final String BOUNDS_FIELD = "bounds";

        /**
         * Deserialisation lookup to resolve UUID references.
         */
        private final Map<UUID, T> elementsMap;
        /**
         * Object cache to be built during deserialisation.
         */
        private final Map<T, BvhNode<T>> objectCache;

        /**
         * Creates an adapter from references to deserialisation objects.
         *
         * @param elementsMap
         *            lookup for UUID references.
         * @param objectCache
         *            to be build during deserialisation.
         */
        public NodeSerialiser(final Map<UUID, T> elementsMap, final Map<T, BvhNode<T>> objectCache) {
            this.elementsMap = elementsMap;
            this.objectCache = objectCache;
        }

        @Override
        public BvhNode<T> deserialize(
                final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                throws JsonParseException {
            final BvhNode<T> deserialisedNode = new BvhNode<>();
            final JsonArray childNodes = json.getAsJsonObject().getAsJsonArray(CHILD_NODES_FIELD);
            childNodes.forEach(node ->
                    deserialisedNode.addChildNode(context.deserialize(node, new TypeToken<BvhNode<T>>() {}.getType())));
            final JsonArray leafIds = json.getAsJsonObject().getAsJsonArray(LEAVES_FIELD);
            leafIds.forEach(id -> {
                final T leaf = this.elementsMap.get(context.deserialize(id, new TypeToken<UUID>() {}.getType()));
                if (leaf != null) {
                    deserialisedNode.addLeaf(leaf);
                    this.objectCache.put(leaf, deserialisedNode);
                }
            });
            final JsonArray bounds = json.getAsJsonObject().getAsJsonArray(BOUNDS_FIELD);
            deserialisedNode.getBounds().add(context.deserialize(bounds.get(0), new TypeToken<Vec3>() {}.getType()));
            deserialisedNode.getBounds().add(context.deserialize(bounds.get(1), new TypeToken<Vec3>() {}.getType()));
            return deserialisedNode;
        }

        @Override
        public JsonElement serialize(
                final BvhNode<T> src, final Type typeOfSrc, final JsonSerializationContext context) {
            final JsonObject serialisedNode = new JsonObject();
            // serialise child nodes
            final JsonArray serialisedChildNodes = new JsonArray();
            src.getChildNodes().forEach(n -> serialisedChildNodes.add(context.serialize(n)));
            // serialise leaves
            final JsonArray serialisedLeaves = new JsonArray();
            src.getLeaves().forEach(l -> serialisedLeaves.add(context.serialize(l.getUuid())));
            // serialise bounds
            final JsonArray serialisedBounds = new JsonArray();
            serialisedBounds.add(context.serialize(src.getBounds().getLower()));
            serialisedBounds.add(context.serialize(src.getBounds().getUpper()));
            // put all together
            serialisedNode.add(CHILD_NODES_FIELD, serialisedChildNodes);
            serialisedNode.add(LEAVES_FIELD, serialisedLeaves);
            serialisedNode.add(BOUNDS_FIELD, serialisedBounds);
            return serialisedNode;
        }
    }
}
