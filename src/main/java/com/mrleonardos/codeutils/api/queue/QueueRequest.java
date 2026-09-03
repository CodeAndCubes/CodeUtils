package com.mrleonardos.codeutils.api.queue;

import java.util.Objects;
import java.util.UUID;

/**
 * Стук в дверь: кто подключается и когда.
 *
 * <p>
 * Заявка приходит на потоке сети, до того как игрок попал в мир. Ни мира, ни сущности игрока в этот
 * момент ещё нет, поэтому здесь только то, что известно из самого подключения.
 */
public final class QueueRequest {

    private final UUID id;
    private final String name;
    private final long at;

    private QueueRequest(UUID id, String name, long at) {
        this.id = id;
        this.name = name;
        this.at = at;
    }

    /**
     * Заявка на вход.
     *
     * @param id   идентификатор игрока из его профиля
     * @param name ник
     * @param at   время стука в миллисекундах эпохи
     */
    public static QueueRequest of(UUID id, String name, long at) {
        return new QueueRequest(Objects.requireNonNull(id, "id"), Objects.requireNonNull(name, "name"), at);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** Время стука в миллисекундах эпохи. */
    public long at() {
        return at;
    }

    @Override
    public String toString() {
        return "knock[" + name + "]";
    }
}
