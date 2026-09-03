package com.mrleonardos.codeutils.api.queue;

import java.util.Objects;
import java.util.UUID;

/**
 * Место в очереди: чьё, какой ступени и с какого времени.
 *
 * <p>
 * Время первого стука повторный стук не двигает, иначе игрок терял бы очередь каждый раз, когда пробует
 * зайти. Номер считается заново на каждый вопрос: он меняется, когда впереди стоящие заходят или когда их
 * билеты протухают.
 */
public final class QueueTicket {

    private final UUID id;
    private final String name;
    private final String tier;
    private final long since;
    private final int place;

    private QueueTicket(UUID id, String name, String tier, long since, int place) {
        this.id = id;
        this.name = name;
        this.tier = tier;
        this.since = since;
        this.place = place;
    }

    /**
     * Билет с посчитанным номером.
     *
     * @param tier  имя ступени или пустая строка у игрока без ступени
     * @param since время первого стука в миллисекундах эпохи
     * @param place номер в очереди, считая с единицы
     */
    public static QueueTicket of(UUID id, String name, String tier, long since, int place) {
        return new QueueTicket(
            Objects.requireNonNull(id, "id"),
            Objects.requireNonNull(name, "name"),
            tier == null ? "" : tier,
            since,
            place);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** Имя ступени; пустая строка у игрока без ступени. */
    public String tier() {
        return tier;
    }

    /** Время первого стука в миллисекундах эпохи. */
    public long since() {
        return since;
    }

    /** Номер в очереди, считая с единицы. */
    public int place() {
        return place;
    }

    @Override
    public String toString() {
        return "ticket[" + place + ", " + name + (tier.isEmpty() ? "" : ", " + tier) + "]";
    }
}
