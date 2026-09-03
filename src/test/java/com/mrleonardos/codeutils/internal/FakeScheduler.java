package com.mrleonardos.codeutils.internal;

import java.util.ArrayList;
import java.util.List;

import com.mrleonardos.codecore.api.util.Scheduler;

/** Планировщик, который ничего не откладывает, а держит очередь и запускает её по требованию теста. */
public final class FakeScheduler implements Scheduler {

    private final List<Runnable> queue = new ArrayList<>();

    private int lastDelay = -1;

    @Override
    public void onMainThread(Runnable task) {
        queue.add(task);
    }

    @Override
    public void afterTicks(int ticks, Runnable task) {
        lastDelay = ticks;
        queue.add(task);
    }

    public int lastDelay() {
        return lastDelay;
    }

    public int pending() {
        return queue.size();
    }

    /** Выполнить последний поставленный вызов: так очередь не копит хвосты прежних поколений. */
    public void runNext() {
        if (queue.isEmpty()) {
            return;
        }
        queue.remove(queue.size() - 1)
            .run();
    }

    /** Выполнить самый старый вызов очереди. */
    public void runFirst() {
        if (queue.isEmpty()) {
            return;
        }
        queue.remove(0)
            .run();
    }
}
