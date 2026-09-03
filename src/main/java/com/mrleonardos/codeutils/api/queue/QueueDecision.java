package com.mrleonardos.codeutils.api.queue;

/**
 * Ответ политики: пустить или отказать, и по какой ступени игрок посчитан.
 *
 * <p>
 * Номера в очереди здесь нет намеренно. Порядок держит сам мод: политика отвечает за потолки и ступени, а
 * билеты, их время и номера считаются в одном месте, чтобы очередь у всех политик работала одинаково.
 */
public final class QueueDecision {

    /** Имя ступени у игрока, которому ступень не досталась. */
    public static final String NO_TIER = "";

    private static final QueueDecision LET = new QueueDecision(true, NO_TIER);
    private static final QueueDecision REFUSED = new QueueDecision(false, NO_TIER);

    private final boolean allowed;
    private final String tier;

    private QueueDecision(boolean allowed, String tier) {
        this.allowed = allowed;
        this.tier = tier;
    }

    /** Пустить игрока без ступени. */
    public static QueueDecision let() {
        return LET;
    }

    /** Пустить игрока по названной ступени. */
    public static QueueDecision let(String tier) {
        return tier == null || tier.isEmpty() ? LET : new QueueDecision(true, tier);
    }

    /** Отказать игроку без ступени. */
    public static QueueDecision refuse() {
        return REFUSED;
    }

    /** Отказать игроку, у которого ступень есть, но её потолок занят. */
    public static QueueDecision refuse(String tier) {
        return tier == null || tier.isEmpty() ? REFUSED : new QueueDecision(false, tier);
    }

    /** Пускать ли игрока. */
    public boolean allowed() {
        return allowed;
    }

    /** Ступень, по которой игрок посчитан; {@link #NO_TIER}, если ступени нет. */
    public String tier() {
        return tier;
    }

    @Override
    public String toString() {
        return (allowed ? "let" : "refused") + (tier.isEmpty() ? "" : " as " + tier);
    }
}
