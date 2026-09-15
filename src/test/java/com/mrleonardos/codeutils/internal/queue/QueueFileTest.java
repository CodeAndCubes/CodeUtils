package com.mrleonardos.codeutils.internal.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mrleonardos.codecore.api.config.ConfigFile;
import com.mrleonardos.codecore.api.config.ConfigFormat;
import com.mrleonardos.codecore.api.config.ConfigRoles;
import com.mrleonardos.codeutils.TestConfigs;
import com.mrleonardos.codeutils.api.queue.QueueDecision;
import com.mrleonardos.codeutils.internal.UtilsSettings;
import com.mrleonardos.codeutils.internal.queue.QueueFile.Tier;

class QueueFileTest {

    @Test
    void theFileSitsBesideTheOtherSettings() {
        assertEquals(
            QueueFile.FILE,
            QueueFile.spec()
                .name());
        assertEquals(
            UtilsSettings.MODID,
            QueueFile.spec()
                .modid());
        assertEquals(
            ConfigRoles.UTILS,
            QueueFile.spec()
                .role());
        assertEquals(
            ConfigFormat.TOML,
            QueueFile.spec()
                .format());
    }

    @Test
    void theFactoryFileHoldsTheTwoTiersOfTheDesign() {
        QueueFile file = new QueueFile();

        assertEquals(QueueFile.DEFAULT_BASE_SLOTS, file.slots.base);
        assertEquals(
            Arrays.asList(QueueFile.VIP, QueueFile.STAFF),
            new java.util.ArrayList<>(file.slots.tiers.keySet()));
        assertEquals("codeutils.slots.vip", file.slots.tiers.get(QueueFile.VIP).node);
        assertEquals(70, file.slots.tiers.get(QueueFile.VIP).slots);
        assertEquals(80, file.slots.tiers.get(QueueFile.STAFF).slots);
        assertTrue(file.queue.enabled);
        assertEquals(QueueFile.BY_PERMISSION, file.queue.policy);
        assertEquals(QueueFile.DEFAULT_TICKET_SECONDS, file.queue.ticketSeconds);
        assertEquals(QueueFile.DEFAULT_HOLD_SECONDS, file.queue.holdSeconds);
        assertEquals(QueueFile.DEFAULT_RETRY_HINT_SECONDS, file.queue.retryHintSeconds);
    }

    @Test
    void theFactoryFileIsReadBackIntoTheSameValues(@TempDir Path root) {
        ConfigFile<QueueFile> opened = TestConfigs.of(root.resolve("config"))
            .open(QueueFile.spec());
        QueueFile written = opened.get();

        opened.reload();
        QueueFile again = opened.get();

        assertEquals(written.slots.base, again.slots.base);
        assertEquals(written.slots.tiers.keySet(), again.slots.tiers.keySet());
        assertEquals(written.slots.tiers.get(QueueFile.VIP).slots, again.slots.tiers.get(QueueFile.VIP).slots);
        assertEquals(written.slots.tiers.get(QueueFile.VIP).node, again.slots.tiers.get(QueueFile.VIP).node);
        assertEquals(written.queue.enabled, again.queue.enabled);
        assertEquals(written.queue.policy, again.queue.policy);
        assertEquals(written.queue.ticketSeconds, again.queue.ticketSeconds);
        assertEquals(written.queue.holdSeconds, again.queue.holdSeconds);
        assertEquals(written.queue.retryHintSeconds, again.queue.retryHintSeconds);
    }

    @Test
    void theWrittenFileLooksLikeTheDesignSaysItShould(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.of(config)
            .open(QueueFile.spec());

        String text = TestConfigs.read(
            TestConfigs.utils(config)
                .resolve(QueueFile.FILE_NAME));

        assertTrue(text.contains("schemaVersion = 1"), text);
        assertTrue(text.contains("[slots]"), text);
        assertTrue(text.contains("base = 60"), text);
        assertTrue(text.contains("[slots.tiers.vip]"), text);
        assertTrue(text.contains("[slots.tiers.staff]"), text);
        assertTrue(text.contains("[queue]"), text);
        assertTrue(text.contains("policy = \"by-permission\""), text);
        assertTrue(
            text.contains("max-players ставится выше самой верхней ступени"),
            () -> "шапка файла на месте:\n" + text);
    }

    @Test
    void aHandWrittenTierIsReadAndTakesItsPlaceBySlots(@TempDir Path root) {
        Path config = root.resolve("config");
        TestConfigs.write(
            TestConfigs.utils(config)
                .resolve(QueueFile.FILE_NAME),
            "schemaVersion = 1",
            "[slots]",
            "base = 40",
            "[slots.tiers.friend]",
            "node = \"server.friend\"",
            "slots = 45",
            "[slots.tiers.donor]",
            "node = \"server.donor\"",
            "slots = 90",
            "[queue]",
            "enabled = false",
            "policy = \"donors\"",
            "ticketSeconds = 30",
            "holdSeconds = 5",
            "retryHintSeconds = 10");

        QueueFile file = TestConfigs.of(config)
            .open(QueueFile.spec())
            .get();
        SlotRules rules = SlotRules.of(file);

        assertEquals(40, rules.base());
        assertEquals(Arrays.asList("donor", "friend"), rules.names(), "старшая ступень идёт первой");
        assertEquals(90, rules.topSlots());
        assertEquals("server.donor", rules.nodeOf("donor"));
        assertFalse(file.queue.enabled);
        assertEquals("donors", file.queue.policy);
        assertEquals(30, file.queue.ticketSeconds);
        assertEquals(5, file.queue.holdSeconds);
        assertEquals(10, file.queue.retryHintSeconds);
    }

    @Test
    void aTierWithoutSlotsIsNoTierAtAll() {
        QueueFile file = new QueueFile();
        file.slots.tiers = new java.util.LinkedHashMap<>();
        file.slots.tiers.put("broken", new Tier("server.broken", 0));

        SlotRules rules = SlotRules.of(file);

        assertTrue(
            rules.names()
                .isEmpty(),
            "ступень без слотов в лестницу не встаёт");
        assertEquals(
            QueueDecision.NO_TIER,
            rules.tierOf(UUID.randomUUID(), (player, node) -> true),
            "у ступени без слотов узла не спрашивают");
    }
}
