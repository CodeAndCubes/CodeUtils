package com.mrleonardos.codeutils.internal.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.internal.broadcast.Rotation.Order;

class RotationTest {

    private static final Logger LOG = LogManager.getLogger("codeutils-test");

    private static final int SIZE = 3;

    @Test
    void cycleClosesTheCircle() {
        Rotation rotation = new Rotation(Order.CYCLE, new Random(1L));

        assertEquals(Arrays.asList(0, 1, 2, 0), take(rotation, 4));
    }

    @Test
    void shuffleGivesEveryMessageOnceBeforeItReshuffles() {
        Rotation rotation = new Rotation(Order.SHUFFLE, new Random(42L));

        List<Integer> circle = take(rotation, SIZE);

        assertEquals(SIZE, new TreeSet<>(circle).size(), () -> "внутри круга повторов нет: " + circle);

        List<Integer> next = take(rotation, SIZE);

        assertEquals(SIZE, new TreeSet<>(next).size(), () -> "новый круг снова без повторов: " + next);
    }

    @Test
    void randomIsAllowedToRepeatItself() {
        Rotation rotation = new Rotation(Order.RANDOM, new Random(7L));

        List<Integer> taken = take(rotation, 40);

        assertTrue(taken.size() > new TreeSet<>(taken).size(), () -> "сорок бросков из трёх дают повтор: " + taken);
    }

    @Test
    void skipMovesTheCyclePointerWithoutGivingAnything() {
        Rotation rotation = new Rotation(Order.CYCLE, new Random(1L));

        rotation.skip(SIZE);

        assertEquals(Arrays.asList(1, 2), take(rotation, 2));
    }

    @Test
    void skipEatsOneOfTheShuffledCircleToo() {
        Rotation skipping = new Rotation(Order.SHUFFLE, new Random(42L));
        Rotation taking = new Rotation(Order.SHUFFLE, new Random(42L));

        skipping.skip(SIZE);
        taking.next(SIZE);

        assertEquals(take(taking, 2), take(skipping, 2), "пропуск и выдача двигают круг одинаково");
    }

    @Test
    void anEmptySetGivesNothing() {
        Rotation rotation = new Rotation(Order.CYCLE, new Random(1L));

        assertEquals(Rotation.NOTHING, rotation.next(0));
    }

    @Test
    void aCycleSurvivesTheSetGettingShorter() {
        Rotation rotation = new Rotation(Order.CYCLE, new Random(1L));
        rotation.next(SIZE);
        rotation.next(SIZE);

        assertEquals(0, rotation.next(1), "два сообщения выкинули из файла, указатель садится на первое");
    }

    @Test
    void aShuffledBagIsRebuiltWhenTheSetChangesSize() {
        Rotation rotation = new Rotation(Order.SHUFFLE, new Random(42L));
        rotation.next(SIZE);

        assertEquals(0, rotation.next(1));
    }

    @Test
    void anUnknownOrderIsReadAsCycle() {
        assertEquals(Order.CYCLE, Rotation.order("по кругу", "тест", LOG));
        assertEquals(Order.CYCLE, Rotation.order(null, "тест", LOG));
        assertEquals(Order.CYCLE, Rotation.order("cycle", "тест", LOG));
        assertEquals(Order.RANDOM, Rotation.order(" RANDOM ", "тест", LOG));
        assertEquals(Order.SHUFFLE, Rotation.order("shuffle", "тест", LOG));
    }

    private static List<Integer> take(Rotation rotation, int times) {
        List<Integer> taken = new ArrayList<>();
        for (int shot = 0; shot < times; shot++) {
            taken.add(Integer.valueOf(rotation.next(SIZE)));
        }
        return taken;
    }
}
