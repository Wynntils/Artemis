/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.objects.minQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * A priority queue utilising the Fibonacci heap.
 *
 * @author Kepler-17c
 *
 * @param <T>
 *            type of the elements in this collection.
 */
public class FibonacciHeapMinQueue<T> implements Queue<T> {
    /**
     * Sorting function for the queue order.
     */
    private final Comparator<T> comparator;
    /**
     * The Fibonacci heap.
     */
    private final List<Tree<T>> heap;
    /**
     * Position of the heap element with the smallest element.
     */
    private int minIndex;
    /**
     * Reference for finding elements in the heap quickly.
     */
    private final Map<T, Tree<T>> objectCache;

    /**
     * Creates an empty queue on the given comparator.
     *
     * @param comparator
     *            defining the queue order.
     */
    public FibonacciHeapMinQueue(final Comparator<T> comparator) {
        this.comparator = comparator;
        this.heap = new ArrayList<>();
        this.minIndex = -1;
        this.objectCache = new HashMap<>();
    }

    @Override
    public boolean add(final T value) {
        if (value == null || this.objectCache.containsKey(value)) {
            return false;
        }
        final Tree<T> heapElement = new Tree<>(value);
        this.insertAndClean(heapElement);
        this.objectCache.put(value, heapElement);
        return true;
    }

    @Override
    public boolean isEmpty() {
        return this.heap.isEmpty()
                || this.heap.stream().filter(Objects::nonNull).count() == 0;
    }

    /**
     * Finds the smallest heap entry and sets {@code #minIndex} accordingly ({@code -1} when the heap is empty).
     */
    private void updateMinIndex() {
        final Optional<Tree<T>> min = this.heap.stream()
                .filter(Objects::nonNull)
                .sorted((a, b) -> this.comparator.compare(a.value, b.value))
                .findFirst();
        this.minIndex = min.isPresent() ? this.heap.indexOf(min.get()) : -1;
    }

    /**
     * Inserts a node in the heap and balances the trees.
     *
     * @param entry
     *            to be inserted.
     */
    private void insertAndClean(Tree<T> entry) {
        if (this.minIndex == -1 || this.heap.get(this.minIndex) == null) { // min got removed, find new min
            this.updateMinIndex();
        }
        boolean updateMinIndex = this.isEmpty() // new element must be min
                || this.comparator.compare(entry.value, this.heap.get(this.minIndex).value) < 0;
        // iterate add operation
        while (true) {
            // detect possible collision
            final int degree = entry.getDegree();
            while (this.heap.size() <= degree) { // ensure heap size
                this.heap.add(null);
            }
            final Tree<T> oldEntry = this.heap.get(degree);
            // insert in heap
            if (oldEntry == null) {
                this.heap.set(degree, entry);
                if (updateMinIndex) {
                    this.minIndex = degree;
                }
                break;
            } else {
                entry = Tree.mergeTrees(entry, oldEntry, this.comparator);
                this.heap.set(degree, null);
                if (this.minIndex == degree) {
                    updateMinIndex = true;
                }
            }
        }
    }

    /**
     * Creates a list of all elements in this queue.
     *
     * @return The list of all elements.
     */
    private List<T> listCopy() {
        final List<T> result = this.heap.stream()
                .filter(Objects::nonNull)
                .map(Tree::listCopy)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        result.sort(this.comparator);
        return result;
    }

    @Override
    public int size() {
        return this.heap.stream().filter(Objects::nonNull).mapToInt(Tree::size).sum();
    }

    @Override
    public boolean contains(final Object o) {
        return this.objectCache.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        final List<T> listCopy = this.listCopy();
        return listCopy.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.listCopy().toArray();
    }

    @Override
    public <E> E[] toArray(final E[] a) {
        return this.listCopy().toArray(a);
    }

    @Override
    public boolean remove(final Object o) {
        final Tree<T> value = this.objectCache.remove(o);
        if (value == null) {
            return false;
        }
        final List<Tree<T>> nodesToReInsert = new ArrayList<>();
        nodesToReInsert.addAll(value.childNodes);
        if (value.parent == null) {
            this.heap.set(this.heap.indexOf(value), null);
        } else {
            Tree<T> activeNode = value.parent;
            activeNode.childNodes.remove(value);
            while (activeNode != null) {
                if (activeNode.parent == null) {
                    nodesToReInsert.add(activeNode);
                    this.heap.set(this.heap.indexOf(activeNode), null);
                    activeNode = null;
                } else if (activeNode.markedRemoved) {
                    final Tree<T> parent = activeNode.parent;
                    parent.childNodes.remove(activeNode);
                    activeNode.parent = null;
                    nodesToReInsert.add(activeNode);
                    activeNode = parent;
                } else {
                    activeNode.markedRemoved = true;
                    activeNode = null;
                }
            }
        }
        nodesToReInsert.forEach(n -> n.markedRemoved = false);
        nodesToReInsert.forEach(n -> n.parent = null);
        nodesToReInsert.forEach(this::insertAndClean);
        if (this.heap.get(this.minIndex) == null) {
            this.updateMinIndex();
        }
        return true;
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return c.stream().map(e -> this.objectCache.containsKey(e)).reduce(true, Boolean::logicalAnd);
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
        return c.stream().map(this::add).reduce(false, Boolean::logicalOr);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return c.stream().map(this::remove).reduce(false, Boolean::logicalOr);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        final List<T> heapContents = this.listCopy();
        heapContents.retainAll(c);
        if (heapContents.size() != this.size()) {
            this.heap.clear();
            this.addAll(heapContents);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        this.heap.clear();
        this.objectCache.clear();
    }

    @Override
    public boolean offer(final T e) {
        return this.add(e);
    }

    @Override
    public T remove() {
        final T polled = this.poll();
        if (polled == null) {
            throw new NoSuchElementException("Queue is empty.");
        }
        return polled;
    }

    @Override
    public T poll() {
        if (this.isEmpty()) {
            return null;
        }
        // take min from heap
        final Tree<T> minTree = this.heap.set(this.minIndex, null);
        // re-insert child nodes
        minTree.childNodes.forEach(entry -> {
            entry.parent = null;
            this.insertAndClean(entry);
        });
        // shrink over-grown heap
        int last = this.heap.size() - 1;
        if (!this.heap.isEmpty() && this.heap.get(last) == null) {
            while (last > 0 && this.heap.get(last - 1) == null) {
                this.heap.remove(last--);
            }
        }
        this.updateMinIndex();
        this.objectCache.remove(minTree.value, minTree);
        return minTree.value;
    }

    @Override
    public T element() {
        final T peeked = this.peek();
        if (peeked == null) {
            throw new NoSuchElementException("Queue is empty.");
        }
        return peeked;
    }

    @Override
    public T peek() {
        return this.isEmpty() ? null : this.heap.get(this.minIndex).value;
    }

    @Override
    public String toString() {
        return this.heap.stream()
                .map(t -> t == null ? "<>" : t.toString())
                .collect(Collectors.joining("\n", "{\n", "\n}"));
    }
}
