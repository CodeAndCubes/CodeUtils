package com.mrleonardos.codeutils.internal.broadcast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.apache.logging.log4j.Logger;

public final class Rotation {

    public static final int NOTHING = -1;

    public enum Order {

        CYCLE,
        RANDOM,
        SHUFFLE
    }

    private final Order order;
    private final Random random;
    private final List<Integer> bag = new ArrayList<>();

    private int pointer;

    public Rotation(Order order, Random random) {
        this.order = order;
        this.random = random;
    }

    public static Order order(String written, String where, Logger log) {
        String word = written == null ? ""
            : written.trim()
                .toLowerCase(Locale.ROOT);
        if (BroadcastsFile.ORDER_RANDOM.equals(word)) {
            return Order.RANDOM;
        }
        if (BroadcastsFile.ORDER_SHUFFLE.equals(word)) {
            return Order.SHUFFLE;
        }
        if (!BroadcastsFile.ORDER_CYCLE.equals(word)) {
            log.warn("{}: order = {} is not one of cycle, random, shuffle, cycle is used", where, written);
        }
        return Order.CYCLE;
    }

    public Order order() {
        return order;
    }

    public int pointer() {
        return pointer;
    }

    public int next(int size) {
        if (size <= 0) {
            return NOTHING;
        }
        switch (order) {
            case RANDOM:
                return random.nextInt(size);
            case SHUFFLE:
                return fromBag(size);
            default:
                return fromCycle(size);
        }
    }

    public void skip(int size) {
        if (size <= 0) {
            return;
        }
        if (order == Order.CYCLE) {
            pointer = (pointer + 1) % size;
            return;
        }
        if (order == Order.SHUFFLE) {
            fromBag(size);
        }
    }

    private int fromCycle(int size) {
        if (pointer >= size) {
            pointer = 0;
        }
        int chosen = pointer;
        pointer = (pointer + 1) % size;
        return chosen;
    }

    private int fromBag(int size) {
        if (bag.size() != size || pointer >= bag.size()) {
            refill(size);
        }
        return bag.get(pointer++);
    }

    private void refill(int size) {
        bag.clear();
        for (int index = 0; index < size; index++) {
            bag.add(index);
        }
        Collections.shuffle(bag, random);
        pointer = 0;
    }
}
