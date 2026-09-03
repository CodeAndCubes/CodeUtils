package com.mrleonardos.codeutils.internal.queue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import com.mrleonardos.codesides.gate.PackageGate;
import com.mrleonardos.codeutils.api.CodeUtilsApi;
import com.mrleonardos.codeutils.api.UtilsRegistry;

/**
 * Решение об отказе приходит на потоке сети, а не в главном.
 *
 * <p>
 * Обещание «мир не трогаем» стережётся не обещанием, а гейтом по байткоду: в пакете очереди нет ни одной
 * ссылки на швы, которые ходят в мир. Появится такая ссылка, тест назовёт класс.
 */
class QueueOffTheWorldTest {

    private static final String[] WORLD = { "com/mrleonardos/codeutils/internal/ServerFacts",
        "com/mrleonardos/codeutils/internal/clean/EntitySweep", "com/mrleonardos/codeutils/internal/restart/Shutdown",
        "com/mrleonardos/codeutils/internal/Announcer" };

    @Test
    void theQueuePackageHoldsNoReferenceToAnySeamThatGoesIntoTheWorld() throws IOException {
        List<String> violations = PackageGate.of(CodeUtilsApi.class, WORLD)
            .violations("com/mrleonardos/codeutils/internal/queue");

        assertTrue(
            violations.isEmpty(),
            () -> "решение об отказе принимается вне главного потока и в мир не ходит:\n"
                + String.join("\n", violations));
    }

    @Test
    void theGateWouldNoticeSuchAReferenceInAnyOtherPackage() throws IOException {
        List<String> found = PackageGate.of(CodeUtilsApi.class, WORLD)
            .violations("com/mrleonardos/codeutils/internal/clean");

        assertFalse(found.isEmpty(), "у очистки ссылка на проход по миру есть, значит гейт смотрит на то самое");
    }

    @Test
    void aDecisionMadeOffTheMainThreadAnswersJustTheSame() throws InterruptedException {
        QueueFile file = new QueueFile();
        UtilsRegistry registry = new UtilsRegistry();
        registry.addPolicy(QueueFile.BY_PERMISSION, new ByPermission());
        QueueGate gate = new QueueGate(() -> file, registry, new FakeRights(), LogManager.getLogger("codeutils-test"));
        gate.arm(60, 200);
        AtomicReference<QueueGate.Answer> answer = new AtomicReference<>();

        Thread netty = new Thread(
            () -> answer.set(gate.decide(UUID.nameUUIDFromBytes("Plain".getBytes()), "Plain", 1_700_000_000_000L)),
            "netty-like");
        netty.start();
        netty.join();

        assertFalse(
            answer.get()
                .allowed(),
            "потолок посчитан своим счётчиком, а не списком игроков сервера");
    }
}
