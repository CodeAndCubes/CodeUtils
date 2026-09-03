package com.mrleonardos.codeutils.internal.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.run.RunOutcome;
import com.mrleonardos.codeutils.api.run.SenderChoice;
import com.mrleonardos.codeutils.api.when.ServerSnapshot;
import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.Conditions;
import com.mrleonardos.codeutils.internal.WhenBlock;
import com.mrleonardos.codeutils.internal.job.JobsFile.JobBlock;

class JobEngineTest {

    private static final Logger LOG = LogManager.getLogger("codeutils-test");

    private static final ZoneId ZONE = ZoneId.of("UTC");

    private static final long SECOND = 1_000L;

    private final UtilsRegistry registry = new UtilsRegistry();
    private final RecordingRunner runner = new RecordingRunner();
    private final JobsFile file = new JobsFile();

    private int players = 1;

    JobEngineTest() {
        registry.addRunner(JobEngine.SERVER_RUNNER, runner);
        file.jobs = new LinkedHashMap<>();
    }

    @Test
    void aJobFiresWhenItsMomentFallsIntoTheGap() {
        put("save", job("save-all", 0, "04:00"));
        JobEngine engine = engine();
        engine.arm(at("2026-09-03T03:00:00"));

        engine.tick(0L, at("2026-09-03T04:00:30"), snapshot("2026-09-03T04:00:30"));

        assertEquals(Arrays.asList("save-all"), runner.commands());
    }

    @Test
    void aMomentThatPassedWhileTheServerWasDownIsNotCaughtUpByDefault() {
        put("save", job("save-all", 0, "06:00"));
        JobEngine engine = engine();
        engine.arm(at("2026-09-03T06:00:40"));

        engine.tick(0L, at("2026-09-03T06:00:41"), snapshot("2026-09-03T06:00:41"));

        assertEquals(0, runner.runs(), "catchUpSeconds = 0 значит момент потерян");
    }

    @Test
    void aCatchUpWindowLetsTheMissedMomentThrough() {
        JobBlock block = job("save-all", 0, "06:00");
        block.catchUpSeconds = 120;
        put("save", block);
        JobEngine engine = engine();
        engine.arm(at("2026-09-03T06:00:40"));

        engine.tick(0L, at("2026-09-03T06:00:41"), snapshot("2026-09-03T06:00:41"));

        assertEquals(1, runner.runs());
    }

    @Test
    void twoMomentsInsideOneJumpGiveTwoRuns() {
        put("save", job("save-all", 0, "04:00", "04:02"));
        JobEngine engine = engine();
        engine.arm(at("2026-09-03T03:59:50"));

        engine.tick(0L, at("2026-09-03T04:02:30"), snapshot("2026-09-03T04:02:30"));

        assertEquals(2, runner.runs());
    }

    @Test
    void conditionsThatAreNotMetLetTheMomentPassWithoutTheCommand() {
        JobBlock block = job("save-all", 0, "04:00");
        block.when = new WhenBlock(2);
        put("save", block);
        JobEngine engine = engine();
        engine.arm(at("2026-09-03T03:00:00"));

        engine.tick(0L, at("2026-09-03T04:00:30"), snapshot("2026-09-03T04:00:30"));

        assertEquals(0, runner.runs());

        players = 2;
        engine.tick(0L, at("2026-09-03T04:01:30"), snapshot("2026-09-03T04:01:30"));

        assertEquals(0, runner.runs(), "момент пройден, ждать до завтра");
    }

    @Test
    void theSenderIsReadFromTheFileAndCarriedIntoTheTicket() {
        JobBlock block = job("save-all", 10, new String[0]);
        block.as = "commandblock";
        put("save", block);
        JobEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(0L, start + 10 * SECOND, snapshot("2026-09-03T04:00:10"));

        assertEquals(
            SenderChoice.Kind.COMMAND_BLOCK,
            runner.tickets()
                .get(0)
                .sender()
                .kind());
    }

    @Test
    void aPlayerWhoIsNotOnlineCountsAsAFailureAndNotAsACrash() {
        JobBlock block = job("give", 10, new String[0]);
        block.as = "player:Steve";
        put("give", block);
        JobEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(0L, start + 10 * SECOND, snapshot("2026-09-03T04:00:10"));

        assertEquals(1, runner.runs());
        assertEquals(
            RunOutcome.NO_PLAYER,
            engine.runNow("give")
                .reason());
    }

    @Test
    void retryTriesAgainAfterItsPauseAndStopsOnTheFirstSuccess() {
        JobBlock block = job("save-all", 10, new String[0]);
        block.onFailure = JobsFile.ON_FAILURE_RETRY;
        block.retrySeconds = 5;
        block.retries = 2;
        put("save", block);
        runner.worksFrom(2);
        JobEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(0L, start + 10 * SECOND, snapshot("2026-09-03T04:00:10"));
        assertEquals(1, runner.runs());

        engine.tick(0L, start + 12 * SECOND, snapshot("2026-09-03T04:00:12"));
        assertEquals(1, runner.runs(), "пауза ещё не вышла");

        engine.tick(0L, start + 15 * SECOND, snapshot("2026-09-03T04:00:15"));
        assertEquals(2, runner.runs());

        engine.tick(0L, start + 19 * SECOND, snapshot("2026-09-03T04:00:19"));
        assertEquals(2, runner.runs(), "вторая попытка сработала, третьей не будет");
    }

    @Test
    void retryGivesUpAfterTheAttemptsAreSpent() {
        JobBlock block = job("save-all", 100, new String[0]);
        block.onFailure = JobsFile.ON_FAILURE_RETRY;
        block.retrySeconds = 5;
        block.retries = 2;
        put("save", block);
        runner.worksFrom(0);
        JobEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(0L, start + 100 * SECOND, snapshot("2026-09-03T04:01:40"));
        engine.tick(0L, start + 105 * SECOND, snapshot("2026-09-03T04:01:45"));
        engine.tick(0L, start + 110 * SECOND, snapshot("2026-09-03T04:01:50"));
        engine.tick(0L, start + 130 * SECOND, snapshot("2026-09-03T04:02:10"));

        assertEquals(3, runner.runs(), "первая попытка и два повтора");
    }

    @Test
    void quietAndLogNeverRepeatTheCommand() {
        put("quiet", failing(JobsFile.ON_FAILURE_QUIET));
        put("noisy", failing(JobsFile.ON_FAILURE_LOG));
        runner.worksFrom(0);
        JobEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(0L, start + 100 * SECOND, snapshot("2026-09-03T04:01:40"));
        engine.tick(0L, start + 130 * SECOND, snapshot("2026-09-03T04:02:10"));

        assertEquals(2, runner.runs(), "по одной неудаче на задание, ни одного повтора");
    }

    @Test
    void aJobThatIsOffOrEmptyDoesNotWork() {
        JobBlock off = job("save-all", 10, new String[0]);
        off.enabled = false;
        put("off", off);
        put("empty", job("", 10, new String[0]));
        put("idle", job("save-all", 0, new String[0]));
        JobEngine engine = engine();
        engine.arm(at("2026-09-03T04:00:00"));

        assertTrue(
            engine.names()
                .isEmpty());
        assertFalse(engine.knows("off"));
        assertTrue(engine.written("off"), "выключенное задание остаётся в файле");
    }

    @Test
    void aHandRunDoesNotMoveTheSchedule() {
        put("save", job("save-all", 0, "04:00"));
        JobEngine engine = engine();
        long start = at("2026-09-03T03:00:00");
        engine.arm(start);
        long next = engine.nextMomentOf("save", start);

        assertTrue(
            engine.runNow("save")
                .successful());
        assertEquals(1, runner.runs());
        assertEquals(next, engine.nextMomentOf("save", start));

        engine.tick(0L, at("2026-09-03T04:00:30"), snapshot("2026-09-03T04:00:30"));

        assertEquals(2, runner.runs(), "момент всё равно приходит в своё время");
    }

    @Test
    void aHandRunOfAnUnknownJobIsRefusedWithoutARunner() {
        JobEngine engine = engine();
        engine.arm(at("2026-09-03T04:00:00"));

        assertEquals(
            RunOutcome.NO_RUNNER,
            engine.runNow("nope")
                .reason());
        assertEquals(0, runner.runs());
    }

    private void put(String name, JobBlock block) {
        file.jobs.put(name, block);
    }

    private JobEngine engine() {
        return new JobEngine(() -> file, registry, new Conditions(registry), () -> ZONE, () -> Boolean.FALSE, LOG);
    }

    private static JobBlock failing(String policy) {
        JobBlock block = job("save-all", 100, new String[0]);
        block.onFailure = policy;
        block.retrySeconds = 5;
        block.retries = 2;
        return block;
    }

    private static JobBlock job(String command, int everySeconds, String... at) {
        JobBlock block = new JobBlock();
        block.command = command;
        block.everySeconds = everySeconds;
        block.at = new ArrayList<>(Arrays.asList(at));
        block.when = new WhenBlock();
        return block;
    }

    private ServerSnapshot snapshot(String time) {
        return ServerSnapshot.of(players, new java.util.LinkedHashSet<>(), 20.0D, LocalDateTime.parse(time));
    }

    private static long at(String time) {
        return Clocks.millisOf(LocalDateTime.parse(time), ZONE);
    }
}
