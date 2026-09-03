package com.mrleonardos.codeutils.internal.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.internal.Clocks;

class JobScheduleTest {

    private static final Logger LOG = LogManager.getLogger("codeutils-test");

    private static final ZoneId ZONE = ZoneId.of("UTC");

    private static final long SECOND = 1_000L;

    @Test
    void aMomentInsideTheGapBetweenTwoClockReadingsFiresOnce() {
        JobSchedule schedule = schedule(0, "04:00");

        assertEquals(1, schedule.due(at("2026-09-03T03:59:50"), at("2026-09-03T04:00:30")));
    }

    @Test
    void theSameMomentDoesNotFireTwiceOnTheNextReading() {
        JobSchedule schedule = schedule(0, "04:00");

        assertEquals(1, schedule.due(at("2026-09-03T03:59:50"), at("2026-09-03T04:00:30")));
        assertEquals(0, schedule.due(at("2026-09-03T04:00:30"), at("2026-09-03T04:01:30")));
    }

    @Test
    void twoMomentsInsideOneJumpBothFire() {
        JobSchedule schedule = schedule(0, "04:00", "04:02");

        assertEquals(2, schedule.due(at("2026-09-03T03:59:50"), at("2026-09-03T04:02:30")));
    }

    @Test
    void aMomentOutsideTheGapDoesNotFire() {
        JobSchedule schedule = schedule(0, "04:00");

        assertEquals(0, schedule.due(at("2026-09-03T04:00:30"), at("2026-09-03T04:01:00")));
        assertEquals(0, schedule.due(at("2026-09-03T03:58:00"), at("2026-09-03T03:59:00")));
    }

    @Test
    void aMomentIsCaughtWhenTheGapCrossesMidnight() {
        JobSchedule schedule = schedule(0, "00:05");

        assertEquals(1, schedule.due(at("2026-09-03T23:59:00"), at("2026-09-04T00:06:00")));
    }

    @Test
    void theIntervalFiresOnceItsDeadlineIsPassed() {
        JobSchedule schedule = schedule(60);
        long start = at("2026-09-03T04:00:00");
        schedule.arm(start);

        assertEquals(0, schedule.due(start, start + 59 * SECOND));
        assertEquals(1, schedule.due(start + 59 * SECOND, start + 60 * SECOND));
        assertEquals(0, schedule.due(start + 60 * SECOND, start + 119 * SECOND));
        assertEquals(1, schedule.due(start + 119 * SECOND, start + 120 * SECOND));
    }

    @Test
    void aStuckServerGetsOneIntervalShotAndNotAWholeBurst() {
        JobSchedule schedule = schedule(60);
        long start = at("2026-09-03T04:00:00");
        schedule.arm(start);

        assertEquals(1, schedule.due(start, start + 600 * SECOND), "десять минут простоя это одна команда, не десять");
        assertEquals(0, schedule.due(start + 600 * SECOND, start + 659 * SECOND), "дедлайн отсчитан от нового времени");
        assertEquals(1, schedule.due(start + 659 * SECOND, start + 660 * SECOND));
    }

    @Test
    void anIntervalAndAMomentWorkTogether() {
        JobSchedule schedule = schedule(3600, "04:00");
        long start = at("2026-09-03T03:59:00");
        schedule.arm(start);

        assertEquals(1, schedule.due(start, at("2026-09-03T04:00:30")), "сработал момент");
        assertEquals(1, schedule.due(at("2026-09-03T04:58:30"), at("2026-09-03T04:59:30")), "сработал интервал");
    }

    @Test
    void aScheduleWithoutAnythingIsIdle() {
        assertTrue(schedule(0).idle());
        assertFalse(schedule(60).idle());
        assertFalse(schedule(0, "04:00").idle());
    }

    @Test
    void momentsAreSortedAndTheirRepeatsAreDropped() {
        JobSchedule schedule = schedule(0, "16:00", "04:00", "04:00");

        assertEquals(Arrays.asList(Integer.valueOf(4 * 60), Integer.valueOf(16 * 60)), schedule.moments());
    }

    @Test
    void aMomentThatIsNotAMomentIsSkipped() {
        JobSchedule schedule = schedule(0, "четыре утра", "04:00", "25:00");

        assertEquals(Collections.singletonList(Integer.valueOf(4 * 60)), schedule.moments());
    }

    @Test
    void theNearestMomentIsTheOneAheadOfNow() {
        JobSchedule schedule = schedule(0, "04:00", "16:00");

        assertEquals(at("2026-09-03T16:00:00"), schedule.nextMoment(at("2026-09-03T05:00:00")));
        assertEquals(
            at("2026-09-04T04:00:00"),
            schedule.nextMoment(at("2026-09-03T17:00:00")),
            "после последнего момента дня идёт первый момент завтрашнего");
    }

    @Test
    void theIntervalCountsAsAMomentWhenItComesEarlier() {
        JobSchedule schedule = schedule(60, "16:00");
        long start = at("2026-09-03T05:00:00");
        schedule.arm(start);

        assertEquals(start + 60 * SECOND, schedule.nextMoment(start));
    }

    private static JobSchedule schedule(int everySeconds, String... at) {
        List<String> moments = Arrays.asList(at);
        return JobSchedule.of(moments, everySeconds, "тест", LOG, () -> ZONE);
    }

    private static long at(String time) {
        return Clocks.millisOf(LocalDateTime.parse(time), ZONE);
    }
}
