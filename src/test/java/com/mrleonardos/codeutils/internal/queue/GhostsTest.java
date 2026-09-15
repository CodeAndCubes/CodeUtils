package com.mrleonardos.codeutils.internal.queue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class GhostsTest {

    private final Ghosts ghosts = new Ghosts();

    private final UUID player = UUID.nameUUIDFromBytes("Plain".getBytes());

    @Test
    void aRefusedPlayerKeepsHisJoinAndLeaveOutOfTheCounter() {
        ghosts.refuse(player);

        assertFalse(ghosts.countsJoin(player), "призрак входа счётчик не поднимает");
        assertFalse(ghosts.countsLeave(player), "призрак выхода счётчик не опускает");
        assertTrue(ghosts.countsJoin(player), "после выхода призрака записи больше нет");
    }

    @Test
    void anAdmissionWipesTheRecordBeforeTheJoinArrives() {
        ghosts.refuse(player);

        ghosts.admit(player);

        assertTrue(ghosts.countsJoin(player), "пущенный игрок считается обычным входом");
    }

    @Test
    void aPlayerWithoutARecordCountsLikeEverybodyElse() {
        assertTrue(ghosts.countsJoin(player));
        assertTrue(ghosts.countsLeave(player));
        assertTrue(ghosts.countsJoin(null));
        assertTrue(ghosts.countsLeave(null));
    }
}
