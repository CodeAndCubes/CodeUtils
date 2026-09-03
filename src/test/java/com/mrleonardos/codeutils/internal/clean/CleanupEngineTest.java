package com.mrleonardos.codeutils.internal.clean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.clean.CleanupReport;
import com.mrleonardos.codeutils.api.clean.EntityKind;
import com.mrleonardos.codeutils.api.when.ServerSnapshot;
import com.mrleonardos.codeutils.internal.Announcer;
import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.Conditions;
import com.mrleonardos.codeutils.internal.FakeFacts;
import com.mrleonardos.codeutils.internal.FakeTexts;
import com.mrleonardos.codeutils.internal.WhenBlock;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile;
import com.mrleonardos.codeutils.internal.broadcast.RecordingSink;
import com.mrleonardos.codeutils.internal.clean.CleanupFile.RuleBlock;
import com.mrleonardos.codeutils.internal.command.UtilsMessages;

class CleanupEngineTest {

    private static final Logger LOG = LogManager.getLogger("codeutils-test");

    private static final ZoneId ZONE = ZoneId.of("UTC");

    private static final long SECOND = Clocks.MILLIS;

    private final UtilsRegistry registry = new UtilsRegistry();
    private final FakeSweep sweep = new FakeSweep();
    private final FakeFacts facts = new FakeFacts();
    private final RecordingSink sink = new RecordingSink();
    private final CleanupFile file = new CleanupFile();

    private int slowPassMillis;

    CleanupEngineTest() {
        registry.addSink(BroadcastsFile.SINK_CHAT, sink);
        facts.join("Watcher", 0);
        file.rules = new LinkedHashMap<>();
    }

    @Test
    void aScheduledRuleRemovesWhatMatchesAndLeavesTheRest() {
        put("drops", rule(60, "@item"));
        sweep.add(FakeEntity.item(), FakeEntity.item(), FakeEntity.zombie());
        CleanupEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(start, start + 60 * SECOND, snapshot("2026-09-03T04:01:00"));

        assertEquals(
            2,
            sweep.removed()
                .size());
        assertEquals(1, sweep.alive(), "зомби под правило не подходил");
    }

    @Test
    void aCountingPassRemovesNothingAndRemovalGoesInOneSecondStep() {
        put("drops", rule(0, "@item"));
        sweep.add(FakeEntity.item(), FakeEntity.item());
        CleanupEngine engine = engine();
        engine.arm(at("2026-09-03T04:00:00"));

        CleanupReport counted = engine.count("drops");

        assertEquals(2, counted.matched());
        assertEquals(0, counted.removed());
        assertEquals(0, sweep.removeCalls(), "счётный проход не трогает ни одной сущности");

        engine.clean("drops");

        assertEquals(1, sweep.removeCalls(), "удаление идёт одним вызовом по собранному списку");
        assertEquals(
            2,
            sweep.removed()
                .size());
    }

    @Test
    void aRuleTouchesOnlyItsOwnDimensions() {
        RuleBlock block = rule(60, "@item");
        block.dimensions = new ArrayList<>(Arrays.asList(Integer.valueOf(-1)));
        put("drops", block);
        sweep.add(
            FakeEntity.item()
                .in(0),
            FakeEntity.item()
                .in(-1));
        CleanupEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(start, start + 60 * SECOND, snapshot("2026-09-03T04:01:00"));

        assertEquals(
            1,
            sweep.removed()
                .size());
        assertEquals(
            -1,
            sweep.removed()
                .get(0)
                .dimension());
    }

    @Test
    void anEntityYoungerThanTheMinimumAgeStays() {
        RuleBlock block = rule(60, "@item");
        block.minAgeSeconds = 60;
        put("drops", block);
        sweep.add(
            FakeEntity.item()
                .age(5),
            FakeEntity.item()
                .age(120));
        CleanupEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(start, start + 60 * SECOND, snapshot("2026-09-03T04:01:00"));

        assertEquals(
            1,
            sweep.removed()
                .size());
        assertEquals(
            120,
            sweep.removed()
                .get(0)
                .ageSeconds());
    }

    @Test
    void aGuardedEntityIsNotRemovedAndAPlayerIsNeverEvenLookedAt() {
        put("mobs", rule(60, "@passive", "@all"));
        sweep.add(
            FakeEntity.wolf()
                .guarded(Guards.TAMED),
            FakeEntity.wolf(),
            FakeEntity.of("Steve", EntityKind.PLAYER));
        CleanupEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(start, start + 60 * SECOND, snapshot("2026-09-03T04:01:00"));

        assertEquals(
            1,
            sweep.removed()
                .size(),
            "снят только не прирученный волк");
        assertEquals(2, sweep.alive(), "прирученный волк и игрок остались");
    }

    @Test
    void aWorldThresholdThatIsNotPassedLeavesEverythingAlone() {
        RuleBlock block = rule(60, "@item");
        block.worldThreshold = 5;
        put("drops", block);
        sweep.add(FakeEntity.item(), FakeEntity.item());
        CleanupEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(start, start + 60 * SECOND, snapshot("2026-09-03T04:01:00"));

        assertEquals(
            0,
            sweep.removed()
                .size());
        assertEquals(0, sink.sends(), "предупреждения тоже не было");
    }

    @Test
    void aChunkThresholdTakesOnlyTheChunksThatAreOverIt() {
        RuleBlock block = rule(60, "@item");
        block.chunkThreshold = 2;
        put("drops", block);
        sweep.add(
            FakeEntity.item()
                .chunk(1, 1),
            FakeEntity.item()
                .chunk(1, 1),
            FakeEntity.item()
                .chunk(1, 1),
            FakeEntity.item()
                .chunk(9, 9));
        CleanupEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(start, start + 60 * SECOND, snapshot("2026-09-03T04:01:00"));

        assertEquals(
            3,
            sweep.removed()
                .size(),
            "снят переполненный чанк");
        assertEquals(1, sweep.alive(), "одинокий предмет другого чанка остался");
    }

    @Test
    void aRuleWithOnlyAThresholdCountsByTheBeatOfTheMod() {
        RuleBlock block = rule(0, "@item");
        block.worldThreshold = 2;
        put("drops", block);
        sweep.add(FakeEntity.item(), FakeEntity.item());
        CleanupEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(start, start + CleanupEngine.THRESHOLD_SECONDS * SECOND, snapshot("2026-09-03T04:00:30"));

        assertEquals(
            0,
            sweep.removed()
                .size(),
            "двух сущностей мало, порог просит больше двух");

        sweep.add(FakeEntity.item());
        long later = start + 2 * CleanupEngine.THRESHOLD_SECONDS * SECOND;
        engine.tick(start + CleanupEngine.THRESHOLD_SECONDS * SECOND, later, snapshot("2026-09-03T04:01:00"));

        assertEquals(
            3,
            sweep.removed()
                .size(),
            "порог перейден, проход снял всё подходящее");
    }

    @Test
    void aRuleWithNeitherScheduleNorThresholdWaitsForTheCommand() {
        put("drops", rule(0, "@item"));
        sweep.add(FakeEntity.item());
        CleanupEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(start, start + 3600 * SECOND, snapshot("2026-09-03T05:00:00"));

        assertEquals(
            0,
            sweep.removed()
                .size());
        assertEquals(0L, engine.nextPassOf("drops"));

        engine.clean("drops");

        assertEquals(
            1,
            sweep.removed()
                .size());
    }

    @Test
    void maxRemovalsCutsTheListOfOnePass() {
        RuleBlock block = rule(60, "@item");
        block.maxRemovals = 2;
        put("drops", block);
        sweep.add(FakeEntity.item(), FakeEntity.item(), FakeEntity.item(), FakeEntity.item());
        CleanupEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(start, start + 60 * SECOND, snapshot("2026-09-03T04:01:00"));

        assertEquals(
            2,
            sweep.removed()
                .size());
        assertEquals(2, sweep.alive());
    }

    @Test
    void warningsGoOutBeforeThePassWithTheirSecondsAndTheirNumber() {
        RuleBlock block = rule(60, "@item");
        block.warnSeconds = new ArrayList<>(Arrays.asList(Integer.valueOf(30), Integer.valueOf(10)));
        put("drops", block);
        sweep.add(FakeEntity.item(), FakeEntity.item());
        CleanupEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        long due = start + 60 * SECOND;
        engine.tick(start, due, snapshot("2026-09-03T04:01:00"));

        assertEquals(1, sink.sends(), "первое предупреждение ушло сразу");
        assertTrue(
            sink.texts()
                .get(0)
                .contains(UtilsMessages.CLEAN_WARN + "[30, 2]"),
            sink.texts()
                .toString());
        assertEquals(
            0,
            sweep.removed()
                .size(),
            "до конца предупреждений ничего не снято");

        engine.tick(due, due + 20 * SECOND, snapshot("2026-09-03T04:01:20"));

        assertEquals(2, sink.sends(), "второе предупреждение за десять секунд");
        assertEquals(
            0,
            sweep.removed()
                .size());

        engine.tick(due + 20 * SECOND, due + 30 * SECOND, snapshot("2026-09-03T04:01:30"));

        assertEquals(
            2,
            sweep.removed()
                .size(),
            "проход прошёл, когда предупреждения кончились");
    }

    @Test
    void aRuleThatIsOffCountsNothingAndKeepsItsRecordInTheFile() {
        RuleBlock block = rule(60, "@item");
        block.enabled = false;
        put("drops", block);
        sweep.add(FakeEntity.item());
        CleanupEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(start, start + 600 * SECOND, snapshot("2026-09-03T04:10:00"));

        assertFalse(engine.knows("drops"));
        assertTrue(engine.written("drops"));
        assertEquals(
            0,
            sweep.removed()
                .size());
    }

    @Test
    void aConditionThatIsNotMetLetsThePassGoBy() {
        RuleBlock block = rule(60, "@item");
        block.when = new WhenBlock(5);
        put("drops", block);
        sweep.add(FakeEntity.item());
        CleanupEngine engine = engine();
        long start = at("2026-09-03T04:00:00");
        engine.arm(start);

        engine.tick(start, start + 60 * SECOND, snapshot("2026-09-03T04:01:00"));

        assertEquals(
            0,
            sweep.removed()
                .size());
    }

    @Test
    void aSlowPassIsCountedInMillisecondsAndTheReportHoldsEveryNumber() {
        slowPassMillis = 0;
        put("drops", rule(0, "@item"));
        sweep.add(FakeEntity.item(), FakeEntity.zombie());
        CleanupEngine engine = engine();
        engine.arm(at("2026-09-03T04:00:00"));

        CleanupReport pass = engine.clean("drops");

        assertEquals("drops", pass.rule());
        assertEquals(2, pass.scanned(), "просмотрены обе сущности");
        assertEquals(1, pass.matched(), "правилу подошёл только предмет");
        assertEquals(1, pass.removed());
    }

    private void put(String name, RuleBlock block) {
        file.rules.put(name, block);
    }

    private CleanupEngine engine() {
        return new CleanupEngine(
            () -> file,
            registry,
            new Conditions(registry),
            sweep,
            new Announcer(facts, new FakeTexts()),
            () -> slowPassMillis,
            () -> Boolean.FALSE,
            LOG);
    }

    private static RuleBlock rule(int intervalSeconds, String... types) {
        RuleBlock block = new RuleBlock();
        block.intervalSeconds = intervalSeconds;
        block.minAgeSeconds = 0;
        block.types = new ArrayList<>(Arrays.asList(types));
        block.warnSeconds = new ArrayList<>();
        block.when = new WhenBlock();
        return block;
    }

    private ServerSnapshot snapshot(String time) {
        return ServerSnapshot.of(
            facts.online()
                .size(),
            new LinkedHashSet<>(),
            20.0D,
            LocalDateTime.parse(time));
    }

    private static long at(String time) {
        return Clocks.millisOf(LocalDateTime.parse(time), ZONE);
    }
}
