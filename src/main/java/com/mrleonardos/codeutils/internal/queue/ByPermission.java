package com.mrleonardos.codeutils.internal.queue;

import java.util.Optional;

import com.mrleonardos.codeutils.api.queue.QueueDecision;
import com.mrleonardos.codeutils.api.queue.QueuePolicy;
import com.mrleonardos.codeutils.api.queue.QueueRequest;
import com.mrleonardos.codeutils.api.queue.QueueState;
import com.mrleonardos.codeutils.api.queue.QueueTicket;

public final class ByPermission implements QueuePolicy {

    @Override
    public QueueDecision decide(QueueRequest request, QueueState state) {
        String tier = QueueDecision.NO_TIER;
        int cap = state.baseSlots();
        for (String name : state.tiers()) {
            if (state.allowed(request.id(), state.nodeOf(name))) {
                tier = name;
                cap = state.slotsOf(name);
                break;
            }
        }
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
