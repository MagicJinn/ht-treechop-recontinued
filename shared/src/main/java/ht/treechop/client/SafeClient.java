package ht.treechop.client;

import ht.treechop.TreeChop;
import ht.treechop.common.network.ServerUpdateChopsPacket;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Expose {@link Client} methods without loading client-only code.
 */
public class SafeClient {

    public static void handleUpdateChopsPacket(ServerUpdateChopsPacket message) {
        if (safe()) {
            Client.handleUpdateChopsPacket(message.getPos(), message.getTag());
        }
    }

    public static void rerenderChoppedLog(Level level, BlockPos pos, BlockState state) {
        if (safe() && level instanceof ClientLevel clientLevel) {
            clientLevel.setBlocksDirty(pos, Blocks.AIR.defaultBlockState(), state);
            Client.treeCache.invalidate();
        }
    }

    private static boolean safe() {
        return !TreeChop.platform.isDedicatedServer();
    }

}
