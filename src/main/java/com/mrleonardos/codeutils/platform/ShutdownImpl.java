package com.mrleonardos.codeutils.platform;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;

import org.apache.logging.log4j.Logger;

import com.mrleonardos.codecore.api.actor.PlayerRef;
import com.mrleonardos.codecore.platform.PlayerRefs;
import com.mrleonardos.codeutils.internal.UtilsTexts;
import com.mrleonardos.codeutils.internal.restart.Shutdown;

public final class ShutdownImpl implements Shutdown {

    private final LoginDoor door;
    private final UtilsTexts texts;
    private final Logger log;

    public ShutdownImpl(LoginDoor door, UtilsTexts texts, Logger log) {
        this.door = door;
        this.texts = texts;
        this.log = log;
    }

    @Override
    public void closeDoor(String reasonKey) {
        door.close(reasonKey);
    }

    @Override
    public void openDoor() {
        door.open();
    }

    @Override
    public boolean doorClosed() {
        return door.closed();
    }

    @Override
    public List<PlayerRef> online() {
        return PlayerRefs.allOnline();
    }

    @Override
    public void closeScreen(PlayerRef player) {
        EntityPlayerMP found = PlayerRefs.online(player);
        if (found != null) {
            found.closeScreen();
        }
    }

    @Override
    public void kick(PlayerRef player, String reasonKey) {
        EntityPlayerMP found = PlayerRefs.online(player);
        if (found != null && found.playerNetServerHandler != null) {
            found.playerNetServerHandler.kickPlayerFromServer(texts.format(reasonKey));
        }
    }

    @Override
    public void savePlayers() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return;
        }
        server.getConfigurationManager()
            .saveAllPlayerData();
    }

    @Override
    public void saveWorlds() {
        for (WorldServer world : worlds()) {
            try {
                world.saveAllChunks(true, null);
            } catch (MinecraftException failure) {
                log.warn(
                    "World {} was not written: {}",
                    world.provider.getDimensionName(),
                    failure.getMessage(),
                    failure);
            }
        }
    }

    @Override
    public void stop() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            server.initiateShutdown();
        }
    }

    private static List<WorldServer> worlds() {
        MinecraftServer server = MinecraftServer.getServer();
        List<WorldServer> found = new ArrayList<>();
        if (server == null || server.worldServers == null) {
            return found;
        }
        for (WorldServer world : server.worldServers) {
            if (world != null) {
                found.add(world);
            }
        }
        return found;
    }
}
