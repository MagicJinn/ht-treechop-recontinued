package ht.treechop.server;

import ht.treechop.common.config.ConfigHandler;
import ht.treechop.common.network.ServerUpdateChopsPacket;
import ht.treechop.common.settings.ChopSettings;
import ht.treechop.common.settings.ChoppingEntity;
import ht.treechop.common.settings.SyncedChopData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public abstract class Server {

    protected static Server instance;

    private final ThreadLocal<List<PendingChopUpdate>> pendingChopUpdates = new ThreadLocal<>();

    public static ChopSettings getDefaultPlayerSettings() {
        return new ChopSettings(ConfigHandler.defaultChopSettings.get());
    }

    public SyncedChopData getPlayerChopData(Player player) {
        ChoppingEntity chopper = (ChoppingEntity) player;
        if (chopper.getChopData() == null) {
            chopper.setChopData(new SyncedChopData(getDefaultPlayerSettings()));
        }
        return chopper.getChopData();
    }

    /**
     * Coalesces chop-block syncs from {@code action} into one packet per tracking
     * player.
     * Nested calls reuse the outer batch.
     */
    public void runWithChopSyncBatch(Runnable action) {
        if (pendingChopUpdates.get() != null) {
            action.run();
            return;
        }

        List<PendingChopUpdate> batch = new ArrayList<>();
        pendingChopUpdates.set(batch);
        try {
            action.run();
            flushChopUpdates(batch);
        } finally {
            pendingChopUpdates.remove();
        }
    }

    public void syncChopUpdate(ServerLevel level, BlockPos pos, CompoundTag tag) {
        List<PendingChopUpdate> batch = pendingChopUpdates.get();
        if (batch != null) {
            batch.add(new PendingChopUpdate(level, pos, tag));
        } else {
            flushChopUpdates(List.of(new PendingChopUpdate(level, pos, tag)));
        }
    }

    private void flushChopUpdates(List<PendingChopUpdate> updates) {
        if (updates.isEmpty())
            return;

        Map<ServerPlayer, List<ServerUpdateChopsPacket.Entry>> byPlayer = new IdentityHashMap<>();
        for (PendingChopUpdate update : updates) {
            ServerUpdateChopsPacket.Entry entry = new ServerUpdateChopsPacket.Entry(update.pos(), update.tag());
            for (ServerPlayer player : trackingPlayers(update.level(), update.pos())) {
                byPlayer.computeIfAbsent(player, ignored -> new ArrayList<>()).add(entry);
            }
        }

        byPlayer.forEach((player, entries) -> sendTo(player, new ServerUpdateChopsPacket(entries)));
    }

    protected abstract Iterable<ServerPlayer> trackingPlayers(ServerLevel level, BlockPos pos);

    public abstract void broadcast(ServerLevel level, BlockPos pos, CustomPacketPayload payload);

    public abstract void sendTo(ServerPlayer player, CustomPacketPayload payload);

    public static Server instance() {
        return instance;
    }

    private record PendingChopUpdate(ServerLevel level, BlockPos pos, CompoundTag tag) {
    }
}
