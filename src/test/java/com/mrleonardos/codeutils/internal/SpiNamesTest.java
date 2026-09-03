package com.mrleonardos.codeutils.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.UtilsRegistry;

class SpiNamesTest {

    private static final Logger LOG = LogManager.getLogger("codeutils-test");

    @Test
    void aSinkThatNobodyRegisteredIsNamedTogetherWithTheSetThatAskedForIt() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.addSink("chat", (recipients, message) -> {});

        SpiNames names = new SpiNames(registry, LOG).add(source(sinks("set tips", "chat", "set links", "chatt")));

        assertEquals(Collections.singletonMap("set links", "chatt"), names.unknownSinks());
    }

    @Test
    void aKnownSinkIsNotNamedAtAll() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.addSink("chat", (recipients, message) -> {});

        SpiNames names = new SpiNames(registry, LOG).add(source(sinks("set tips", "chat")));

        assertTrue(
            names.unknownSinks()
                .isEmpty());
    }

    @Test
    void everyUnknownConditionIsNamedOnceHoweverManyBlocksAskForIt() {
        UtilsRegistry registry = new UtilsRegistry();
        registry.addCondition("event", snapshot -> true);

        SpiNames names = new SpiNames(registry, LOG)
            .add(source(Collections.<String, String>emptyMap(), "event", "missing", "missing"))
            .add(source(Collections.<String, String>emptyMap(), "gone", "missing"));

        assertEquals(Arrays.asList("missing", "gone"), names.unknownConditions());
    }

    @Test
    void theResolvingPassSurvivesHavingNoSourcesAtAll() {
        SpiNames names = new SpiNames(new UtilsRegistry(), LOG);

        names.resolve();

        assertTrue(
            names.unknownSinks()
                .isEmpty());
        assertTrue(
            names.unknownConditions()
                .isEmpty());
    }

    @Test
    void bothAnswersAreReadOnly() {
        SpiNames names = new SpiNames(new UtilsRegistry(), LOG).add(source(sinks("set tips", "chatt"), "missing"));

        org.junit.jupiter.api.Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> names.unknownConditions()
                .add("чужое"));
        org.junit.jupiter.api.Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> names.unknownSinks()
                .put("чужое", "чужое"));
    }

    private static Map<String, String> sinks(String... pairs) {
        Map<String, String> named = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            named.put(pairs[index], pairs[index + 1]);
        }
        return named;
    }

    private static SpiNames.Source source(Map<String, String> sinks, String... conditions) {
        List<String> named = Arrays.asList(conditions);
        return new SpiNames.Source() {

            @Override
            public Map<String, String> sinks() {
                return sinks;
            }

            @Override
            public List<String> conditions() {
                return named;
            }
        };
    }
}
