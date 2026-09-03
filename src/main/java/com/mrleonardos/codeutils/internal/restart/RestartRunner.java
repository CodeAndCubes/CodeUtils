package com.mrleonardos.codeutils.internal.restart;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codecore.api.actor.PlayerRef;
import com.mrleonardos.codecore.api.util.Scheduler;
import com.mrleonardos.codeutils.api.UtilsRegistry;
import com.mrleonardos.codeutils.api.restart.RestartPhase;
import com.mrleonardos.codeutils.api.restart.RestartPlanView;
import com.mrleonardos.codeutils.api.restart.RestartStep;

public final class RestartRunner {

    private final Supplier<RestartFile> file;
    private final UtilsRegistry registry;
    private final Shutdown shutdown;
    private final Scheduler scheduler;
    private final Runnable stopTick;
    private final Logger log;

    private boolean stopping;

    public RestartRunner(Supplier<RestartFile> file, UtilsRegistry registry, Shutdown shutdown, Scheduler scheduler,
        Runnable stopTick, Logger log) {
        this.file = file;
        this.registry = registry;
        this.shutdown = shutdown;
        this.scheduler = scheduler;
        this.stopTick = stopTick;
        this.log = log;
    }

    public boolean stopping() {
        return stopping;
    }

    public void begin(RestartPlanView plan) {
        if (stopping) {
            return;
        }
        stopping = true;
        stopTick.run();
        steps(RestartPhase.BEFORE_KICK, plan);
        kickEveryone();
        scheduler.afterTicks(Math.max(0, file.get().steps.settleTicks), () -> settled(plan));
    }

    private void settled(RestartPlanView plan) {
        List<PlayerRef> left = shutdown.online();
        if (!left.isEmpty()) {
            log.warn(
                "Restart: {} player(s) are still on the server after the kick: {}",
                Integer.valueOf(left.size()),
                nicks(left));
            kickEveryone();
            left = shutdown.online();
            if (!left.isEmpty()) {
                log.warn(
                    "Restart: {} player(s) did not leave, the server stops anyway: {}",
                    Integer.valueOf(left.size()),
                    nicks(left));
            }
        }
        steps(RestartPhase.BEFORE_SAVE, plan);
        RestartFile.Steps written = file.get().steps;
        if (written.savePlayers) {
            shutdown.savePlayers();
        }
        if (written.saveWorlds) {
            shutdown.saveWorlds();
        }
        steps(RestartPhase.AFTER_SAVE, plan);
        shutdown.stop();
    }

    private void kickEveryone() {
        String reasonKey = file.get().messages.kickKey;
        for (PlayerRef player : shutdown.online()) {
            shutdown.closeScreen(player);
            shutdown.kick(player, reasonKey);
        }
    }

    void steps(RestartPhase phase, RestartPlanView plan) {
        for (RestartStep step : registry.steps(phase)) {
            log.info("Restart step {} of phase {} is running", step.name(), phase.name());
            try {
                step.run(plan);
            } catch (RuntimeException failure) {
                log.warn(
                    "Restart step {} failed, the server stops anyway: {}",
                    step.name(),
                    failure.toString(),
                    failure);
            }
        }
    }

    private static String nicks(List<PlayerRef> players) {
        List<String> names = new ArrayList<>();
        for (PlayerRef player : players) {
            names.add(player.name());
        }
        return String.join(", ", names);
    }
}
