package com.mrleonardos.codeutils.internal.queue;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.queue.QueueDecision;
import com.mrleonardos.codeutils.api.queue.QueuePolicy;
import com.mrleonardos.codeutils.api.queue.QueueRequest;
import com.mrleonardos.codeutils.api.queue.QueueState;
import com.mrleonardos.codeutils.api.queue.QueueTicket;
import com.mrleonardos.codeutils.internal.Rights;

public final class QueueGate {

    private final Supplier<QueueFile> file;
    private final UtilsRegistry registry;
    private final Rights rights;
    private final Logger log;

    private final QueueBoard board = new QueueBoard();

    private volatile QueuePolicy policy;
    private volatile int online;

    public QueueGate(Supplier<QueueFile> file, UtilsRegistry registry, Rights rights, Logger log) {
        this.file = file;
        this.registry = registry;
        this.rights = rights;
        this.log = log;
    }

    public void arm(int onlineNow, int maxPlayers) {
        online = Math.max(0, onlineNow);
        QueueFile written = file.get();
        policy = registry.policy(word(written.queue.policy))
            .orElse(null);
        if (policy == null) {
            log.warn(
                "{} names the queue policy {}, and no mod registered it, the soft caps stay open",
                QueueFile.FILE_NAME,
                written.queue.policy);
        }
        int top = SlotRules.of(written)
            .topSlots();
        if (maxPlayers > 0 && top > maxPlayers) {
            log.warn(
                "max-players in server.properties is {}, and the top tier asks for {}: "
                    + "every seat above {} is refused by the game itself, not by the mod",
                Integer.valueOf(maxPlayers),
                Integer.valueOf(top),
                Integer.valueOf(maxPlayers));
        }
    }

    public Answer decide(UUID id, String name, long now) {
        QueuePolicy chosen = policy;
        if (chosen == null) {
            return Answer.let(QueueDecision.NO_TIER);
        }
        QueueFile written = file.get();
        board.expire(now, written.queue.ticketSeconds);
        SlotRules rules = SlotRules.of(written);
        QueueDecision decision = chosen
            .decide(QueueRequest.of(id, name, now), new Board(rules, written, board, rights, online, now));
        if (decision.allowed()) {
            board.drop(id);
            return Answer.let(decision.tier());
        }
        if (!written.queue.enabled) {
            return Answer.refuse(decision.tier(), 0);
        }
        return Answer
            .refuse(decision.tier(), board.knock(id, name, decision.tier(), rules.rankOf(decision.tier()), now));
    }

    public void joined(UUID id) {
        online++;
        board.drop(id);
    }

    public void left(long now) {
        online = Math.max(0, online - 1);
        board.freed(now);
    }

    public int online() {
        return online;
    }

    public int retryHintSeconds() {
        return Math.max(0, file.get().queue.retryHintSeconds);
    }

    public boolean queueOn() {
        return file.get().queue.enabled;
    }

    public List<QueueTicket> queue(long now) {
        board.expire(now, file.get().queue.ticketSeconds);
        return board.list();
    }

    public SlotRules rules() {
        return SlotRules.of(file.get());
    }

    public static final class Answer {

        private final boolean allowed;
        private final String tier;
        private final int place;

        private Answer(boolean allowed, String tier, int place) {
            this.allowed = allowed;
            this.tier = tier;
            this.place = place;
        }

        static Answer let(String tier) {
            return new Answer(true, tier, 0);
        }

        static Answer refuse(String tier, int place) {
            return new Answer(false, tier, place);
        }

        public boolean allowed() {
            return allowed;
        }

        public String tier() {
            return tier;
        }

        public int place() {
            return place;
        }

        @Override
        public String toString() {
            return allowed ? "let" : "refused, place " + place;
        }
    }

    private static final class Board implements QueueState {

        private final SlotRules rules;
        private final QueueFile file;
        private final QueueBoard board;
        private final Rights rights;
        private final int online;
        private final long now;

        private Board(SlotRules rules, QueueFile file, QueueBoard board, Rights rights, int online, long now) {
            this.rules = rules;
            this.file = file;
            this.board = board;
            this.rights = rights;
            this.online = online;
            this.now = now;
        }

        @Override
        public int online() {
            return online;
        }

        @Override
        public int baseSlots() {
            return rules.base();
        }

        @Override
        public List<String> tiers() {
            return rules.names();
        }

        @Override
        public int slotsOf(String tier) {
            return rules.slotsOf(tier);
        }

        @Override
        public String nodeOf(String tier) {
            return rules.nodeOf(tier);
        }

        @Override
        public boolean allowed(UUID player, String node) {
            return node != null && !node.isEmpty() && rights.allowed(player, node);
        }

        @Override
        public boolean queueOn() {
            return file.queue.enabled;
        }

        @Override
        public List<QueueTicket> queue() {
            return board.list();
        }

        @Override
        public Optional<QueueTicket> heldFor() {
            return board.holding(now, file.queue.holdSeconds) ? Optional.ofNullable(board.head()) : Optional.empty();
        }
    }

    private static String word(String value) {
        return value == null ? ""
            : value.trim()
                .toLowerCase(Locale.ROOT);
    }
}
