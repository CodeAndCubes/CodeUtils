package com.mrleonardos.codeutils.internal.broadcast;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codeutils.api.message.ClickKind;
import com.mrleonardos.codeutils.api.message.Message;
import com.mrleonardos.codeutils.api.message.MessagePart;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile.MessageBlock;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile.PartBlock;

public final class MessageRenderer {

    private static final char CLICK_SEPARATOR = ':';

    private final Logger log;

    public MessageRenderer(Logger log) {
        this.log = log;
    }

    public Message render(String set, int number, MessageBlock block) {
        List<MessagePart> parts = new ArrayList<>();
        if (block.text != null && !block.text.isEmpty()) {
            parts.add(part(set, number, block.text, block.hover, block.click));
        }
        if (block.parts != null) {
            for (PartBlock piece : block.parts) {
                if (piece == null || piece.text == null || piece.text.isEmpty()) {
                    continue;
                }
                parts.add(part(set, number, piece.text, piece.hover, piece.click));
            }
        }
        return Message.of(parts);
    }

    public Message plain(String text) {
        return Message.text(ColorCodes.paint(text));
    }

    private MessagePart part(String set, int number, String text, String hover, String click) {
        MessagePart part = MessagePart.of(ColorCodes.paint(text));
        if (hover != null && !hover.isEmpty()) {
            part = part.hover(ColorCodes.paint(hover));
        }
        if (click == null || click.isEmpty()) {
            return part;
        }
        int separator = click.indexOf(CLICK_SEPARATOR);
        ClickKind kind = separator <= 0 ? null : ClickKind.byPrefix(click.substring(0, separator));
        if (kind == null) {
            log.warn(
                "{}: message {} has click = {}, which is none of url:, run:, suggest:, "
                    + "the message goes out without a click",
                set,
                number,
                click);
            return part;
        }
        return part.click(kind, click.substring(separator + 1));
    }
}
