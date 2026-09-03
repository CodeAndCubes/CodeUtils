package com.mrleonardos.codeutils.internal.restart;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mrleonardos.codecore.api.actor.PlayerRef;

/** Остановка сервера, которой не было: журнал вызовов по порядку вместо кика и записи миров. */
public final class FakeShutdown implements Shutdown {

    public static final String DOOR_CLOSED = "door closed";
    public static final String DOOR_OPEN = "door open";
    public static final String PLAYERS_SAVED = "players saved";
    public static final String WORLDS_SAVED = "worlds saved";
    public static final String STOPPED = "stopped";

    private final List<String> journal = new ArrayList<>();
    private final List<PlayerRef> online = new ArrayList<>();
    private final Set<String> stubborn = new LinkedHashSet<>();

    private boolean closed;
    private String doorKey = "";

    public PlayerRef join(String name) {
        PlayerRef player = PlayerRef.of(UUID.nameUUIDFromBytes(name.getBytes()), name);
        online.add(player);
        return player;
    }

    /** Игрок, который переживает первый кик: так проверяется повтор кика после паузы. */
    public FakeShutdown stubborn(String name) {
        stubborn.add(name);
        return this;
    }

    public List<String> journal() {
        return journal;
    }

    public String doorKey() {
        return doorKey;
    }

    @Override
    public void closeDoor(String reasonKey) {
        doorKey = reasonKey;
        closed = true;
        journal.add(DOOR_CLOSED);
    }

    @Override
    public void openDoor() {
        closed = false;
        journal.add(DOOR_OPEN);
    }

    @Override
    public boolean doorClosed() {
        return closed;
    }

    @Override
    public List<PlayerRef> online() {
        return new ArrayList<>(online);
    }

    @Override
    public void closeScreen(PlayerRef player) {
        journal.add("screen " + player.name());
    }

    @Override
    public void kick(PlayerRef player, String reasonKey) {
        journal.add("kick " + player.name());
        if (stubborn.remove(player.name())) {
            return;
        }
        online.remove(player);
    }

    @Override
    public void savePlayers() {
        journal.add(PLAYERS_SAVED);
    }

    @Override
    public void saveWorlds() {
        journal.add(WORLDS_SAVED);
    }

    @Override
    public void stop() {
        journal.add(STOPPED);
    }
}
