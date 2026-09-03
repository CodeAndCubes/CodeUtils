package com.mrleonardos.codeutils.internal.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class UtilsMessagesTest {

    private static final String RUSSIAN = "assets/codeutils/lang/ru_RU.lang";
    private static final String ENGLISH = "assets/codeutils/lang/en_US.lang";

    private static final String NODE_PREFIX = "codeutils.admin.";

    @Test
    void everyKeyOfTheCodeIsTranslatedInBothFiles() throws IOException {
        Set<String> keys = constantsOf(UtilsMessages.class);
        Set<String> russian = keysOf(RUSSIAN);
        Set<String> english = keysOf(ENGLISH);

        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            if (!russian.contains(key)) {
                missing.add("ru_RU: " + key);
            }
            if (!english.contains(key)) {
                missing.add("en_US: " + key);
            }
        }

        assertTrue(missing.isEmpty(), () -> "ключ из кода без перевода: " + missing);
    }

    @Test
    void bothFilesHoldTheSameKeys() throws IOException {
        assertEquals(new TreeSet<>(keysOf(RUSSIAN)), new TreeSet<>(keysOf(ENGLISH)));
    }

    @Test
    void everyKeyStartsWithTheNameOfTheMod() {
        for (String key : constantsOf(UtilsMessages.class)) {
            assertTrue(key.startsWith("codeutils."), key);
        }
    }

    @Test
    void thereAreSevenAdminNodesAndTheyAllShareOnePrefix() {
        Set<String> nodes = constantsOf(Nodes.class);

        assertEquals(7, nodes.size(), () -> "узлы прав: " + nodes);
        for (String node : nodes) {
            assertTrue(node.startsWith(NODE_PREFIX), node);
        }
        assertTrue(nodes.contains(Nodes.ADMIN_STATUS));
        assertTrue(nodes.contains(Nodes.ADMIN_RELOAD));
        assertTrue(nodes.contains(Nodes.ADMIN_BROADCAST));
        assertTrue(nodes.contains(Nodes.ADMIN_JOBS));
        assertTrue(nodes.contains(Nodes.ADMIN_CLEAN));
        assertTrue(nodes.contains(Nodes.ADMIN_RESTART));
        assertTrue(nodes.contains(Nodes.ADMIN_QUEUE));
    }

    private static Set<String> constantsOf(Class<?> type) {
        Set<String> found = new LinkedHashSet<>();
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) {
                continue;
            }
            try {
                found.add((String) field.get(null));
            } catch (IllegalAccessException unreachable) {
                throw new IllegalStateException(field.getName(), unreachable);
            }
        }
        return found;
    }

    private static Set<String> keysOf(String resource) throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        try (InputStream stream = UtilsMessagesTest.class.getClassLoader()
            .getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("файл перевода не найден: " + resource);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                int separator = line.indexOf('=');
                if (line.trim()
                    .isEmpty() || line.startsWith("#")
                    || separator <= 0) {
                    continue;
                }
                keys.add(
                    line.substring(0, separator)
                        .trim());
            }
        }
        return keys;
    }
}
