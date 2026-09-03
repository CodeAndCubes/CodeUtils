package com.mrleonardos.codeutils.internal;

import java.util.List;

import com.mrleonardos.codecore.api.actor.PlayerRef;
import com.mrleonardos.codeutils.api.message.BroadcastSink;
import com.mrleonardos.codeutils.api.message.Message;
import com.mrleonardos.codeutils.internal.broadcast.ColorCodes;

public final class Announcer {

    private final ServerFacts facts;
    private final UtilsTexts texts;

    public Announcer(ServerFacts facts, UtilsTexts texts) {
        this.facts = facts;
        this.texts = texts;
    }

    public int say(BroadcastSink sink, String prefix, String key, Object... arguments) {
        List<PlayerRef> recipients = facts.online();
        if (sink == null || recipients.isEmpty()) {
            return 0;
        }
        Message message = Message.text(ColorCodes.paint(texts.format(key, arguments)))
            .withPrefix(ColorCodes.paint(prefix));
        sink.send(recipients, message);
        return recipients.size();
    }
}
