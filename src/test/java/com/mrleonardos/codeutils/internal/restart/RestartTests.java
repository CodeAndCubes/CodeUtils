package com.mrleonardos.codeutils.internal.restart;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.restart.RestartPhase;
import com.mrleonardos.codeutils.api.restart.RestartPlanView;
import com.mrleonardos.codeutils.api.restart.RestartStep;
import com.mrleonardos.codeutils.api.when.ServerSnapshot;
import com.mrleonardos.codeutils.internal.Announcer;
import com.mrleonardos.codeutils.internal.Clocks;
import com.mrleonardos.codeutils.internal.Conditions;
import com.mrleonardos.codeutils.internal.FakeFacts;
import com.mrleonardos.codeutils.internal.FakeScheduler;
import com.mrleonardos.codeutils.internal.FakeTexts;
import com.mrleonardos.codeutils.internal.broadcast.BroadcastsFile;
import com.mrleonardos.codeutils.internal.broadcast.RecordingSink;

/** Общая обвязка тестов остановки: поддельный сервер, поддельные часы и записывающий приёмник. */
abstract class RestartTests {

    static final Logger LOG = LogManager.getLogger("codeutils-test");

    static final ZoneId ZONE = ZoneId.of("UTC");

    static final long SECOND = Clocks.MILLIS;

    final UtilsRegistry registry = new UtilsRegistry();
    final FakeShutdown shutdown = new FakeShutdown();
    final FakeScheduler scheduler = new FakeScheduler();
    final FakeFacts facts = new FakeFacts();
    final RecordingSink sink = new RecordingSink();
    final RestartFile file = new RestartFile();

    boolean tickStopped;

    RestartTests() {
        registry.addSink(BroadcastsFile.SINK_CHAT, sink);
        facts.join("Watcher", 0);
    }

    final RestartRunner runner() {
        return new RestartRunner(() -> file, registry, shutdown, scheduler, () -> tickStopped = true, LOG);
    }

    final RestartPlan plan(RestartRunner runner) {
        RestartPlan plan = new RestartPlan(
            () -> file,
            registry,
            new Conditions(registry),
            new Announcer(facts, new FakeTexts()),
            shutdown,
            runner,
            () -> ZONE,
            LOG);
        plan.arm(0L);
        return plan;
    }

    final void step(String name, RestartPhase phase) {
        registry.addStep(new RestartStep() {

            @Override
            public RestartPhase phase() {
                return phase;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public void run(RestartPlanView plan) {
                shutdown.journal()
                    .add("step " + name);
            }
        });
    }

    final void failingStep(String name, RestartPhase phase) {
        registry.addStep(new RestartStep() {

            @Override
            public RestartPhase phase() {
                return phase;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public void run(RestartPlanView plan) {
                shutdown.journal()
                    .add("step " + name);
                throw new IllegalStateException("шаг чужого мода упал");
            }
        });
    }

    final void warnAt(int... seconds) {
        List<Integer> written = new ArrayList<>();
        for (int second : seconds) {
            written.add(Integer.valueOf(second));
        }
        file.warnings.seconds = written;
    }

    final List<String> warnings() {
        return new ArrayList<>(sink.texts());
    }

    final ServerSnapshot snapshot(String time) {
        return ServerSnapshot.of(
            facts.online()
                .size(),
            new LinkedHashSet<>(),
            20.0D,
            LocalDateTime.parse(time));
    }

    static long at(String time) {
        return Clocks.millisOf(LocalDateTime.parse(time), ZONE);
    }

    static List<String> list(String... values) {
        return Arrays.asList(values);
    }
}
