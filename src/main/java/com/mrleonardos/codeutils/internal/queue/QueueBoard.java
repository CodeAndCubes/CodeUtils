package com.mrleonardos.codeutils.internal.queue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mrleonardos.codeutils.api.queue.QueueTicket;
import com.mrleonardos.codeutils.internal.Clocks;

public final class QueueBoard {

    private final Map<UUID, Seat> seats = new ConcurrentHashMap<>();

    private volatile long freedAt;

    public int knock(UUID id, String name, String tier, int rank, long now) {
        Seat held = seats.get(id);
        if (held == null) {
            seats.put(id, new Seat(id, name, tier, rank, now));
        } else {
            held.tier = tier;
            held.rank = rank;
            held.knocked = now;
        }
        return place(id);
    }

    public void drop(UUID id) {
        if (seats.remove(id) != null) {
            freedAt = 0L;
        }
    }

    public void freed(long now) {
        freedAt = now;
    }

    public void expire(long now, int ticketSeconds) {
        if (ticketSeconds <= 0) {
            return;
        }
        long oldest = now - ticketSeconds * Clocks.MILLIS;
        for (Map.Entry<UUID, Seat> entry : seats.entrySet()) {
            if (entry.getValue().knocked < oldest) {
                seats.remove(entry.getKey());
            }
        }
    }

    public boolean holding(long now, int holdSeconds) {
        long since = freedAt;
        return since > 0L && !seats.isEmpty() && now - since <= Math.max(0, holdSeconds) * Clocks.MILLIS;
    }

    public QueueTicket head() {
        List<QueueTicket> queue = list();
        return queue.isEmpty() ? null : queue.get(0);
    }

    public List<QueueTicket> list() {
        List<Seat> sorted = new ArrayList<>(seats.values());
        Collections.sort(
            sorted,
            Comparator.comparingInt((Seat seat) -> seat.rank)
                .reversed()
                .thenComparingLong(seat -> seat.since));
        List<QueueTicket> tickets = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            Seat seat = sorted.get(index);
            tickets.add(QueueTicket.of(seat.id, seat.name, seat.tier, seat.since, index + 1));
        }
        return tickets;
    }

    public int size() {
        return seats.size();
    }

    public long freedAt() {
        return freedAt;
    }

    private int place(UUID id) {
        for (QueueTicket ticket : list()) {
            if (ticket.id()
                .equals(id)) {
                return ticket.place();
            }
        }
        return 0;
    }

    private static final class Seat {

        private final UUID id;
        private final String name;
        private final long since;

        private volatile String tier;
        private volatile int rank;
        private volatile long knocked;

        private Seat(UUID id, String name, String tier, int rank, long now) {
            this.id = id;
            this.name = name;
            this.tier = tier;
            this.rank = rank;
            this.since = now;
            this.knocked = now;
        }
    }
}
