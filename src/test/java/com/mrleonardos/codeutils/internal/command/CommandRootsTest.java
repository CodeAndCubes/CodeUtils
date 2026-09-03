package com.mrleonardos.codeutils.internal.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.mrleonardos.codecore.api.command.CommandNode;
import com.mrleonardos.codecore.api.config.ConfigFormat;
import com.mrleonardos.codecore.api.config.ConfigRoles;
import com.mrleonardos.codeutils.internal.UtilsSettings;

class CommandRootsTest {

    private static final Logger LOG = LogManager.getLogger("codeutils-test");

    @Test
    void theFreshFileHoldsThreeRootsWithTheirFactoryAliases() {
        CommandRoots book = new CommandRoots();

        assertEquals(
            Arrays.asList(CommandRoots.CODEUTILS, CommandRoots.RESTART, CommandRoots.BROADCAST),
            new ArrayList<>(book.commands.keySet()));
        for (Map.Entry<String, CommandRoots.Entry> entry : book.commands.entrySet()) {
            assertTrue(entry.getValue().enabled, entry.getKey());
        }
        assertEquals(Collections.singletonList("cu"), book.commands.get(CommandRoots.CODEUTILS).aliases);
        assertEquals(Collections.singletonList("bc"), book.commands.get(CommandRoots.BROADCAST).aliases);
        assertTrue(book.commands.get(CommandRoots.RESTART).aliases.isEmpty());
    }

    @Test
    void aRootTurnedOffIsNotRegisteredAtAll() {
        CommandRoots book = new CommandRoots();
        book.commands.get(CommandRoots.RESTART).enabled = false;

        List<CommandNode> chosen = book.chosen(roots(CommandRoots.CODEUTILS, CommandRoots.RESTART), LOG);

        assertEquals(Collections.singletonList(CommandRoots.CODEUTILS), names(chosen));
    }

    @Test
    void ownAliasesReplaceTheFactoryOnes() {
        CommandRoots book = new CommandRoots();
        book.commands.get(CommandRoots.BROADCAST).aliases = new ArrayList<>(Collections.singletonList("say-all"));

        List<CommandNode> chosen = book.chosen(roots(CommandRoots.BROADCAST), LOG);

        assertEquals(
            Collections.singletonList("say-all"),
            chosen.get(0)
                .aliases());
        assertFalse(
            chosen.get(0)
                .matches("bc"),
            "заводской алиас после замены больше не отвечает");
        assertTrue(
            chosen.get(0)
                .matches("say-all"));
    }

    @Test
    void anEmptyListLeavesTheRootWithoutAliases() {
        CommandRoots book = new CommandRoots();
        book.commands.get(CommandRoots.CODEUTILS).aliases = new ArrayList<>();

        List<CommandNode> chosen = book.chosen(roots(CommandRoots.CODEUTILS), LOG);

        assertTrue(
            chosen.get(0)
                .aliases()
                .isEmpty());
        assertTrue(
            chosen.get(0)
                .matches(CommandRoots.CODEUTILS));
    }

    @Test
    void aMissingAliasFieldFallsBackToTheFactoryList() {
        CommandRoots book = new CommandRoots();
        book.commands.get(CommandRoots.CODEUTILS).aliases = null;

        List<CommandNode> chosen = book.chosen(roots(CommandRoots.CODEUTILS), LOG);

        assertEquals(
            Collections.singletonList("cu"),
            chosen.get(0)
                .aliases());
    }

    @Test
    void aTypoInTheRootNameSwitchesOffNothing() {
        CommandRoots book = new CommandRoots();
        book.commands.put("codeutilz", new CommandRoots.Entry(false));

        List<String> unknown = book.unknownRoots();
        List<CommandNode> chosen = book.chosen(roots(CommandRoots.CODEUTILS), LOG);

        assertEquals(Collections.singletonList("codeutilz"), unknown);
        assertEquals(Collections.singletonList(CommandRoots.CODEUTILS), names(chosen));
    }

    @Test
    void aRootMissingFromTheFileIsWrittenBackWithFactoryValues() {
        CommandRoots book = new CommandRoots();
        book.commands.remove(CommandRoots.CODEUTILS);

        assertFalse(book.filledIn(), "до выбора дописывать нечего");

        List<CommandNode> chosen = book.chosen(roots(CommandRoots.CODEUTILS), LOG);

        assertEquals(Collections.singletonList(CommandRoots.CODEUTILS), names(chosen));
        assertTrue(book.commands.get(CommandRoots.CODEUTILS).enabled);
        assertTrue(book.filledIn(), "добавленную запись нужно сохранить на диск, иначе править нечего");
    }

    @Test
    void aFileThatNeedsNothingIsNotMarkedForSaving() {
        CommandRoots book = new CommandRoots();

        book.chosen(roots(CommandRoots.CODEUTILS, CommandRoots.BROADCAST), LOG);

        assertFalse(book.filledIn());
    }

    @Test
    void theFileSitsBesideTheOtherSettings() {
        assertEquals(
            CommandRoots.FILE,
            CommandRoots.spec()
                .name());
        assertEquals(
            UtilsSettings.MODID,
            CommandRoots.spec()
                .modid());
        assertEquals(
            ConfigRoles.UTILS,
            CommandRoots.spec()
                .role());
        assertEquals(
            ConfigFormat.TOML,
            CommandRoots.spec()
                .format());
    }

    private static List<CommandNode> roots(String... names) {
        List<CommandNode> roots = new ArrayList<>();
        for (String name : names) {
            roots.add(CommandNode.literal(name));
        }
        return roots;
    }

    private static List<String> names(List<CommandNode> nodes) {
        List<String> names = new ArrayList<>();
        for (CommandNode node : nodes) {
            names.add(node.name());
        }
        return names;
    }
}
