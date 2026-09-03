package com.mrleonardos.codeutils.api.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QueueDecisionTest {

    @Test
    void aDecisionCarriesTheTierItWasMadeBy() {
        QueueDecision let = QueueDecision.let("vip");
        QueueDecision refused = QueueDecision.refuse("staff");

        assertTrue(let.allowed());
        assertEquals("vip", let.tier());
        assertFalse(refused.allowed());
        assertEquals("staff", refused.tier());
    }

    @Test
    void aPlayerWithoutATierGetsAnEmptyTierAndNotANullOne() {
        assertEquals(
            QueueDecision.NO_TIER,
            QueueDecision.let()
                .tier());
        assertEquals(
            QueueDecision.NO_TIER,
            QueueDecision.refuse()
                .tier());
        assertEquals(
            QueueDecision.NO_TIER,
            QueueDecision.let(null)
                .tier());
        assertEquals(
            QueueDecision.NO_TIER,
            QueueDecision.refuse("")
                .tier());
    }
}
