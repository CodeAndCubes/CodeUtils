package com.mrleonardos.codeutils.internal.queue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.queue.QueueDecision;
import com.mrleonardos.codeutils.api.queue.QueuePolicy;
import com.mrleonardos.codeutils.api.queue.QueueRequest;
import com.mrleonardos.codeutils.api.queue.QueueState;
import com.mrleonardos.codeutils.api.queue.QueueTicket;
import com.mrleonardos.codeutils.internal.Rights;
import com.mrleonardos.codeutils.internal.UtilsTexts;
import com.mrleonardos.codeutils.internal.Words;
import com.mrleonardos.codeutils.internal.command.UtilsMessages;

public final class QueueGate {

    private final Supplier<QueueFile> file;
    private final UtilsRegistry registry;
    private final Rights rights;
    private final Logger log;

    private final QueueBoard board = new QueueBoard();
    private final AtomicInteger online = new AtomicInteger();

    private volatile QueuePolicy policy = new BaseOnly();
    private volatile QueueFile settings;

    public QueueGate(Supplier<QueueFile> file, UtilsRegistry registry, Rights rights, Logger log) {
        this.file = file;
        this.registry = registry;
        this.rights = rights;
        this.log = log;
    }

    public void arm(int onlineNow, int maxPlayers) {
        online.set(Math.max(0, onlineNow));
        QueueFile written = file.get();
        settings = written;
        QueuePolicy named = registry.policy(Words.word(written.queue.policy))
            .orElse(null);
        policy = named == null ? new BaseOnly() : named;
        if (named == null) {
            log.warn(
                "{} names the queue policy {}, and no mod registered it, the tiers do not work: "
                    + "everybody is counted against slots.base",
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
        QueueFile written = held();
        board.expire(now, written.queue.ticketSeconds);
        SlotRules rules = SlotRules.of(written);
        QueueDecision decision = chosen
            .decide(QueueRequest.of(id, name, now), new Board(rules, written, board, rights, online.get(), now));
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
        online.incrementAndGet();
        board.drop(id);
    }

    public void left(long now) {
        board.freed(now);
        online.updateAndGet(held -> Math.max(0, held - 1));
    }

    public int online() {
        return online.get();
    }

    public String refusalOf(Answer answer, UtilsTexts texts) {
        return answer.place() > 0 ? texts
            .format(UtilsMessages.QUEUE_REFUSED, Integer.valueOf(answer.place()), Integer.valueOf(retryHintSeconds()))
            : texts.format(UtilsMessages.QUEUE_FULL);
    }

    public int retryHintSeconds() {
        return Math.max(0, held().queue.retryHintSeconds);
    }

    public boolean queueOn() {
        return held().queue.enabled;
    }

    public List<QueueTicket> queue(long now) {
        board.expire(now, held().queue.ticketSeconds);
        return board.list();
    }

    public SlotRules rules() {
        return SlotRules.of(held());
    }

    private QueueFile held() {
        QueueFile written = settings;
        return written == null ? file.get() : written;
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
}
