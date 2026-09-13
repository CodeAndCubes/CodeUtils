package com.mrleonardos.codeutils.api.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SenderChoiceTest {

    @Test
    void theConsoleIsReadFromItsWord() {
        SenderChoice choice = SenderChoice.parse("console");

        assertEquals(SenderChoice.Kind.CONSOLE, choice.kind());
        assertTrue(choice.known());
        assertEquals("", choice.nick());
    }

    @Test
    void theCommandBlockIsReadFromItsWord() {
        assertEquals(
            SenderChoice.Kind.COMMAND_BLOCK,
            SenderChoice.parse(" CommandBlock ")
                .kind());
    }

    @Test
    void aPlayerIsReadTogetherWithTheNick() {
        SenderChoice choice = SenderChoice.parse("player:Steve");

        assertEquals(SenderChoice.Kind.PLAYER, choice.kind());
        assertEquals("Steve", choice.nick());
        assertTrue(choice.known());
    }

    @Test
    void aNickWithSpacesAroundItIsTrimmed() {
        assertEquals(
            "Steve",
            SenderChoice.parse("player: Steve ")
                .nick());
    }

    @Test
    void anUnknownWordFallsBackToTheConsoleAndSaysSo() {
        SenderChoice choice = SenderChoice.parse("робот");

        assertEquals(SenderChoice.Kind.CONSOLE, choice.kind());
        assertFalse(choice.known(), "подмену отправителя нужно назвать в логе");
        assertEquals("робот", choice.written());
    }

    @Test
    void aPlayerWithoutANickIsNotUnderstoodEither() {
        SenderChoice choice = SenderChoice.parse("player:");

        assertEquals(SenderChoice.Kind.CONSOLE, choice.kind());
        assertFalse(choice.known());
    }

    @Test
    void anEmptyValueIsNotUnderstoodEither() {
        assertFalse(
            SenderChoice.parse("")
                .known());
        assertFalse(
            SenderChoice.parse(null)
                .known());
        assertEquals(
            SenderChoice.Kind.CONSOLE,
            SenderChoice.parse(null)
                .kind());
    }

    @Test
    void aTicketCarriesTheJobTheCommandAndTheAttempt() {
        CommandTicket ticket = CommandTicket.of("night-clean", "save-all", SenderChoice.console());

        assertEquals("night-clean", ticket.job());
        assertEquals("save-all", ticket.command());
        assertEquals(1, ticket.attempt());
        assertEquals(
            2,
            ticket.next()
                .attempt());
        assertEquals(
            "save-all",
            ticket.next()
                .command());
    }

    @Test
    void anOutcomeWithoutASingleSuccessIsARefusal() {
        assertTrue(
            RunOutcome.ran(1)
                .successful());
        assertFalse(
            RunOutcome.ran(0)
                .successful());
        assertEquals(
            RunOutcome.NOTHING,
            RunOutcome.ran(0)
                .reason());
        assertEquals(
            RunOutcome.NOTHING,
            RunOutcome.ran(-1)
                .reason());
        assertEquals(
            RunOutcome.NO_PLAYER,
            RunOutcome.refused(RunOutcome.NO_PLAYER)
                .reason());
        assertEquals(
            3,
            RunOutcome.ran(3)
                .count());
    }
}
