package com.mrleonardos.codeutils.internal.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mrleonardos.codecore.api.command.ArgumentType;
import com.mrleonardos.codecore.api.command.CommandSender;

/**
 * Сторож на подсказки по Tab.
 *
 * <p>
 * {@code suggestions} в api ядра это метод по умолчанию, и реализация, написанная без
 * {@code @Override}, после смены подписи молча становится перегрузкой: сборка зелёная, подсказки
 * замолчали. Аннотацию сборкой не поймать, у неё {@code RetentionPolicy.SOURCE}, поэтому ловится сама
 * поломка: непустой список на непустом наборе имён.
 */
class UtilsArgumentsTest {

    private static final CommandSender NOBODY = null;

    private final UtilsArguments arguments = new UtilsArguments(
        () -> Arrays.asList("tips", "links"),
        () -> Arrays.asList("save", "night-clean"),
        () -> Arrays.asList("drops", "mobs"));

    @Test
    void theSetNameOffersEverySetOnAnEmptyWord() {
        List<String> offered = arguments.setName()
            .suggestions(NOBODY, "");

        assertFalse(offered.isEmpty(), "подсказки имён наборов замолчали");
        assertEquals(Arrays.asList("tips", "links"), offered);
    }

    @Test
    void theRuleNameOffersEveryRuleOnAnEmptyWord() {
        List<String> offered = arguments.ruleName()
            .suggestions(NOBODY, "");

        assertFalse(offered.isEmpty(), "подсказки имён правил очистки замолчали");
        assertEquals(Arrays.asList("drops", "mobs"), offered);
    }

    @Test
    void theJobNameOffersEveryJobOnAnEmptyWord() {
        List<String> offered = arguments.jobName()
            .suggestions(NOBODY, "");

        assertFalse(offered.isEmpty(), "подсказки имён заданий замолчали");
        assertEquals(Arrays.asList("save", "night-clean"), offered);
    }

    @Test
    void aStartedWordNarrowsTheOffer() {
        assertEquals(
            Collections.singletonList("tips"),
            arguments.setName()
                .suggestions(NOBODY, "ti"));
        assertEquals(
            Collections.singletonList("night-clean"),
            arguments.jobName()
                .suggestions(NOBODY, "ni"));
    }

    @Test
    void theOfferIgnoresTheCaseOfWhatIsTyped() {
        assertEquals(
            Collections.singletonList("tips"),
            arguments.setName()
                .suggestions(NOBODY, "TI"));
    }

    @Test
    void aWordThatMatchesNothingOffersNothing() {
        assertTrue(
            arguments.setName()
                .suggestions(NOBODY, "zzz")
                .isEmpty());
    }

    @Test
    void anEmptyListOfNamesOffersNothingAndDoesNotFall() {
        UtilsArguments empty = new UtilsArguments(
            Collections::emptyList,
            Collections::emptyList,
            Collections::emptyList);

        assertTrue(
            empty.setName()
                .suggestions(NOBODY, "")
                .isEmpty());
        assertTrue(
            empty.jobName()
                .suggestions(NOBODY, "")
                .isEmpty());
        assertTrue(
            empty.ruleName()
                .suggestions(NOBODY, "")
                .isEmpty());
    }

    @Test
    void bothTypesGiveTheWordBackAsItWasTyped() {
        ArgumentType<String> sets = arguments.setName();

        assertEquals("tips", sets.parse("tips"));
        assertEquals(
            "save",
            arguments.jobName()
                .parse("save"));
        assertFalse(sets.greedy(), "имя набора это одно слово, а не остаток строки");
    }
}
