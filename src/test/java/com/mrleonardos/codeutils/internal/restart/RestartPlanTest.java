package com.mrleonardos.codeutils.internal.restart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.restart.RestartReason;
import com.mrleonardos.codeutils.internal.WhenBlock;
import com.mrleonardos.codeutils.internal.command.UtilsMessages;

class RestartPlanTest extends RestartTests {

    @Test
    void everyWarningOfTheListGoesOutOnceAndAtItsOwnSecond() {
        warnAt(60, 30, 30);
        RestartPlan plan = plan(runner());
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start + 60 * SECOND, RestartReason.COMMAND);

        plan.tick(start - SECOND, start, snapshot("2026-09-03T05:00:00"));

        assertEquals(1, sink.sends());
        assertTrue(
            warnings().get(0)
                .contains(UtilsMessages.RESTART_WARN + "[1m]"),
            warnings().toString());

        plan.tick(start, start + 29 * SECOND, snapshot("2026-09-03T05:00:29"));

        assertEquals(1, sink.sends(), "между моментами предупреждений тихо");

        plan.tick(start + 29 * SECOND, start + 31 * SECOND, snapshot("2026-09-03T05:00:31"));

        assertEquals(2, sink.sends(), "повтор тридцатки в списке отброшен");
        assertTrue(
            warnings().get(1)
                .contains(UtilsMessages.RESTART_WARN + "[30s]"),
            warnings().toString());
    }

    @Test
    void aNewPlanOverAClosedDoorGivesTheDoorBack() {
        file.steps.closeDoorSeconds = 30;
        file.steps.kickSeconds = 5;
        warnAt();
        RestartPlan plan = plan(runner());
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start + 60 * SECOND, RestartReason.COMMAND);

        plan.tick(start + 29 * SECOND, start + 31 * SECOND, snapshot("2026-09-03T05:00:31"));
        assertTrue(plan.doorClosed());

        assertEquals(RestartPlan.Answer.ARMED, plan.armAt(start + 3600 * SECOND, RestartReason.COMMAND));

        assertFalse(plan.doorClosed(), "перевооружение сбрасывает закрытую дверь");
        assertEquals(
            list(FakeShutdown.DOOR_CLOSED, FakeShutdown.DOOR_OPEN),
            shutdown.journal(),
            "дверь открыта снова и закроется по срокам нового плана");

        plan.tick(start + 31 * SECOND, start + 32 * SECOND, snapshot("2026-09-03T05:00:32"));
        assertFalse(plan.doorClosed(), "новый план закрывает дверь за свои тридцать секунд, а не час раньше");
    }

    @Test
    void theDoorClosesItsOwnSecondsBeforeTheKick() {
        file.steps.closeDoorSeconds = 30;
        file.steps.kickSeconds = 5;
        warnAt();
        shutdown.join("Alice");
        RestartPlan plan = plan(runner());
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start + 60 * SECOND, RestartReason.COMMAND);

        plan.tick(start, start + 29 * SECOND, snapshot("2026-09-03T05:00:29"));

        assertFalse(plan.doorClosed(), "до тридцати секунд дверь открыта");

        plan.tick(start + 29 * SECOND, start + 31 * SECOND, snapshot("2026-09-03T05:00:31"));

        assertTrue(plan.doorClosed());
        assertEquals(list(FakeShutdown.DOOR_CLOSED), shutdown.journal(), "кика ещё не было");

        plan.tick(start + 31 * SECOND, start + 55 * SECOND, snapshot("2026-09-03T05:00:55"));

        assertEquals(
            list(FakeShutdown.DOOR_CLOSED, "screen Alice", "kick Alice"),
            shutdown.journal(),
            "на пятой секунде до остановки идёт кик");
    }

    @Test
    void aDoorThatClosesTogetherWithTheKickIsAllowed() {
        file.steps.closeDoorSeconds = 0;
        file.steps.kickSeconds = 0;
        warnAt();
        shutdown.join("Alice");
        RestartPlan plan = plan(runner());
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start, RestartReason.COMMAND);

        plan.tick(start - SECOND, start, snapshot("2026-09-03T05:00:00"));

        assertEquals(list("door closed", "screen Alice", "kick Alice"), shutdown.journal());
    }

    @Test
    void cancelBeforeTheKickGivesTheDoorAndTheScheduleBack() {
        warnAt();
        RestartPlan plan = plan(runner());
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start + 60 * SECOND, RestartReason.COMMAND);
        plan.tick(start + 29 * SECOND, start + 31 * SECOND, snapshot("2026-09-03T05:00:31"));

        assertEquals(RestartPlan.Answer.CANCELLED, plan.cancel(start + 32 * SECOND));

        assertFalse(plan.armed());
        assertFalse(plan.doorClosed());
        assertEquals(list(FakeShutdown.DOOR_CLOSED, FakeShutdown.DOOR_OPEN), shutdown.journal());
        assertEquals(1, sink.sends(), "об отмене сказано всем онлайн");
        assertTrue(
            warnings().get(0)
                .contains(UtilsMessages.RESTART_CANCELLED));
    }

    @Test
    void cancelAfterTheKickIsRefused() {
        warnAt();
        RestartPlan plan = plan(runner());
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start, RestartReason.COMMAND);
        plan.tick(start - SECOND, start, snapshot("2026-09-03T05:00:00"));

        assertEquals(RestartPlan.Answer.TOO_LATE, plan.cancel(start + SECOND));
        assertEquals(RestartPlan.Answer.TOO_LATE, plan.armAt(start + 600 * SECOND, RestartReason.COMMAND));
    }

    @Test
    void cancelWithoutARestartSaysThereWasNothing() {
        RestartPlan plan = plan(runner());

        assertEquals(RestartPlan.Answer.NOTHING, plan.cancel(at("2026-09-03T05:00:00")));
        assertEquals(0, sink.sends());
    }

    @Test
    void theScheduleStartsTheCountdownSoThatTheServerStopsAtItsMoment() {
        file.schedule.enabled = true;
        file.schedule.at = new java.util.ArrayList<>(Arrays.asList("06:00"));
        warnAt(600, 60);
        RestartPlan plan = plan(runner());

        long early = at("2026-09-03T05:49:00");
        plan.tick(early - SECOND, early, snapshot("2026-09-03T05:49:00"));

        assertFalse(plan.armed(), "за одиннадцать минут отсчёт ещё не начинается");

        long lead = at("2026-09-03T05:50:00");
        plan.tick(lead - SECOND, lead, snapshot("2026-09-03T05:50:00"));

        assertTrue(plan.armed());
        assertEquals(at("2026-09-03T06:00:00"), plan.stopAt(), "остановка ровно в момент расписания");
        assertEquals(RestartReason.SCHEDULE, plan.reason());
        assertEquals(1, sink.sends(), "первое предупреждение уходит сразу");
    }

    @Test
    void aDayThatIsNotInTheListIsSkipped() {
        file.schedule.enabled = true;
        file.schedule.at = new java.util.ArrayList<>(Arrays.asList("06:00"));
        file.schedule.days = new java.util.ArrayList<>(Arrays.asList("mon"));
        warnAt(600);
        RestartPlan plan = plan(runner());

        long lead = at("2026-09-03T05:50:00");
        plan.tick(lead - SECOND, lead, snapshot("2026-09-03T05:50:00"));

        assertFalse(plan.armed(), "третье сентября 2026 это четверг");
    }

    @Test
    void aConditionThatIsNotMetHoldsTheScheduleBack() {
        file.schedule.enabled = true;
        file.schedule.at = new java.util.ArrayList<>(Arrays.asList("06:00"));
        file.schedule.when = new WhenBlock(5);
        warnAt(600);
        RestartPlan plan = plan(runner());

        long lead = at("2026-09-03T05:50:00");
        plan.tick(lead - SECOND, lead, snapshot("2026-09-03T05:50:00"));

        assertFalse(plan.armed(), "на сервере один игрок, а условие просит пятерых");

        facts.join("Bob", 0);
        facts.join("Carol", 0);
        facts.join("Dave", 0);
        facts.join("Erin", 0);
        long later = at("2026-09-03T05:51:00");
        plan.tick(later - SECOND, later, snapshot("2026-09-03T05:51:00"));

        assertTrue(plan.armed(), "условие сошлось, отсчёт начался");
    }

    @Test
    void aScheduleThatIsOffLeavesTheCommandWorking() {
        file.schedule.enabled = false;
        RestartPlan plan = plan(runner());

        long lead = at("2026-09-03T05:50:00");
        plan.tick(lead - SECOND, lead, snapshot("2026-09-03T05:50:00"));

        assertFalse(plan.armed());
        assertEquals(0L, plan.nextScheduled(lead));

        assertEquals(RestartPlan.Answer.ARMED, plan.armAt(lead + 60 * SECOND, RestartReason.COMMAND));
        assertTrue(plan.armed());
    }

    @Test
    void nowStartsTheStopWithoutWaitingForTheNextTick() {
        shutdown.join("Alice");
        RestartPlan plan = plan(runner());
        long start = at("2026-09-03T05:00:00");

        assertEquals(RestartPlan.Answer.ARMED, plan.now(start));

        assertEquals(list("door closed", "screen Alice", "kick Alice"), shutdown.journal());
        assertEquals(0, sink.sends(), "у немедленной остановки предупреждений нет");
    }

    @Test
    void secondsLeftCountUpwardsToTheWholeSecond() {
        RestartPlan plan = plan(runner());
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start + 61_500L, RestartReason.COMMAND);

        assertEquals(62, plan.secondsLeft(start));
        assertEquals(0, plan.secondsLeft(start + 62_000L));
    }
}
