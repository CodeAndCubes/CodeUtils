package com.mrleonardos.codeutils.platform;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.DimensionManager;

import com.mrleonardos.codecore.api.CodeApi;
import com.mrleonardos.codecore.api.actor.PlayerRef;
import com.mrleonardos.codecore.api.service.PermissionService;
import com.mrleonardos.codecore.platform.PlayerRefs;
import com.mrleonardos.codecore.platform.Players;
import com.mrleonardos.codeutils.internal.ServerFacts;

public final class ServerFactsImpl implements ServerFacts {

    static final int NO_DIMENSION = Integer.MIN_VALUE;

    private static final double NANOS_IN_MILLI = 1.0E6D;

    @Override
    public long now() {
        return System.currentTimeMillis();
    }

    @Override
    public List<PlayerRef> online() {
        return PlayerRefs.allOnline();
    }

    @Override
    public int dimensionOf(UUID player) {
        EntityPlayerMP found = Players.online(player);
        return found == null ? NO_DIMENSION : found.dimension;
    }

    @Override
    public Set<Integer> dimensions() {
        Set<Integer> loaded = new LinkedHashSet<>();
        Integer[] ids = DimensionManager.getIDs();
        if (ids != null) {
            for (Integer id : ids) {
                loaded.add(id);
            }
        }
        return loaded;
    }

    @Override
    public double tickMillis() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return 0.0D;
        }
        return MathHelper.average(server.tickTimeArray) / NANOS_IN_MILLI;
    }

    @Override
    public boolean allowed(UUID player, String node) {
        PermissionService rights = CodeApi.services()
            .find(PermissionService.class)
            .orElse(null);
        return rights != null && rights.has(player, node);
    }
}
