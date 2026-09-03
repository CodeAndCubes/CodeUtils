package com.mrleonardos.codeutils.platform;

import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import io.netty.buffer.ByteBuf;

public final class SpawnCommandBlock extends CommandBlockLogic {

    public static final String NAME = "CodeUtils";

    private static final int PERMISSION_LEVEL = 2;

    public SpawnCommandBlock() {
        func_145754_b(NAME);
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates() {
        World world = getEntityWorld();
        return world == null ? new ChunkCoordinates(0, 0, 0) : world.getSpawnPoint();
    }

    @Override
    public World getEntityWorld() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return null;
        }
        WorldServer[] worlds = server.worldServers;
        return worlds == null || worlds.length == 0 ? null : worlds[0];
    }

    @Override
    public boolean canCommandSenderUseCommand(int permissionLevel, String command) {
        return permissionLevel <= PERMISSION_LEVEL;
    }

    @Override
    public void func_145756_e() {}

    @Override
    public int func_145751_f() {
        return 0;
    }

    @Override
    public void func_145757_a(ByteBuf buffer) {}
}
