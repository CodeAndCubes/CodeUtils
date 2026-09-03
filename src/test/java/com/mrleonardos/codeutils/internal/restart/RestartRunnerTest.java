package com.mrleonardos.codeutils.internal.restart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.restart.RestartPhase;
import com.mrleonardos.codeutils.api.restart.RestartReason;

class RestartRunnerTest extends RestartTests {

    @Test
    void theJournalOfAWholeStopMatchesTheOrderOfTheDesign() {
        shutdown.join("Alice");
        step("door", RestartPhase.BEFORE_DOOR);
        step("kick", RestartPhase.BEFORE_KICK);
        step("save", RestartPhase.BEFORE_SAVE);
        step("after", RestartPhase.AFTER_SAVE);
        warnAt();
        RestartRunner runner = runner();
        RestartPlan plan = plan(runner);
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start + 30 * SECOND, RestartReason.COMMAND);

        plan.tick(start, start + 30 * SECOND, snapshot("2026-09-03T05:00:30"));

        assertEquals(
            list("step door", "door closed", "step kick", "screen Alice", "kick Alice"),
            shutdown.journal(),
            "до паузы: дверь, шаги, закрытие окна и только потом кик");

        scheduler.runNext();

        assertEquals(
            list(
                "step door",
                "door closed",
                "step kick",
                "screen Alice",
                "kick Alice",
                "step save",
                "players saved",
                "worlds saved",
                "step after",
                "stopped"),
            shutdown.journal());
    }

    @Test
    void theWindowWaitsTheSettleTicksThroughTheSchedulerOfTheCore() {
        shutdown.join("Alice");
        file.steps.settleTicks = 40;
        RestartRunner runner = runner();
        RestartPlan plan = plan(runner);
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start, RestartReason.COMMAND);

        plan.tick(start - SECOND, start, snapshot("2026-09-03T05:00:00"));

        assertEquals(40, scheduler.lastDelay());
        assertEquals(1, scheduler.pending(), "запись миров ждёт своей паузы, а не идёт сразу");
        assertTrue(tickStopped, "такт мода встал с начала кика");
    }

    @Test
    void aPlayerWhoSlippedThroughTheDoorGetsOneMoreKickAndTheStopGoesOn() {
        shutdown.join("Alice");
        shutdown.stubborn("Alice");
        RestartRunner runner = runner();
        RestartPlan plan = plan(runner);
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start, RestartReason.COMMAND);

        plan.tick(start - SECOND, start, snapshot("2026-09-03T05:00:00"));
        scheduler.runNext();

        assertEquals(
            list(
                "door closed",
                "screen Alice",
                "kick Alice",
                "screen Alice",
                "kick Alice",
                "players saved",
                "worlds saved",
                "stopped"),
            shutdown.journal(),
            "кик повторён ровно один раз, дальше остановка идёт по порядку");
    }

    @Test
    void aStepThatThrowsIsWrittenDownAndTheServerStopsAnyway() {
        failingStep("greedy", RestartPhase.BEFORE_SAVE);
        step("after", RestartPhase.AFTER_SAVE);
        RestartRunner runner = runner();
        RestartPlan plan = plan(runner);
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start, RestartReason.COMMAND);

        plan.tick(start - SECOND, start, snapshot("2026-09-03T05:00:00"));
        scheduler.runNext();

        assertEquals(
            list("door closed", "step greedy", "players saved", "worlds saved", "step after", "stopped"),
            shutdown.journal());
    }

    @Test
    void theKeysOfTheKickAndTheDoorComeFromTheFile() {
        shutdown.join("Alice");
        file.messages.doorKey = "my.door";
        RestartRunner runner = runner();
        RestartPlan plan = plan(runner);
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start, RestartReason.COMMAND);

        plan.tick(start - SECOND, start, snapshot("2026-09-03T05:00:00"));

        assertEquals("my.door", shutdown.doorKey());
    }

    @Test
    void writingPlayersAndWorldsCanBeLeftToTheServer() {
        file.steps.savePlayers = false;
        file.steps.saveWorlds = false;
        RestartRunner runner = runner();
        RestartPlan plan = plan(runner);
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start, RestartReason.COMMAND);

        plan.tick(start - SECOND, start, snapshot("2026-09-03T05:00:00"));
        scheduler.runNext();

        assertEquals(list("door closed", "stopped"), shutdown.journal());
    }

    @Test
    void aSecondBeginChangesNothingWhileTheStopIsAlreadyGoing() {
        RestartRunner runner = runner();
        RestartPlan plan = plan(runner);
        long start = at("2026-09-03T05:00:00");
        plan.armAt(start, RestartReason.COMMAND);

        plan.tick(start - SECOND, start, snapshot("2026-09-03T05:00:00"));
        assertTrue(runner.stopping());
        plan.tick(start, start + SECOND, snapshot("2026-09-03T05:00:01"));

        assertEquals(1, scheduler.pending(), "второй паузы не завелось");
        assertFalse(
            shutdown.journal()
                .contains("stopped"),
            "остановка идёт только после паузы");
    }
}
