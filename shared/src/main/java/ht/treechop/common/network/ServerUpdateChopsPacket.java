package ht.treechop.common.network;

import ht.treechop.TreeChop;
import ht.treechop.client.Client;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerUpdateChopsPacket implements CustomPacketPayload {
    public static final Identifier ID = TreeChop.resource("server_update_chops");
    public static final CustomPacketPayload.Type<ServerUpdateChopsPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, ServerUpdateChopsPacket> STREAM_CODEC = CustomPacketPayload.codec(
            ServerUpdateChopsPacket::encode, ServerUpdateChopsPacket::decode
    );

    private final List<Entry> updates;

    public ServerUpdateChopsPacket(BlockPos pos, CompoundTag tag) {
        this(Collections.singletonList(new Entry(pos, tag)));
    }

    public ServerUpdateChopsPacket(List<Entry> updates) {
        this.updates = updates;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(updates.size());
        for (Entry entry : updates) {
            buffer.writeBlockPos(entry.pos());
            buffer.writeNbt(entry.tag());
        }
    }

    public static ServerUpdateChopsPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<Entry> updates = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BlockPos pos = buffer.readBlockPos();
            CompoundTag tag = buffer.readNbt();
            updates.add(new Entry(pos, tag));
        }
        return new ServerUpdateChopsPacket(updates);
    }

    public void handle() {
        for (Entry entry : updates) {
            Client.handleUpdateChopsPacket(entry.pos(), entry.tag());
        }
    }

    public List<Entry> getUpdates() {
        return updates;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(BlockPos pos, CompoundTag tag) {
    }
}
