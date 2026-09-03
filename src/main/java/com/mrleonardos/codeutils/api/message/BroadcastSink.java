package com.mrleonardos.codeutils.api.message;

import java.util.List;

import com.mrleonardos.codecore.api.actor.PlayerRef;

/**
 * Куда уходит сообщение рассылки.
 *
 * <p>
 * Встроены два приёмника: {@code chat} шлёт сообщение в чат каждому получателю, {@code log} пишет его
 * плоским текстом в лог сервера. Свой приёмник (мост в Discord, канал чата, собственный интерфейс) мод
 * заявляет в реестр в фазе init и называет его имя в ключе {@code sink} набора.
 *
 * <p>
 * Список получателей уже сужен узлом права и измерениями набора. Пустым он приёмнику не приходит:
 * поведение на пустом сервере решается до отправки ключом {@code whenEmpty}.
 */
@FunctionalInterface
public interface BroadcastSink {

    /** Доставить сообщение получателям. Вызов идёт в главном потоке сервера. */
    void send(List<PlayerRef> recipients, Message message);
}
