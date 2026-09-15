package com.mrleonardos.codeutils.internal.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.queue.QueueDecision;
import com.mrleonardos.codeutils.api.queue.QueueTicket;
import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.FakeTexts;
import com.mrleonardos.codeutils.internal.command.UtilsMessages;

class QueueGateTest {

    private static final Logger LOG = LogManager.getLogger("codeutils-test");

    private static final long SECOND = Clocks.MILLIS;

    private static final long START = 1_700_000_000_000L;

    private final UtilsRegistry registry = new UtilsRegistry();
    private final FakeRights rights = new FakeRights();
    private final QueueFile file = new QueueFile();

    private final UUID plain = UUID.nameUUIDFromBytes("Plain".getBytes());
    private final UUID other = UUID.nameUUIDFromBytes("Other".getBytes());
    private final UUID vip = UUID.nameUUIDFromBytes("Vip".getBytes());
    private final UUID staff = UUID.nameUUIDFromBytes("Staff".getBytes());

    QueueGateTest() {
        registry.addPolicy(QueueFile.BY_PERMISSION, new ByPermission());
        rights.give(vip, "codeutils.slots.vip");
        rights.give(staff, "codeutils.slots.staff");
    }

    @Test
    void aTierLetsItsOwnerInOverTheCommonCap() {
        QueueGate gate = gate(63);

        assertFalse(
            gate.decide(plain, "Plain", START)
                .allowed(),
            "общий потолок шестьдесят, онлайн шестьдесят три");
        assertTrue(
            gate.decide(vip, "Vip", START)
                .allowed(),
            "у ступени vip семьдесят слотов");
    }

    @Test
    void everySeatOfTheTierIsCountedToo() {
        QueueGate gate = gate(70);

        assertFalse(
            gate.decide(vip, "Vip", START)
                .allowed(),
            "семьдесят занято, ступень vip кончилась");
        assertTrue(
            gate.decide(staff, "Staff", START)
                .allowed(),
            "у ступени staff восемьдесят");
    }

    @Test
    void theNumberInTheQueueDoesNotMoveWhileNobodyLeaves() {
        QueueGate gate = gate(60);
        assertEquals(
            1,
            gate.decide(plain, "Plain", START)
                .place());
        assertEquals(
            2,
            gate.decide(other, "Other", START + SECOND)
                .place());

        QueueGate.Answer again = gate.decide(other, "Other", START + 10 * SECOND);

        assertEquals(2, again.place(), "повторный стук номера не меняет");
        assertEquals(
            START + SECOND,
            gate.queue(START + 10 * SECOND)
                .get(1)
                .since(),
            "время первого стука прежнее");
    }

    @Test
    void theHigherTierIsServedBeforeTheEarlierKnock() {
        QueueGate gate = gate(80);
        gate.decide(plain, "Plain", START);
        gate.decide(staff, "Staff", START + 30 * SECOND);

        QueueTicket head = gate.queue(START + 31 * SECOND)
            .get(0);

        assertEquals("Staff", head.name());
        assertEquals(QueueFile.STAFF, head.tier());
    }

    @Test
    void aTicketThatIsNotKnockedOnDiesOfOldAge() {
        file.queue.ticketSeconds = 60;
        QueueGate gate = gate(60);
        gate.decide(plain, "Plain", START);
        gate.decide(other, "Other", START + SECOND);

        assertEquals(
            2,
            gate.queue(START + 30 * SECOND)
                .size());

        QueueGate.Answer late = gate.decide(plain, "Plain", START + 120 * SECOND);

        assertEquals(1, late.place(), "билет протух, стук ставит игрока заново");
        assertEquals(
            1,
            gate.queue(START + 120 * SECOND)
                .size(),
            "билет соседа протух тоже");
    }

    @Test
    void theFreedSlotWaitsForTheHeadOfTheQueue() {
        QueueGate gate = gate(60);
        gate.decide(plain, "Plain", START);
        gate.decide(other, "Other", START + SECOND);
        gate.left(START + 10 * SECOND);

        assertFalse(
            gate.decide(other, "Other", START + 12 * SECOND)
                .allowed(),
            "слот придержан за головой очереди, а голова это Plain");
        assertTrue(
            gate.decide(plain, "Plain", START + 13 * SECOND)
                .allowed(),
            "голова успела внутри holdSeconds");
    }

    @Test
    void aFreedSlotGoesToAnybodyOnceTheHoldIsOver() {
        file.queue.holdSeconds = 15;
        QueueGate gate = gate(60);
        gate.decide(plain, "Plain", START);
        gate.left(START + 10 * SECOND);

        assertTrue(
            gate.decide(other, "Other", START + 30 * SECOND)
                .allowed(),
            "голова не постучалась за пятнадцать секунд");
    }

    @Test
    void theQueueThatIsOffLeavesTheCapsAndTakesTheNumbersAway() {
        file.queue.enabled = false;
        QueueGate gate = gate(60);

        QueueGate.Answer answer = gate.decide(plain, "Plain", START);

        assertFalse(answer.allowed(), "потолок работает и без очереди");
        assertEquals(0, answer.place(), "номера нет");
        assertTrue(
            gate.queue(START)
                .isEmpty(),
            "билет не заведён");
    }

    @Test
    void aPlayerWhoGotInLosesHisTicketAndTheHeldSlot() {
        QueueGate gate = gate(60);
        gate.decide(plain, "Plain", START);
        gate.left(START + SECOND);

        assertEquals(59, gate.online());

        gate.joined(plain);

        assertEquals(60, gate.online());
        assertTrue(
            gate.queue(START + 2 * SECOND)
                .isEmpty(),
            "вошедший из очереди уходит");
    }

    @Test
    void aPolicyOfAnotherModDecidesInsteadOfTheBuiltInOne() {
        registry.addPolicy("donors", (request, state) -> QueueDecision.let("donor"));
        file.queue.policy = "donors";
        QueueGate gate = gate(1000);

        QueueGate.Answer answer = gate.decide(plain, "Plain", START);

        assertTrue(answer.allowed(), "чужая политика пустила поверх всех потолков");
        assertEquals("donor", answer.tier());
    }

    @Test
    void anUnknownPolicyLeavesTheBaseCapAndTakesTheTiersAway() {
        file.queue.policy = "by-permissionn";
        QueueGate gate = gate(63);

        assertFalse(
            gate.decide(plain, "Plain", START)
                .allowed(),
            "общий потолок работает и с опечаткой в имени политики");
        assertFalse(
            gate.decide(vip, "Vip", START)
                .allowed(),
            "ступени без своей политики не работают, все считаются по slots.base");

        QueueGate open = gate(10);

        assertTrue(
            open.decide(plain, "Plain", START)
                .allowed(),
            "ниже общего потолка вход свободен");
    }

    @Test
    void aPolicyIsAskedForTheBaseCapWhenNoTierFits() {
        QueueGate gate = gate(59);

        assertTrue(
            gate.decide(plain, "Plain", START)
                .allowed());
        assertEquals(
            "",
            gate.decide(plain, "Plain", START)
                .tier(),
            "у игрока без ступени её и нет");
    }

    @Test
    void theRefusalCarriesThePlaceAndTheRetryHint() {
        file.queue.retryHintSeconds = 20;
        QueueGate gate = gate(60);
        gate.decide(other, "Other", START);

        QueueGate.Answer answer = gate.decide(plain, "Plain", START + SECOND);

        assertEquals(
            UtilsMessages.QUEUE_REFUSED + "[2, 20]",
            gate.refusalOf(answer, new FakeTexts()),
            "номер в очереди и подсказка повторного стука уходят в текст отказа");
    }

    @Test
    void theRefusalWithoutAPlaceSpeaksOfAFullServerOnly() {
        file.queue.enabled = false;
        QueueGate gate = gate(60);

        QueueGate.Answer answer = gate.decide(plain, "Plain", START);

        assertEquals(UtilsMessages.QUEUE_FULL, gate.refusalOf(answer, new FakeTexts()));
    }

    @Test
    void parallelJoinsAreAllCounted() throws InterruptedException {
        QueueGate gate = gate(0);
        int threads = 8;
        int each = 500;
        Thread[] joiners = new Thread[threads];
        CountDownLatch go = new CountDownLatch(1);
        for (int index = 0; index < threads; index++) {
            UUID who = UUID.nameUUIDFromBytes(("Joiner" + index).getBytes());
            joiners[index] = new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException stopped) {
                    Thread.currentThread()
                        .interrupt();
                }
                for (int knock = 0; knock < each; knock++) {
                    gate.joined(who);
                }
            }, "joiner-" + index);
            joiners[index].start();
        }
        go.countDown();
        for (Thread joiner : joiners) {
            joiner.join();
        }

        assertEquals(threads * each, gate.online(), "вход из многих потоков не теряет ни одного счёта");
    }

    private QueueGate gate(int online) {
        QueueGate gate = new QueueGate(() -> file, registry, rights, LOG);
        gate.arm(online, 200);
        return gate;
    }

}
