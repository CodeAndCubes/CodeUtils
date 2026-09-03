package com.mrleonardos.codeutils.platform;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

import com.mrleonardos.codeutils.api.message.ClickKind;
import com.mrleonardos.codeutils.api.message.Message;
import com.mrleonardos.codeutils.api.message.MessagePart;

public final class Components {

    private Components() {}

    public static IChatComponent of(Message message) {
        IChatComponent whole = null;
        for (MessagePart part : message.parts()) {
            IChatComponent piece = of(part);
            if (whole == null) {
                whole = piece;
                continue;
            }
            whole.appendSibling(piece);
        }
        return whole == null ? new ChatComponentText("") : whole;
    }

    private static IChatComponent of(MessagePart part) {
        ChatComponentText piece = new ChatComponentText(part.text());
        if (!part.hovers() && !part.clicks()) {
            return piece;
        }
        ChatStyle style = new ChatStyle();
        if (part.hovers()) {
            style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(part.hover())));
        }
        if (part.clicks()) {
            style.setChatClickEvent(new ClickEvent(action(part.clickKind()), part.clickValue()));
        }
        piece.setChatStyle(style);
        return piece;
    }

    private static ClickEvent.Action action(ClickKind kind) {
        switch (kind) {
            case OPEN_URL:
                return ClickEvent.Action.OPEN_URL;
            case RUN_COMMAND:
                return ClickEvent.Action.RUN_COMMAND;
            default:
                return ClickEvent.Action.SUGGEST_COMMAND;
        }
    }
}
