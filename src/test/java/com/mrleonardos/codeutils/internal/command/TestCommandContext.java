package com.mrleonardos.codeutils.internal.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mrleonardos.codecore.api.actor.PlayerRef;
import com.mrleonardos.codecore.api.command.CommandContext;
import com.mrleonardos.codecore.api.command.CommandSender;
import com.mrleonardos.codecore.api.command.SenderKind;
import com.mrleonardos.codecore.api.command.SenderPosition;

final class TestCommandContext implements CommandContext {

    private final Map<String, Object> values = new LinkedHashMap<>();
    private final List<Sent> replies = new ArrayList<>();

    private CommandSender caller = new TestSender(SenderKind.CONSOLE, "Server");

    TestCommandContext set(String name, Object value) {
        values.put(name, value);
        return this;
    }

    TestCommandContext by(CommandSender sender) {
        this.caller = sender;
        return this;
    }

    @Override
    public CommandSender caller() {
        return caller;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        if (!values.containsKey(name)) {
            throw new IllegalArgumentException("Command has no argument named " + name);
        }
        return (T) values.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String name, T fallback) {
        return values.containsKey(name) ? (T) values.get(name) : fallback;
    }

    @Override
    public boolean has(String name) {
        return values.containsKey(name);
    }

    @Override
    public void reply(String translationKey, Object... arguments) {
        replies.add(new Sent(translationKey, arguments, false));
    }

    @Override
    public void replyError(String translationKey, Object... arguments) {
        replies.add(new Sent(translationKey, arguments, true));
    }

    Sent last() {
        return replies.get(replies.size() - 1);
    }

    List<Sent> sent() {
        return replies;
    }

    List<String> keys() {
        List<String> keys = new ArrayList<>();
        for (Sent reply : replies) {
            keys.add(reply.key);
        }
        return keys;
    }

    static final class Sent {

        final String key;
        final List<Object> arguments;
        final boolean error;

        private Sent(String key, Object[] arguments, boolean error) {
            this.key = key;
            this.arguments = Arrays.asList(arguments);
            this.error = error;
        }
    }

    /** Отправитель без единого типа игры: вид, имя и позиция, больше подписи автора ничего не нужно. */
    static final class TestSender implements CommandSender {

        private final SenderKind kind;
        private final String name;
        private final SenderPosition position;

        TestSender(SenderKind kind, String name) {
            this(kind, name, null);
        }

        TestSender(SenderKind kind, String name, SenderPosition position) {
            this.kind = kind;
            this.name = name;
            this.position = position;
        }

        @Override
        public SenderKind kind() {
            return kind;
        }

        @Override
        public Optional<PlayerRef> player() {
            return kind == SenderKind.PLAYER
                ? Optional.of(PlayerRef.of(java.util.UUID.nameUUIDFromBytes(name.getBytes()), name))
                : Optional.empty();
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Optional<SenderPosition> position() {
            return Optional.ofNullable(position);
        }

        @Override
        public void reply(String translationKey, Object... arguments) {}

        @Override
        public void replyError(String translationKey, Object... arguments) {}
    }
}
