package com.mrleonardos.codeutils.internal.queue;

import com.mrleonardos.codeutils.api.queue.QueueDecision;
import com.mrleonardos.codeutils.api.queue.QueuePolicy;
import com.mrleonardos.codeutils.api.queue.QueueRequest;
import com.mrleonardos.codeutils.api.queue.QueueState;

public final class BaseOnly implements QueuePolicy {

    @Override
    public QueueDecision decide(QueueRequest request, QueueState state) {
        return Seats.take(request, state, QueueDecision.NO_TIER, state.baseSlots());
    }
}
