package com.mrleonardos.codeutils.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.when.ServerSnapshot;
import com.mrleonardos.codeutils.api.when.When;

class ConditionsTest {

    private static final ServerSnapshot NOW = ServerSnapshot
        .of(3, Collections.singleton(Integer.valueOf(0)), 20.0D, LocalDateTime.parse("2026-09-03T12:00"));

    @Test
    void aKnownConditionIsAskedAndItsAnswerCounts() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.addCondition("event", snapshot -> false);
        Conditions conditions = new Conditions(registry);

        When when = When.builder()
            .custom("event")
            .build();

        assertFalse(conditions.allows(when, NOW));
    }

    @Test
    void aKnownConditionThatAllowsLetsTheBlockThrough() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.addCondition("event", snapshot -> true);
        Conditions conditions = new Conditions(registry);

        assertTrue(
            conditions.allows(
                When.builder()
                    .custom("event")
                    .build(),
                NOW));
    }

    @Test
    void anUnknownNameCountsAsAConditionThatIsNotMet() {
        Conditions conditions = new Conditions(new UtilsRegistry());

        assertFalse(
            conditions.allows(
                When.builder()
                    .custom("nobody-registered-this")
                    .build(),
                NOW));
    }

    @Test
    void builtInKeysAreCheckedBeforeTheForeignOnes() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.addCondition("event", snapshot -> true);
        Conditions conditions = new Conditions(registry);

        When when = When.builder()
            .minPlayers(10)
            .custom("event")
            .build();

        assertFalse(conditions.allows(when, NOW), "чужое условие разрешает, а игроков всё равно мало");
    }

    @Test
    void everyUnknownNameIsNamedOnceAndKnownOnesAreNot() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.addCondition("event", snapshot -> true);
        Conditions conditions = new Conditions(registry);

        List<When> blocks = Arrays.asList(
            When.builder()
                .custom("event")
                .custom("missing")
                .build(),
            When.builder()
                .custom("missing")
                .build(),
            When.builder()
                .custom("gone")
                .build());

        assertEquals(Arrays.asList("missing", "gone"), conditions.unknown(blocks));
    }
}
