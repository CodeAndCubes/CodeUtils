package com.mrleonardos.codeutils.internal.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.mrleonardos.codecore.api.actor.PlayerRef;
import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.when.ServerSnapshot;
import com.mrleonardos.codeutils.internal.Conditions;
import com.mrleonardos.codeutils.internal.FakeFacts;
import com.mrleonardos.codeutils.internal.WhenBlock;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile.MessageBlock;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile.SetBlock;

class BroadcastEngineTest {

    private static final Logger LOG = LogManager.getLogger("codeutils-test");

    private static final long START = 1_000_000L;
    private static final long SECOND = 1_000L;

    private final FakeFacts facts = new FakeFacts();
    private final UtilsRegistry registry = new UtilsRegistry();
    private final RecordingSink sink = new RecordingSink();
    private final BroadcastsFile file = new BroadcastsFile();

    BroadcastEngineTest() {
        registry.addSink("chat", sink);
        file.sets = new LinkedHashMap<>();
    }

    @Test
    void theFirstMessageWaitsForTheFirstDelayAndThenTheIntervalRules() {
        put("tips", set(10, 5, "первое", "второе"));
        facts.join("Steve", 0);
        BroadcastEngine engine = engine();
        engine.arm(START);

        tick(engine, START + 4 * SECOND);
        assertEquals(0, sink.sends(), "до firstDelaySeconds никто ничего не шлёт");

        tick(engine, START + 5 * SECOND);
        assertEquals(1, sink.sends());

        tick(engine, START + 10 * SECOND);
        assertEquals(1, sink.sends(), "интервал десять секунд, ещё не время");

        tick(engine, START + 15 * SECOND);
        assertEquals(2, sink.sends());
        assertEquals(Arrays.asList("первое", "второе"), sink.texts());
    }

    @Test
    void twoSetsWithDifferentDelaysDoNotSpeakAtOnce() {
        put("tips", set(10, 5, "подсказка"));
        put("links", set(10, 8, "ссылка"));
        facts.join("Steve", 0);
        BroadcastEngine engine = engine();
        engine.arm(START);

        tick(engine, START + 5 * SECOND);
        tick(engine, START + 8 * SECOND);

        assertEquals(Arrays.asList("подсказка", "ссылка"), sink.texts());
    }

    @Test
    void thePrefixGoesInFrontOfEveryMessage() {
        SetBlock block = set(10, 0, "текст");
        block.prefix = "&8[!]&r ";
        put("tips", block);
        facts.join("Steve", 0);
        BroadcastEngine engine = engine();
        engine.arm(START);

        tick(engine, START + SECOND);

        assertEquals(Arrays.asList("§8[!]§r текст"), sink.texts());
    }

    @Test
    void recipientsAreNarrowedByThePermissionAndTheDimensionTogether() {
        SetBlock block = set(10, 0, "текст");
        block.permission = "codeutils.vip";
        block.dimensions = new ArrayList<>(Collections.singletonList(Integer.valueOf(0)));
        put("tips", block);
        facts.join("Steve", 0, "codeutils.vip");
        facts.join("Alex", 0);
        facts.join("Notch", -1, "codeutils.vip");
        BroadcastEngine engine = engine();
        engine.arm(START);

        tick(engine, START + SECOND);

        List<PlayerRef> got = sink.lastRecipients();
        assertEquals(1, got.size(), () -> "и право, и измерение: " + got);
        assertEquals(
            "Steve",
            got.get(0)
                .name());
    }

    @Test
    void emptyPermissionAndEmptyDimensionsMeanEverybody() {
        put("tips", set(10, 0, "текст"));
        facts.join("Steve", 0);
        facts.join("Notch", -1);
        BroadcastEngine engine = engine();
        engine.arm(START);

        tick(engine, START + SECOND);

        assertEquals(
            2,
            sink.lastRecipients()
                .size());
    }

    @Test
    void holdSendsNothingAndKeepsThePointerWhereItWas() {
        SetBlock block = set(10, 0, "первое", "второе");
        block.whenEmpty = BroadcastsFile.EMPTY_HOLD;
        put("tips", block);
        BroadcastEngine engine = engine();
        engine.arm(START);

        tick(engine, START + SECOND);
        assertEquals(0, sink.sends());

        facts.join("Steve", 0);
        tick(engine, START + 11 * SECOND);

        assertEquals(Arrays.asList("первое"), sink.texts(), "первый зашедший видит первую подсказку");
    }

    @Test
    void skipMovesThePointerWithoutSendingAnything() {
        SetBlock block = set(10, 0, "первое", "второе");
        block.whenEmpty = BroadcastsFile.EMPTY_SKIP;
        put("tips", block);
        BroadcastEngine engine = engine();
        engine.arm(START);

        tick(engine, START + SECOND);
        assertEquals(0, sink.sends());

        facts.join("Steve", 0);
        tick(engine, START + 11 * SECOND);

        assertEquals(Arrays.asList("второе"), sink.texts(), "указатель сдвинулся, первое сообщение пропущено");
    }

    @Test
    void anEmptyServerNeverBuildsUpAQueueOfMessages() {
        SetBlock block = set(10, 0, "первое", "второе", "третье");
        block.whenEmpty = BroadcastsFile.EMPTY_HOLD;
        put("tips", block);
        BroadcastEngine engine = engine();
        engine.arm(START);

        for (int shot = 1; shot <= 10; shot++) {
            tick(engine, START + shot * 10 * SECOND);
        }
        facts.join("Steve", 0);
        tick(engine, START + 111 * SECOND);

        assertEquals(1, sink.sends(), "после десяти пустых интервалов уходит одно сообщение, а не десять");
    }

    @Test
    void aSetWhoseConditionsAreNotMetStaysQuietAndKeepsItsPointer() {
        SetBlock block = set(10, 0, "первое", "второе");
        block.when = new WhenBlock(2);
        put("tips", block);
        facts.join("Steve", 0);
        BroadcastEngine engine = engine();
        engine.arm(START);

        tick(engine, START + SECOND);
        assertEquals(0, sink.sends(), "нужно двое, а онлайн один");

        facts.join("Alex", 0);
        tick(engine, START + 11 * SECOND);

        assertEquals(Arrays.asList("первое"), sink.texts());
    }

    @Test
    void aSetThatIsOffDoesNotWork() {
        SetBlock block = set(10, 0, "текст");
        block.enabled = false;
        put("tips", block);
        facts.join("Steve", 0);
        BroadcastEngine engine = engine();
        engine.arm(START);

        tick(engine, START + SECOND);

        assertEquals(0, sink.sends());
        assertTrue(
            engine.names()
                .isEmpty());
        assertEquals(
            Sent.Result.SET_OFF,
            engine.sendNow("tips")
                .result(),
            "имя в файле есть, но набор снят");
    }

    @Test
    void aSetWithAnUnknownSinkDoesNotWork() {
        SetBlock block = set(10, 0, "текст");
        block.sink = "chatt";
        put("tips", block);
        facts.join("Steve", 0);
        BroadcastEngine engine = engine();
        engine.arm(START);

        tick(engine, START + SECOND);

        assertEquals(0, sink.sends());
        assertFalse(
            engine.names()
                .contains("tips"));
    }

    @Test
    void aSetWithoutMessagesDoesNotWork() {
        SetBlock block = set(10, 0);
        put("tips", block);
        BroadcastEngine engine = engine();
        engine.arm(START);

        assertTrue(
            engine.names()
                .isEmpty());
    }

    @Test
    void aSetCanBeSentByHandWithoutMovingItsDeadline() {
        put("tips", set(10, 5, "первое", "второе"));
        facts.join("Steve", 0);
        BroadcastEngine engine = engine();
        engine.arm(START);
        long deadline = engine.deadlineOf("tips");

        Sent sent = engine.sendNow("tips");

        assertEquals(Sent.Result.DONE, sent.result());
        assertEquals(1, sent.recipients());
        assertEquals(Arrays.asList("первое"), sink.texts());
        assertEquals(deadline, engine.deadlineOf("tips"), "ручная отправка расписание не сбивает");

        tick(engine, START + 5 * SECOND);

        assertEquals(Arrays.asList("первое", "второе"), sink.texts(), "очередное срабатывание берёт следующее");
    }

    @Test
    void aTypoInTheSetNameIsAnswerableAsUnknown() {
        put("tips", set(10, 5, "текст"));
        BroadcastEngine engine = engine();
        engine.arm(START);

        assertEquals(
            Sent.Result.UNKNOWN_SET,
            engine.sendNow("tipz")
                .result());
    }

    @Test
    void withNobodyOnlineTheHandSendSaysSo() {
        put("tips", set(10, 5, "текст"));
        BroadcastEngine engine = engine();
        engine.arm(START);

        assertEquals(
            Sent.Result.NOBODY,
            engine.sendNow("tips")
                .result());
    }

    @Test
    void aPlainLineGoesToEverybodyThroughTheChatSink() {
        facts.join("Steve", 0);
        facts.join("Alex", -1);
        BroadcastEngine engine = engine();
        engine.arm(START);

        Sent sent = engine.sendPlain("&aвсем привет");

        assertEquals(2, sent.recipients());
        assertEquals(Arrays.asList("§aвсем привет"), sink.texts());
    }

    private void put(String name, SetBlock block) {
        file.sets.put(name, block);
    }

    private BroadcastEngine engine() {
        return new BroadcastEngine(() -> file, registry, new Conditions(registry), facts, new Random(1L), LOG);
    }

    private void tick(BroadcastEngine engine, long to) {
        facts.at(to);
        engine.tick(to - SECOND, to, snapshot());
    }

    private ServerSnapshot snapshot() {
        return ServerSnapshot.of(
            facts.online()
                .size(),
            facts.dimensions(),
            facts.tickMillis(),
            LocalDateTime.parse("2026-09-03T12:00"));
    }

    private static SetBlock set(int intervalSeconds, int firstDelaySeconds, String... messages) {
        SetBlock block = new SetBlock();
        block.intervalSeconds = intervalSeconds;
        block.firstDelaySeconds = firstDelaySeconds;
        block.prefix = "";
        block.when = new WhenBlock();
        block.messages = new ArrayList<>();
        for (String text : messages) {
            block.messages.add(MessageBlock.of(text));
        }
        return block;
    }
}
