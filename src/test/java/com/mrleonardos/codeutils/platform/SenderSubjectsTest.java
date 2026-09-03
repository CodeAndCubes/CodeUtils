package com.mrleonardos.codeutils.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mrleonardos.codecore.api.actor.PlayerRef;
import com.mrleonardos.codecore.api.command.CommandSender;
import com.mrleonardos.codecore.api.command.SenderKind;
import com.mrleonardos.codecore.api.command.SenderPosition;

/**
 * Подпись автора в записи аудита собирается из вида отправителя и его позиции, а не из цепочки
 * {@code instanceof} по классам игры. Из-за этого её видно без запуска сервера.
 */
class SenderSubjectsTest {

    @Test
    void aPlayerSignsWithHisOwnNick() {
        assertEquals("Steve", SenderSubjects.actorOf(sender(SenderKind.PLAYER, "Steve", null)));
    }

    @Test
    void aCommandBlockSignsWithItsCoordinates() {
        assertEquals(
            "commandblock@10,64,-20",
            SenderSubjects.actorOf(sender(SenderKind.COMMAND_BLOCK, "@", new SenderPosition(0, 10, 64, -20))));
    }

    @Test
    void aCommandBlockWithoutAPositionStillSignsSomething() {
        assertEquals("commandblock", SenderSubjects.actorOf(sender(SenderKind.COMMAND_BLOCK, "@", null)));
    }

    @Test
    void theConsoleAndTheRemoteConsoleAreToldApart() {
        assertEquals("console", SenderSubjects.actorOf(sender(SenderKind.CONSOLE, "Server", null)));
        assertEquals("rcon", SenderSubjects.actorOf(sender(SenderKind.RCON, "Rcon", null)));
    }

    private static CommandSender sender(SenderKind kind, String name, SenderPosition position) {
        return new CommandSender() {

            @Override
            public SenderKind kind() {
                return kind;
            }

            @Override
            public Optional<PlayerRef> player() {
                return kind == SenderKind.PLAYER
                    ? Optional.of(PlayerRef.of(UUID.nameUUIDFromBytes(name.getBytes()), name))
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
        };
    }
}
