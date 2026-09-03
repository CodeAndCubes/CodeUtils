package com.mrleonardos.codeutils.internal.clean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.UtilsRegistry;

class GuardsTest {

    private final UtilsRegistry registry = new UtilsRegistry();

    @Test
    void everyOneOfTheEightGuardsStopsTheCleanupOnItsOwn() {
        Guards guards = Guards.of(Collections.<String>emptyList(), registry);

        assertEquals(
            8,
            Guards.builtIn()
                .size());
        for (String guard : Guards.builtIn()) {
            assertTrue(
                guards.protects(
                    FakeEntity.wolf()
                        .guarded(guard)),
                () -> "защита " + guard + " обязана удержать сущность");
        }
        assertFalse(guards.protects(FakeEntity.wolf()), "сущность без единого признака не защищена");
    }

    @Test
    void aGuardIsTakenOffOnlyByItsOwnNameAndTheOthersKeepWorking() {
        Guards guards = Guards.of(Arrays.asList(Guards.NAMED), registry);

        assertFalse(
            guards.protects(
                FakeEntity.wolf()
                    .guarded(Guards.NAMED)),
            "названная защита снята");
        assertTrue(
            guards.protects(
                FakeEntity.wolf()
                    .guarded(Guards.TAMED)),
            "остальные семь продолжают работать");
        assertTrue(guards.ignores(Guards.NAMED));
    }

    @Test
    void aGuardOfAnotherModIsAskedWithoutAnySettingAtAll() {
        registry.addGuard("pets", entity -> "Wolf".equals(entity.type()));
        Guards guards = Guards.of(Collections.<String>emptyList(), registry);

        assertTrue(guards.protects(FakeEntity.wolf()), "чужая защита спрашивается на каждом проходе");
        assertFalse(guards.protects(FakeEntity.zombie()));
    }

    @Test
    void aGuardOfAnotherModIsTakenOffByTheSameKey() {
        registry.addGuard("pets", entity -> true);
        Guards guards = Guards.of(Arrays.asList("pets"), registry);

        assertFalse(guards.protects(FakeEntity.wolf()), "владелец сервера не оказывается запертым");
    }

    @Test
    void aNameInIgnoreGuardsThatBelongsToNoGuardIsNamedBack() {
        Guards guards = Guards.of(Arrays.asList("named", "petz"), registry);

        assertEquals(Arrays.asList("petz"), guards.unknown(registry));
    }

    @Test
    void theNameOfAGuardIsReadWithoutCareForItsCase() {
        Guards guards = Guards.of(Arrays.asList("NAMED", " tamed "), registry);

        assertTrue(guards.ignores(Guards.NAMED));
        assertTrue(guards.ignores(Guards.TAMED));
        assertTrue(
            guards.unknown(registry)
                .isEmpty());
    }
}
