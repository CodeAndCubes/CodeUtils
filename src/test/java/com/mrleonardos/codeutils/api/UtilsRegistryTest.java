package com.mrleonardos.codeutils.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.message.BroadcastSink;
import com.mrleonardos.codeutils.api.queue.QueueDecision;
import com.mrleonardos.codeutils.api.restart.RestartPhase;
import com.mrleonardos.codeutils.api.restart.RestartPlanView;
import com.mrleonardos.codeutils.api.restart.RestartStep;
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
    void everyOneOfTheSixRegistriesAnswersOnItsOwn() {
        UtilsRegistry registry = new UtilsRegistry();

        registry.addSink("discord", SINK);
        registry.addCondition("event", snapshot -> true);
        registry.addRunner("mine", ticket -> RunOutcome.ran(1));
        registry.addGuard("pets", entity -> true);
        registry.addStep(step("dump", RestartPhase.BEFORE_SAVE));
        registry.addPolicy("donors", (request, state) -> QueueDecision.let());

        assertTrue(
            registry.sink("discord")
                .isPresent());
        assertTrue(
            registry.condition("event")
                .isPresent());
        assertTrue(
            registry.runner("mine")
                .isPresent());
        assertTrue(
            registry.guard("pets")
                .isPresent());
        assertTrue(
            registry.policy("donors")
                .isPresent());
        assertEquals(Arrays.asList("dump"), registry.stepNames());
        assertFalse(
            registry.condition("discord")
                .isPresent(),
            "имена реестров не смешиваются");
        assertFalse(
            registry.guard("discord")
                .isPresent(),
            "имена реестров не смешиваются");
    }

    @Test
    void everyGuardIsAskedSoTheyAllComeOutTogether() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.addGuard("pets", entity -> true);
        registry.addGuard("shops", entity -> false);

        assertEquals(
            2,
            registry.guards()
                .size());
        assertEquals(Arrays.asList("pets", "shops"), registry.guardNames());
    }

    @Test
    void stepsComeOutByTheirOwnPhaseOnly() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.addStep(step("dump", RestartPhase.BEFORE_SAVE));
        registry.addStep(step("snapshot", RestartPhase.AFTER_SAVE));

        assertEquals(
            1,
            registry.steps(RestartPhase.BEFORE_SAVE)
                .size());
        assertEquals(
            "snapshot",
            registry.steps(RestartPhase.AFTER_SAVE)
                .get(0)
                .name());
        assertTrue(
            registry.steps(RestartPhase.BEFORE_DOOR)
                .isEmpty());
    }

    @Test
    void aStepWithoutAPhaseIsRefused() {
        UtilsRegistry registry = new UtilsRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.addStep(step("dump", null)));
    }

    private static RestartStep step(String name, RestartPhase phase) {
        return new RestartStep() {

            @Override
            public RestartPhase phase() {
                return phase;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public void run(RestartPlanView plan) {}
        };
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
