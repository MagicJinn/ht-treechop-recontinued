package ht.treechop.compat.polymer;

import ht.treechop.common.network.ServerUpdateChopsPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Detects Polymer presence and TreeChop clients.
 * (safe to load when Polymer is absent).
 */
public final class TreeChopPolymerClients {
    public static boolean isPolymerLoaded() {
        return FabricLoader.getInstance().isModLoaded("polymer-bundled")
                || FabricLoader.getInstance().isModLoaded("polymer-core");
    }

    /** Checks if the player has the TreeChop mod installed. */
    public static boolean hasMod(@Nullable ServerPlayer player) {
        return player != null && ServerPlayNetworking.canSend(player, ServerUpdateChopsPacket.TYPE);
    }

    public static boolean shouldSendPacket(ServerPlayer player, CustomPacketPayload payload) {
        return player == null || ServerPlayNetworking.canSend(player, payload.type());
    }
}
