package com.mrleonardos.codeutils.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mrleonardos.codecore.api.util.Scheduler;

class TickerTest {

    @Test
    void theTickArmsItselfAgainAfterEveryBeat() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeClock clock = new FakeClock(1_000L);
        List<long[]> beats = new ArrayList<>();
        Ticker ticker = new Ticker(scheduler, clock, (from, to) -> beats.add(new long[] { from, to }));

        ticker.start();
        clock.at(2_000L);
        scheduler.runNext();
        clock.at(3_000L);
        scheduler.runNext();

        assertEquals(2, beats.size());
        assertArray(new long[] { 1_000L, 2_000L }, beats.get(0));
        assertArray(new long[] { 2_000L, 3_000L }, beats.get(1));
        assertEquals(1, scheduler.pending(), "после каждого такта в очереди снова ровно один вызов");
    }

    @Test
    void theTickIsArmedForOneSecond() {
        FakeScheduler scheduler = new FakeScheduler();
        Ticker ticker = new Ticker(scheduler, new FakeClock(0L), (from, to) -> {});

        ticker.start();

        assertEquals(Ticker.TICKS_PER_SECOND, scheduler.lastDelay());
    }

    @Test
    void stopSilencesTheCallLeftInTheQueue() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeClock clock = new FakeClock(1_000L);
        List<long[]> beats = new ArrayList<>();
        Ticker ticker = new Ticker(scheduler, clock, (from, to) -> beats.add(new long[] { from, to }));

        ticker.start();
        ticker.stop();
        clock.at(2_000L);
        scheduler.runNext();

        assertTrue(beats.isEmpty(), "отложенный вызов прежнего поколения ничего не делает");
        assertFalse(ticker.running());
        assertEquals(0, scheduler.pending(), "погашенный такт себя не переармирует");
    }

    @Test
    void aRestartedTickIgnoresTheCallOfTheOldGeneration() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeClock clock = new FakeClock(1_000L);
        List<long[]> beats = new ArrayList<>();
        Ticker ticker = new Ticker(scheduler, clock, (from, to) -> beats.add(new long[] { from, to }));

        ticker.start();
        ticker.stop();
        clock.at(5_000L);
        ticker.start();
        scheduler.runFirst();

        assertTrue(beats.isEmpty(), "первым в очереди лежит вызов прежнего поколения");

        clock.at(6_000L);
        scheduler.runNext();

        assertEquals(1, beats.size());
        assertArray(new long[] { 5_000L, 6_000L }, beats.get(0));
    }

    @Test
    void aClockThatDidNotMoveDoesNotWakeTheSubsystems() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeClock clock = new FakeClock(1_000L);
        List<long[]> beats = new ArrayList<>();
        Ticker ticker = new Ticker(scheduler, clock, (from, to) -> beats.add(new long[] { from, to }));

        ticker.start();
        scheduler.runNext();

        assertTrue(beats.isEmpty());
        assertEquals(1, scheduler.pending(), "такт всё равно ставит себя заново");
    }

    @Test
    void aStuckServerGetsOneBeatWithTheWholeGap() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeClock clock = new FakeClock(1_000L);
        List<long[]> beats = new ArrayList<>();
        Ticker ticker = new Ticker(scheduler, clock, (from, to) -> beats.add(new long[] { from, to }));

        ticker.start();
        clock.at(11_000L);
        scheduler.runNext();

        assertEquals(1, beats.size());
        assertArray(new long[] { 1_000L, 11_000L }, beats.get(0));
    }

    private static void assertArray(long[] expected, long[] actual) {
        assertEquals(expected[0], actual[0], "начало окна");
        assertEquals(expected[1], actual[1], "конец окна");
    }

    static final class FakeClock implements java.util.function.LongSupplier {

        private long millis;

        FakeClock(long millis) {
            this.millis = millis;
        }

        void at(long value) {
            millis = value;
        }

        @Override
        public long getAsLong() {
            return millis;
        }
    }

    static final class FakeScheduler implements Scheduler {

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

        int lastDelay() {
            return lastDelay;
        }

        int pending() {
            return queue.size();
        }

        /** Выполнить последний поставленный вызов: так очередь не копит хвосты прежних поколений. */
        void runNext() {
            if (queue.isEmpty()) {
                return;
            }
            queue.remove(queue.size() - 1)
                .run();
        }

        /** Выполнить самый старый вызов очереди. */
        void runFirst() {
            if (queue.isEmpty()) {
                return;
            }
            queue.remove(0)
                .run();
        }
    }
}
