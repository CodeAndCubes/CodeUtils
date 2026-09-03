package com.mrleonardos.codeutils.internal.broadcast;

import java.util.List;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codecore.api.actor.PlayerRef;
import com.mrleonardos.codeutils.api.message.BroadcastSink;
import com.mrleonardos.codeutils.api.message.Message;

public final class LogSink implements BroadcastSink {

    private final Logger log;

    public LogSink(Logger log) {
        this.log = log;
    }

    @Override
    public void send(List<PlayerRef> recipients, Message message) {
        log.info("Broadcast to {} player(s): {}", Integer.valueOf(recipients.size()), ColorCodes.strip(message.flat()));
    }
}
