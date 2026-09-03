package com.mrleonardos.codeutils.api.when;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

class WhenTest {

    private static final Set<Integer> OVERWORLD = Collections.singleton(Integer.valueOf(0));

    @Test
    void anEmptyBlockAllowsEverything() {
        assertTrue(
            When.always()
                .empty());
        assertTrue(
            When.always()
                .allows(snapshot(0, 20.0D, "2026-09-03T04:00")));
    }

    @Test
    void keysAreJoinedByAnd() {
        When when = When.builder()
            .minPlayers(1)
            .maxTickMillis(50)
            .build();

        assertTrue(when.allows(snapshot(1, 20.0D, "2026-09-03T04:00")));
        assertFalse(when.allows(snapshot(0, 20.0D, "2026-09-03T04:00")), "мало игроков");
        assertFalse(when.allows(snapshot(1, 80.0D, "2026-09-03T04:00")), "тик длиннее потолка");
    }

    @Test
    void theCeilingOnPlayersWorksToo() {
        When when = When.builder()
            .maxPlayers(2)
            .build();

        assertTrue(when.allows(snapshot(2, 20.0D, "2026-09-03T04:00")));
        assertFalse(when.allows(snapshot(3, 20.0D, "2026-09-03T04:00")));
    }

    @Test
    void aWindowThroughMidnightCoversBothSides() {
        When when = When.builder()
            .window(23 * 60, 2 * 60)
            .build();

        assertTrue(when.allows(snapshot(0, 20.0D, "2026-09-03T00:30")), "половина первого ночи внутри окна");
        assertTrue(when.allows(snapshot(0, 20.0D, "2026-09-03T23:00")), "начало окна включено");
        assertFalse(when.allows(snapshot(0, 20.0D, "2026-09-03T02:00")), "конец окна не включён");
        assertFalse(when.allows(snapshot(0, 20.0D, "2026-09-03T12:00")));
    }

    @Test
    void anOrdinaryWindowStaysInsideTheDay() {
        When when = When.builder()
            .window(9 * 60, 18 * 60)
            .build();

        assertTrue(when.allows(snapshot(0, 20.0D, "2026-09-03T09:00")));
        assertTrue(when.allows(snapshot(0, 20.0D, "2026-09-03T17:59")));
        assertFalse(when.allows(snapshot(0, 20.0D, "2026-09-03T18:00")));
        assertFalse(when.allows(snapshot(0, 20.0D, "2026-09-03T08:59")));
    }

    @Test
    void aListOfDaysKeepsOutTheOtherDays() {
        When when = When.builder()
            .day(DayOfWeek.THURSDAY)
            .build();

        assertTrue(when.allows(snapshot(0, 20.0D, "2026-09-03T12:00")), "3 сентября 2026 это четверг");
        assertFalse(when.allows(snapshot(0, 20.0D, "2026-09-04T12:00")));
    }

    @Test
    void customNamesAreKeptWithoutRepeats() {
        When when = When.builder()
            .custom("event")
            .custom("event")
            .custom("")
            .build();

        assertTrue(
            when.custom()
                .contains("event"));
        assertTrue(
            when.custom()
                .size() == 1,
            () -> "повторы и пустые имена в список не идут: " + when.custom());
        assertFalse(when.empty(), "блок с чужим условием не пустой");
    }

    private static ServerSnapshot snapshot(int players, double tickMillis, String time) {
        return ServerSnapshot.of(players, OVERWORLD, tickMillis, LocalDateTime.parse(time));
    }
}
