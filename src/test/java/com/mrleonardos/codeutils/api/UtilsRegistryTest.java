package com.mrleonardos.codeutils.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.message.BroadcastSink;
import com.mrleonardos.codeutils.api.run.RunOutcome;

class UtilsRegistryTest {

    private static final BroadcastSink SINK = (recipients, message) -> {};

    @Test
    void aClaimMadeInTimeIsAcceptedAndFoundBackByItsName() {
        UtilsRegistry registry = new UtilsRegistry();

        registry.addSink("discord", SINK);

        assertTrue(
            registry.sink("discord")
                .isPresent());
        assertFalse(
            registry.sink("discrod")
                .isPresent());
        assertEquals(Arrays.asList("discord"), registry.sinkNames());
    }

    @Test
    void everyOneOfTheThreeRegistriesAnswersOnItsOwn() {
        UtilsRegistry registry = new UtilsRegistry();

        registry.addSink("discord", SINK);
        registry.addCondition("event", snapshot -> true);
        registry.addRunner("mine", ticket -> RunOutcome.ran(1));

        assertTrue(
            registry.sink("discord")
                .isPresent());
        assertTrue(
            registry.condition("event")
                .isPresent());
        assertTrue(
            registry.runner("mine")
                .isPresent());
        assertFalse(
            registry.condition("discord")
                .isPresent(),
            "имена реестров не смешиваются");
    }

    @Test
    void aClaimAfterTheFreezeIsRefusedWithTheNameOfThePhase() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.freeze();

        IllegalStateException refused = assertThrows(
            IllegalStateException.class,
            () -> registry.addSink("discord", SINK));

        assertTrue(
            refused.getMessage()
                .contains("init"),
            refused.getMessage());
        assertTrue(
            refused.getMessage()
                .contains("discord"),
            refused.getMessage());
        assertTrue(registry.frozen());
    }

    @Test
    void aNameThatIsAlreadyTakenIsRefused() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.addSink("discord", SINK);

        assertThrows(IllegalArgumentException.class, () -> registry.addSink("discord", (to, message) -> {}));
    }

    @Test
    void theSameClaimTwiceIsNotAQuarrel() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.addSink("discord", SINK);
        registry.addSink("discord", SINK);

        assertEquals(
            1,
            registry.sinkNames()
                .size());
    }

    @Test
    void aClaimWithoutANameIsRefused() {
        UtilsRegistry registry = new UtilsRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.addSink("  ", SINK));
        assertThrows(IllegalArgumentException.class, () -> registry.addSink(null, SINK));
    }

    @Test
    void aNameIsLookedUpWithoutItsStraySpaces() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.addSink(" discord ", SINK);

        assertTrue(
            registry.sink("discord")
                .isPresent());
    }
}
