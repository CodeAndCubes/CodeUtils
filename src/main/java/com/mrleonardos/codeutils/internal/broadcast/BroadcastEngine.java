package com.mrleonardos.codeutils.internal.broadcast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codecore.api.actor.PlayerRef;
import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.message.BroadcastSink;
import com.mrleonardos.codeutils.api.message.Message;
import com.mrleonardos.codeutils.api.when.ServerSnapshot;
import com.mrleonardos.codeutils.api.when.When;
import com.mrleonardos.codeutils.internal.Conditions;
import com.mrleonardos.codeutils.internal.ServerFacts;
import com.mrleonardos.codeutils.internal.SpiNames;
import com.mrleonardos.codeutils.internal.Subsystem;
import com.mrleonardos.codeutils.internal.WhenBlock;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile.MessageBlock;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile.SetBlock;

public final class BroadcastEngine implements Subsystem, SpiNames.Source {

    private static final long MILLIS = 1000L;

    private final Supplier<BroadcastsFile> file;
    private final UtilsRegistry registry;
    private final Conditions conditions;
    private final ServerFacts facts;
    private final MessageRenderer renderer;
    private final Random random;
    private final Logger log;

    private final Map<String, Live> live = new LinkedHashMap<>();

    public BroadcastEngine(Supplier<BroadcastsFile> file, UtilsRegistry registry, Conditions conditions,
        ServerFacts facts, Random random, Logger log) {
        this.file = file;
        this.registry = registry;
        this.conditions = conditions;
        this.facts = facts;
        this.renderer = new MessageRenderer(log);
        this.random = random;
        this.log = log;
    }

    public void arm(long now) {
        live.clear();
        for (Map.Entry<String, SetBlock> entry : file.get().sets.entrySet()) {
            String name = entry.getKey();
            SetBlock block = entry.getValue();
            if (!block.enabled) {
                continue;
            }
            if (block.messages.isEmpty()) {
                log.warn("Broadcast set {} holds no messages, it stays quiet", name);
                continue;
            }
            BroadcastSink sink = registry.sink(word(block.sink))
                .orElse(null);
            if (sink == null) {
                continue;
            }
            Live set = new Live(name, block, sink, new Rotation(Rotation.order(block.order, where(name), log), random));
            if (block.intervalSeconds > 0) {
                set.deadline = now + Math.max(0, block.firstDelaySeconds) * MILLIS;
            } else {
                log.info("Broadcast set {} has intervalSeconds = 0, it goes out only by /broadcast {}", name, name);
            }
            live.put(name, set);
        }
    }

    @Override
    public Map<String, String> sinks() {
        Map<String, String> named = new LinkedHashMap<>();
        for (Map.Entry<String, SetBlock> entry : file.get().sets.entrySet()) {
            SetBlock block = entry.getValue();
            if (block.enabled && !block.messages.isEmpty()) {
                named.put(where(entry.getKey()), word(block.sink));
            }
        }
        return named;
    }

    @Override
    public Map<String, String> runners() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> guards() {
        return Collections.emptyList();
    }

    @Override
    public List<String> conditions() {
        List<String> named = new ArrayList<>();
        for (SetBlock block : file.get().sets.values()) {
            if (block.enabled && block.when != null && block.when.custom != null) {
                named.addAll(block.when.custom);
            }
        }
        return named;
    }

    @Override
    public void tick(long from, long to, ServerSnapshot snapshot) {
        for (Live set : live.values()) {
            if (set.deadline == 0L || to < set.deadline) {
                continue;
            }
            set.deadline = to + set.block.intervalSeconds * MILLIS;
            if (!conditions.allows(set.when, snapshot)) {
                continue;
            }
            fire(set);
        }
    }

    public Sent sendNow(String name) {
        Live set = live.get(name);
        if (set == null) {
            return file.get().sets.containsKey(name) ? Sent.setOff() : Sent.unknownSet();
        }
        List<PlayerRef> recipients = recipients(set.block);
        if (recipients.isEmpty()) {
            return Sent.nobody();
        }
        return deliver(set, next(set), recipients);
    }

    public Sent sendPlain(String text) {
        BroadcastSink sink = registry.sink(BroadcastsFile.SINK_CHAT)
            .orElse(null);
        List<PlayerRef> recipients = facts.online();
        if (sink == null || recipients.isEmpty()) {
            return Sent.nobody();
        }
        sink.send(recipients, renderer.plain(text));
        return Sent.to(recipients.size());
    }

    public List<String> names() {
        return new ArrayList<>(live.keySet());
    }

    public int workingSets() {
        return live.size();
    }

    public long deadlineOf(String name) {
        Live set = live.get(name);
        return set == null ? 0L : set.deadline;
    }

    private void fire(Live set) {
        List<PlayerRef> recipients = recipients(set.block);
        if (!recipients.isEmpty()) {
            deliver(set, next(set), recipients);
            return;
        }
        String mode = word(set.block.whenEmpty);
        if (BroadcastsFile.EMPTY_SKIP.equals(mode)) {
            set.rotation.skip(set.block.messages.size());
            return;
        }
        if (BroadcastsFile.EMPTY_LOG.equals(mode)) {
            Message message = next(set);
            log.info("Broadcast set {} had nobody to talk to: {}", set.name, ColorCodes.strip(message.flat()));
            return;
        }
        if (!BroadcastsFile.EMPTY_HOLD.equals(mode) && !set.emptyModeTold) {
            set.emptyModeTold = true;
            log.warn(
                "{}: whenEmpty = {} is not one of hold, skip, log, hold is used",
                where(set.name),
                set.block.whenEmpty);
        }
    }

    private Message next(Live set) {
        int index = set.rotation.next(set.block.messages.size());
        if (index < 0) {
            return Message.empty();
        }
        MessageBlock block = set.block.messages.get(index);
        Message message = renderer.render(where(set.name), index + 1, block);
        return message.withPrefix(ColorCodes.paint(set.block.prefix));
    }

    private Sent deliver(Live set, Message message, List<PlayerRef> recipients) {
        if (message.blank() || recipients.isEmpty()) {
            return Sent.nobody();
        }
        set.sink.send(recipients, message);
        return Sent.to(recipients.size());
    }

    private List<PlayerRef> recipients(SetBlock block) {
        String node = block.permission == null ? "" : block.permission.trim();
        List<PlayerRef> found = new ArrayList<>();
        for (PlayerRef player : facts.online()) {
            if (!node.isEmpty() && !facts.allowed(player.id(), node)) {
                continue;
            }
            if (!block.dimensions.isEmpty()
                && !block.dimensions.contains(Integer.valueOf(facts.dimensionOf(player.id())))) {
                continue;
            }
            found.add(player);
        }
        return found;
    }

    private static String where(String name) {
        return BroadcastsFile.FILE_NAME + " set " + name;
    }

    private static String word(String value) {
        return value == null ? ""
            : value.trim()
                .toLowerCase(Locale.ROOT);
    }

    private final class Live {

        private final String name;
        private final SetBlock block;
        private final BroadcastSink sink;
        private final Rotation rotation;
        private final When when;

        private long deadline;
        private boolean emptyModeTold;

        private Live(String name, SetBlock block, BroadcastSink sink, Rotation rotation) {
            this.name = name;
            this.block = block;
            this.sink = sink;
            this.rotation = rotation;
            WhenBlock written = block.when == null ? new WhenBlock() : block.when;
            this.when = written.toWhen(where(name), log);
        }
    }
}
