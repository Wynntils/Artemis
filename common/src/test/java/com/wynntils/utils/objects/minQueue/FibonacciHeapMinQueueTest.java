/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.objects.minQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FibonacciHeapMinQueueTest {

    @Test
    void testConstructor() {
        final Queue<Integer> queue = new FibonacciHeapMinQueue<>(Integer::compareTo);
        Assertions.assertTrue(queue.isEmpty());
    }

    @Test
    void testAdd() {
        final Queue<Integer> queue = new FibonacciHeapMinQueue<>(Integer::compareTo);
        final Set<Integer> refSet = new HashSet<>();
        final Random random = new Random("fibonacci".hashCode());
        final int size = 1 << 16;
        for (int i = 0; i < size; i++) {
            final int tmp = random.nextInt();
            queue.add(tmp);
            refSet.add(tmp);
        }
        final List<Integer> refList = refSet.stream().sorted().collect(Collectors.toList());
        for (final Integer i : refList) {
            Assertions.assertEquals(i, queue.poll());
        }
    }

    @Test
    void testIsEmpty() {
        final Queue<Integer> queue = new FibonacciHeapMinQueue<>(Integer::compareTo);
        Assertions.assertTrue(queue.isEmpty());
        queue.add(0);
        Assertions.assertFalse(queue.isEmpty());
        queue.remove(0);
        Assertions.assertTrue(queue.isEmpty());
    }

    @Test
    void testAddRemoveContainsSize() {
        final Queue<Integer> queue = new FibonacciHeapMinQueue<>(Integer::compareTo);
        final Set<Integer> reference = new HashSet<>();
        final Random random = new Random("Detlas".hashCode());
        final int probes = 1 << 10;
        final int removalWeight = 1 << 3;
        final int range = 1 << 4;
        for (int i = 0; i < probes; i++) {
            final Integer value = random.nextInt(range);
            final boolean remove = random.nextInt(removalWeight) == 0;
            if (remove) {
                queue.remove(value);
                reference.remove(value);
            } else {
                queue.add(value);
                reference.add(value);
            }
            Assertions.assertEquals(reference.contains(value), queue.contains(value));
            Assertions.assertTrue(reference.containsAll(queue));
            Assertions.assertTrue(queue.containsAll(reference));
            Assertions.assertEquals(reference.stream().sorted().findFirst().orElse(null), queue.peek());
            Assertions.assertEquals(reference.size(), queue.size());
        }
    }

    @Test
    void testIterator() {
        final Random random = new Random("Cinfras".hashCode());
        final Queue<Integer> queue = new FibonacciHeapMinQueue<>(Integer::compareTo);
        final int size = 1 << 16;
        for (int i = 0; i < size; i++) {
            queue.add(random.nextInt());
        }
        final Iterator<Integer> iterator = queue.iterator();
        Integer last = iterator.next();
        Integer current;
        while (iterator.hasNext()) {
            Assertions.assertTrue(last.compareTo(current = iterator.next()) < 0);
            last = current;
        }
    }

    @Test
    void testAddNull() {
        final Queue<Integer> queue = new FibonacciHeapMinQueue<>(Integer::compareTo);
        Assertions.assertFalse(queue.add(null));
        Assertions.assertTrue(queue.add(0));
    }

    @Test
    void testToArray() {
        final Queue<Integer> queue = new FibonacciHeapMinQueue<>(Integer::compareTo);
        final Random random = new Random("Almuj".hashCode());
        final int size = 1 << 8;
        for (int i = 0; i < size; i++) {
            queue.add(random.nextInt());
        }
        final Object[] arr = queue.toArray();
        final Object[] sorted = Arrays.copyOf(arr, arr.length);
        Arrays.sort(sorted);
        Assertions.assertArrayEquals(sorted, arr);
    }

    @Test
    void testToArrayEArray() {
        final Queue<Integer> queue = new FibonacciHeapMinQueue<>(Integer::compareTo);
        final Random random = new Random("Rodoroc".hashCode());
        final int size = 1 << 8;
        for (int i = 0; i < size; i++) {
            queue.add(random.nextInt());
        }
        final Integer[] arr = queue.toArray(new Integer[0]);
        final Integer[] sorted = Arrays.copyOf(arr, arr.length);
        Arrays.sort(sorted);
        Assertions.assertArrayEquals(sorted, arr);
    }

    @Test
    void testContainsAll() {
        final Queue<Integer> queue = new FibonacciHeapMinQueue<>(Integer::compareTo);
        Assertions.assertTrue(queue.containsAll(Collections.emptySet()));
        queue.add(0);
        Assertions.assertFalse(queue.containsAll(Arrays.asList(0, 1)));
        Assertions.assertTrue(queue.containsAll(Arrays.asList(0)));
        queue.add(1);
        Assertions.assertTrue(queue.containsAll(Arrays.asList(0, 1)));
        Assertions.assertTrue(queue.containsAll(Arrays.asList(0)));
    }

    @Test
    void testAddAll() {
        final Queue<Integer> queue = new FibonacciHeapMinQueue<>(Integer::compareTo);
        final Random random = new Random("Ahmsord".hashCode());
        final int size = 1 << 16;
        final int bound = 1 << 8;
        final Set<Integer> reference = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            reference.add(random.nextInt(bound));
        }
        queue.addAll(reference);
        final List<Integer> refList = new ArrayList<>(size);
        refList.addAll(reference.stream().sorted().collect(Collectors.toList()));
        while (!queue.isEmpty()) {
            Assertions.assertEquals(refList.remove(0), queue.poll());
        }
        Assertions.assertTrue(refList.isEmpty());
    }
}
