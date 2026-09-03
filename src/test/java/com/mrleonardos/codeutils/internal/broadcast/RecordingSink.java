package com.mrleonardos.codeutils.internal.broadcast;

import java.util.ArrayList;
import java.util.List;

import com.mrleonardos.codecore.api.actor.PlayerRef;
import com.mrleonardos.codeutils.api.message.BroadcastSink;
import com.mrleonardos.codeutils.api.message.Message;

/** Приёмник, который ничего не отправляет, а складывает отправленное списком. */
public final class RecordingSink implements BroadcastSink {

    private final List<Message> messages = new ArrayList<>();
    private final List<List<PlayerRef>> recipients = new ArrayList<>();

    @Override
    public void send(List<PlayerRef> to, Message message) {
        recipients.add(new ArrayList<>(to));
        messages.add(message);
    }

    public List<Message> messages() {
        return messages;
    }

    public List<String> texts() {
        List<String> flat = new ArrayList<>();
        for (Message message : messages) {
            flat.add(message.flat());
        }
        return flat;
    }

    public List<PlayerRef> lastRecipients() {
        return recipients.get(recipients.size() - 1);
    }

    public int sends() {
        return messages.size();
    }
}
