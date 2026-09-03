package com.mrleonardos.codeutils.internal;

import java.util.function.LongSupplier;

import com.mrleonardos.codecore.api.util.Scheduler;

public final class Ticker {

    public static final int TICKS_PER_SECOND = 20;

    private final Scheduler scheduler;
    private final LongSupplier clock;
    private final Beat beat;

    private int generation;
    private boolean running;
    private long previous;

    public Ticker(Scheduler scheduler, LongSupplier clock, Beat beat) {
        this.scheduler = scheduler;
        this.clock = clock;
        this.beat = beat;
    }

    public interface Beat {

        void tick(long from, long to);
    }

    public void start() {
        generation++;
        running = true;
        previous = clock.getAsLong();
        arm(generation);
    }

    public void stop() {
        generation++;
        running = false;
    }

    public boolean running() {
        return running;
    }

    public long previous() {
        return previous;
    }

    private void arm(int mine) {
        scheduler.afterTicks(TICKS_PER_SECOND, () -> tick(mine));
    }

    private void tick(int mine) {
        if (!running || mine != generation) {
            return;
        }
        long from = previous;
        long to = clock.getAsLong();
        previous = to;
        if (to > from) {
            beat.tick(from, to);
        }
        arm(mine);
    }
}
