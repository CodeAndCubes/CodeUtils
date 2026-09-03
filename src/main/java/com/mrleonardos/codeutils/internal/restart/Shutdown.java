package com.mrleonardos.codeutils.internal.restart;

import java.util.List;

import com.mrleonardos.codecore.api.actor.PlayerRef;

public interface Shutdown {

    void closeDoor(String reasonKey);

    void openDoor();

    boolean doorClosed();

    List<PlayerRef> online();

    void closeScreen(PlayerRef player);

    void kick(PlayerRef player, String reasonKey);

    void savePlayers();

    void saveWorlds();

    void stop();
}
