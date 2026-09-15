package com.mrleonardos.codeutils.internal.clean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.clean.EntityKind;

class EntityMatcherTest {

    private static final Logger LOG = LogManager.getLogger("codeutils-test");

    @Test
    void anExactNameTakesThatEntityAndNoOther() {
        EntityMatcher matcher = of("Item");

        assertTrue(matcher.matches(FakeEntity.item()));
        assertFalse(matcher.matches(FakeEntity.zombie()));
    }

    @Test
    void aGroupTakesEveryEntityOfItsKind() {
        EntityMatcher matcher = of("@hostile");

        assertTrue(matcher.matches(FakeEntity.zombie()));
        assertTrue(matcher.matches(FakeEntity.of("Creeper", EntityKind.HOSTILE)));
        assertFalse(matcher.matches(FakeEntity.wolf()));
    }

    @Test
    void aTailWithAStarTakesEveryNameThatStartsWithIt() {
        EntityMatcher matcher = of("Thermal*");

        assertTrue(matcher.matches(FakeEntity.of("ThermalGolem", EntityKind.OTHER)));
        assertTrue(matcher.matches(FakeEntity.of("Thermal", EntityKind.OTHER)));
        assertFalse(matcher.matches(FakeEntity.of("Golem", EntityKind.OTHER)));
    }

    @Test
    void aMinusTakesTheEntityOutOfWhatWasAlreadyChosen() {
        EntityMatcher matcher = of("@hostile", "-Wither");

        assertTrue(matcher.matches(FakeEntity.zombie()));
        assertFalse(matcher.matches(FakeEntity.of("Wither", EntityKind.HOSTILE)));
    }

    @Test
    void theListIsReadFromLeftToRightSoOrderDecides() {
        EntityMatcher minusFirst = of("-Zombie", "@hostile");

        assertTrue(minusFirst.matches(FakeEntity.zombie()), "минус вычел пустое место, группа взяла зомби назад");

        EntityMatcher groupFirst = of("@hostile", "-Zombie");

        assertFalse(groupFirst.matches(FakeEntity.zombie()));
    }

    @Test
    void theGroupOfEverythingLeavesOnlyPlayersOut() {
        EntityMatcher matcher = of("@all");

        assertTrue(matcher.matches(FakeEntity.item()));
        assertTrue(matcher.matches(FakeEntity.of("Steve", EntityKind.OTHER)));
        assertFalse(matcher.matches(FakeEntity.of("Steve", EntityKind.PLAYER)));
    }

    @Test
    void anUnknownGroupIsSkippedAndTheRestOfTheListWorks() {
        EntityMatcher matcher = of("@animals", "@item");

        assertTrue(matcher.matches(FakeEntity.item()));
        assertFalse(matcher.matches(FakeEntity.wolf()));
    }

    @Test
    void anEmptyListMatchesNothing() {
        assertTrue(
            EntityMatcher.of(Collections.<String>emptyList(), "test", LOG)
                .empty());
        assertFalse(
            EntityMatcher.of(Collections.<String>emptyList(), "test", LOG)
                .matches(FakeEntity.item()));
    }

    @Test
    void aNameIsMatchedWithoutCareForItsCase() {
        assertTrue(of("item").matches(FakeEntity.item()));
        assertTrue(of("ITEM").matches(FakeEntity.item()));
        assertTrue(of("thermal*").matches(FakeEntity.of("ThermalGolem", EntityKind.OTHER)));
    }

    private static EntityMatcher of(String... types) {
        List<String> written = Arrays.asList(types);
        return EntityMatcher.of(written, "test", LOG);
    }
}
