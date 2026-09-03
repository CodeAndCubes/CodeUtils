package com.mrleonardos.codeutils.api.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void aPartRemembersItsTextHoverAndClick() {
        MessagePart part = MessagePart.of("текст")
            .hover("подсказка")
            .click(ClickKind.OPEN_URL, "https://example.com");

        assertEquals("текст", part.text());
        assertEquals("подсказка", part.hover());
        assertEquals(ClickKind.OPEN_URL, part.clickKind());
        assertEquals("https://example.com", part.clickValue());
        assertTrue(part.hovers());
        assertTrue(part.clicks());
    }

    @Test
    void anEmptyHoverOrClickTakesTheOldOneAway() {
        MessagePart part = MessagePart.of("текст")
            .hover("подсказка")
            .click(ClickKind.RUN_COMMAND, "spawn");

        assertFalse(
            part.hover("")
                .hovers());
        assertNull(
            part.click(null, "spawn")
                .clickKind());
        assertNull(
            part.click(ClickKind.RUN_COMMAND, "")
                .clickKind());
    }

    @Test
    void partsKeepTheOrderTheyWereGivenIn() {
        Message message = Message.of(MessagePart.of("раз"), MessagePart.of(" два"), MessagePart.of(" три"));

        assertEquals(
            3,
            message.parts()
                .size());
        assertEquals("раз два три", message.flat());
    }

    @Test
    void anEmptyPartNeverGetsIntoAMessage() {
        Message message = Message.of(MessagePart.of(""), MessagePart.of("живое"), null);

        assertEquals(
            1,
            message.parts()
                .size());
        assertEquals("живое", message.flat());
    }

    @Test
    void aMessageWithoutASinglePartIsBlank() {
        assertTrue(
            Message.of(MessagePart.of(""))
                .blank());
        assertTrue(
            Message.empty()
                .blank());
        assertFalse(
            Message.text("текст")
                .blank());
    }

    @Test
    void aPrefixGoesInFrontAsItsOwnPart() {
        Message message = Message.text("текст")
            .withPrefix("[!] ");

        assertEquals(
            2,
            message.parts()
                .size());
        assertEquals("[!] текст", message.flat());
        assertEquals(
            "[!] ",
            message.parts()
                .get(0)
                .text());
    }

    @Test
    void anEmptyPrefixChangesNothing() {
        Message message = Message.text("текст");

        assertEquals(message, message.withPrefix(""));
        assertEquals(message, message.withPrefix(null));
        assertTrue(
            Message.empty()
                .withPrefix("[!] ")
                .blank(),
            "нечего слать, нечего и подписывать");
    }

    @Test
    void theListOfPartsCannotBeChangedFromOutside() {
        Message message = Message.text("текст");

        org.junit.jupiter.api.Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> message.parts()
                .add(MessagePart.of("чужое")));
    }

    @Test
    void twoPartsAreTheSameWhenEverythingAboutThemIsTheSame() {
        MessagePart one = MessagePart.of("текст")
            .hover("подсказка");
        MessagePart two = MessagePart.of("текст")
            .hover("подсказка");

        assertEquals(one, two);
        assertEquals(one.hashCode(), two.hashCode());
        assertEquals(Message.of(one), Message.of(two));
    }

    @Test
    void everyClickKindIsFoundByItsPrefixAndBackAgain() {
        for (ClickKind kind : ClickKind.values()) {
            assertEquals(kind, ClickKind.byPrefix(kind.prefix()), kind.name());
        }
        assertEquals(ClickKind.OPEN_URL, ClickKind.byPrefix(" URL "));
        assertNull(ClickKind.byPrefix("teleport"));
        assertNull(ClickKind.byPrefix(null));
    }

    @Test
    void theThreeKindsAreExactlyWhatTheProtocolOfSevenTenAllows() {
        assertEquals(
            Arrays.asList("url", "run", "suggest"),
            Arrays.asList(
                ClickKind.OPEN_URL.prefix(),
                ClickKind.RUN_COMMAND.prefix(),
                ClickKind.SUGGEST_COMMAND.prefix()));
        assertEquals(3, ClickKind.values().length, "четвёртого действия, которое сервер вправе слать, там нет");
    }
}
