package com.mrleonardos.codeutils.internal.queue;

import java.util.Optional;

import com.mrleonardos.codeutils.api.queue.QueueDecision;
import com.mrleonardos.codeutils.api.queue.QueueRequest;
import com.mrleonardos.codeutils.api.queue.QueueState;
import com.mrleonardos.codeutils.api.queue.QueueTicket;

final class Seats {

    private Seats() {}

    static QueueDecision take(QueueRequest request, QueueState state, String tier, int cap) {
        if (state.online() >= cap) {
            return QueueDecision.refuse(tier);
        }
        Optional<QueueTicket> held = state.heldFor();
        if (held.isPresent() && !held.get()
            .id()
            .equals(request.id())) {
            return QueueDecision.refuse(tier);
        }
        return QueueDecision.let(tier);
    }
}
