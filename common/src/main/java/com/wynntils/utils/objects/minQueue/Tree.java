/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.objects.minQueue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

class Tree<T> {
    final List<Tree<T>> childNodes;
    final T value;
    Tree<T> parent;
    boolean markedRemoved;

    public Tree(final T value) {
        this.childNodes = new ArrayList<>();
        this.value = value;
        this.parent = null;
        this.markedRemoved = false;
    }

    public int getDegree() {
        return this.childNodes.size();
    }

    public void addChild(final Tree<T> child) {
        this.childNodes.add(child);
        child.parent = this;
    }

    public int size() {
        return 1 + this.childNodes.stream().collect(Collectors.summingInt(Tree::size));
    }

    public List<T> listCopy() {
        final List<T> result = new ArrayList<>();
        final Queue<Tree<T>> openNodes = new LinkedList<>();
        openNodes.add(this);
        while (!openNodes.isEmpty()) {
            final Tree<T> node = openNodes.poll();
            result.add(node.value);
            openNodes.addAll(node.childNodes);
        }
        return result;
    }

    @Override
    public String toString() {
        return "{" + this.value + ":"
                + this.childNodes.stream().map(Tree::toString).collect(Collectors.joining(", ", "[", "]")) + "}";
    }

    public static <T> Tree<T> mergeTrees(final Tree<T> a, final Tree<T> b, final Comparator<T> comparator) {
        if (comparator.compare(a.value, b.value) < 0) {
            a.addChild(b);
            return a;
        } else {
            b.addChild(a);
            return b;
        }
    }
}
