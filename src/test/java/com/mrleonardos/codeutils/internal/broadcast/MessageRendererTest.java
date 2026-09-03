package com.mrleonardos.codeutils.internal.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.mrleonardos.codeutils.api.message.ClickKind;
import com.mrleonardos.codeutils.api.message.Message;
import com.mrleonardos.codeutils.api.message.MessagePart;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile.MessageBlock;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile.PartBlock;

class MessageRendererTest {

    private static final Logger LOG = LogManager.getLogger("codeutils-test");

    private final MessageRenderer renderer = new MessageRenderer(LOG);

    @Test
    void aPlainTextBecomesOnePartWithColoursPainted() {
        Message message = renderer.render("tips", 1, MessageBlock.of("&7Подсказка"));

        assertEquals(
            1,
            message.parts()
                .size());
        MessagePart part = message.parts()
            .get(0);
        assertEquals("§7Подсказка", part.text());
        assertFalse(part.hovers());
        assertFalse(part.clicks());
    }

    @Test
    void everyOneOfTheThreeClickKindsIsUnderstood() {
        assertEquals(ClickKind.OPEN_URL, kindOf("url:https://example.com/rules"));
        assertEquals(ClickKind.RUN_COMMAND, kindOf("run:spawn"));
        assertEquals(ClickKind.SUGGEST_COMMAND, kindOf("suggest:/pay "));
    }

    @Test
    void theValueOfTheClickKeepsEverythingAfterTheFirstColon() {
        Message message = renderer.render("links", 1, MessageBlock.link("текст", "", "url:https://example.com/rules"));

        assertEquals(
            "https://example.com/rules",
            message.parts()
                .get(0)
                .clickValue());
    }

    @Test
    void hoverIsPaintedTheSameWayAsTheText() {
        Message message = renderer.render("links", 1, MessageBlock.link("&bссылка", "&fоткрыть", "run:spawn"));
        MessagePart part = message.parts()
            .get(0);

        assertEquals("§bссылка", part.text());
        assertEquals("§fоткрыть", part.hover());
    }

    @Test
    void anUnknownClickPrefixLeavesTheMessageWithoutAClick() {
        Message message = renderer.render("links", 2, MessageBlock.link("текст", "", "teleport:0,64,0"));
        MessagePart part = message.parts()
            .get(0);

        assertEquals("текст", part.text());
        assertFalse(part.clicks(), "сообщение уходит, но без щелчка");
        assertNull(part.clickKind());
    }

    @Test
    void aClickWithoutAColonIsUnknownToo() {
        Message message = renderer.render("links", 3, MessageBlock.link("текст", "", "https://example.com"));

        assertFalse(
            message.parts()
                .get(0)
                .clicks());
    }

    @Test
    void partsFollowTheOwnTextOfTheMessageInTheOrderTheyAreWritten() {
        MessageBlock block = MessageBlock.of("начало ");
        PartBlock first = PartBlock.of("правила");
        first.click = "url:https://example.com/rules";
        PartBlock second = PartBlock.of(" и голосование");
        second.click = "url:https://example.com/vote";
        block.parts = new ArrayList<>(Arrays.asList(first, second));

        Message message = renderer.render("links", 1, block);

        assertEquals(
            3,
            message.parts()
                .size());
        assertEquals("начало правила и голосование", message.flat());
        assertEquals(
            "https://example.com/rules",
            message.parts()
                .get(1)
                .clickValue());
        assertEquals(
            "https://example.com/vote",
            message.parts()
                .get(2)
                .clickValue());
        assertFalse(
            message.parts()
                .get(0)
                .clicks(),
            "у собственного текста своего щелчка нет");
    }

    @Test
    void anEmptyPartIsNotSentAtAll() {
        MessageBlock block = MessageBlock.of("");
        block.parts = new ArrayList<>(Arrays.asList(PartBlock.of(""), PartBlock.of("живая часть")));

        Message message = renderer.render("links", 1, block);

        assertEquals(
            1,
            message.parts()
                .size());
        assertEquals("живая часть", message.flat());
    }

    @Test
    void aMessageWithNothingInItIsBlank() {
        assertTrue(
            renderer.render("links", 1, MessageBlock.of(""))
                .blank());
    }

    private ClickKind kindOf(String click) {
        return renderer.render("links", 1, MessageBlock.link("текст", "", click))
            .parts()
            .get(0)
            .clickKind();
    }
}
