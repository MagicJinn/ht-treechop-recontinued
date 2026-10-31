package ht.treechop.common.block;

import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.utils.PolymerClientDecoded;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.compat.polymer.ChoppedLogDisplay;
import ht.treechop.compat.polymer.TreeChopPolymer;
import ht.treechop.compat.polymer.TreeChopPolymerClients;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Polymer stand-in. "Empty" leaves ({@code requestEmpty(LEAVES)}) +
 * BlockDisplay. Needs to be a transparent block, so neighbors are not culled.
 * Breaks at speed of a log.
 */
public class PolymerFabricChoppedLogBlock extends FabricChoppedLogBlock
        implements PolymerTexturedBlock, PolymerClientDecoded, BlockWithElementHolder {

    public PolymerFabricChoppedLogBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public boolean canSyncRawToClient(PacketContext context) {
        ServerPlayer player = PolymerCommonUtils.getPlayer(context);
        return player == null || TreeChopPolymerClients.hasMod(player);
    }

    /**
     * Vanilla Polymer clients mine at leaves speed if left to the client. Server
     * mining uses the real chopped-log destroy progress (wood). TreeChop clients
     * mine locally.
     */
    @Override
    public boolean handleMiningOnServer(ItemStack tool, BlockState state, BlockPos pos, ServerPlayer player) {
        return !TreeChopPolymerClients.hasMod(player);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        ServerPlayer player = PolymerCommonUtils.getPlayer(context);
        if (player == null)
            return Blocks.OAK_LOG.defaultBlockState();

        HitResult hit = player.pick(player.blockInteractionRange(), 0f, false);
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            return ChopUtil.phaseChangeFxState(player.level(), pos, player.level().getBlockState(pos));
        }
        return Blocks.OAK_LOG.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, @Nullable PacketContext context) {
        if (context == null || TreeChopPolymerClients.hasMod(PolymerCommonUtils.getPlayer(context)))
            return state;

        boolean waterlogged = state.hasProperty(WATERLOGGED) && state.getValue(WATERLOGGED);
        return TreeChopPolymer.emptyStandIn(waterlogged);
    }

    @Override
    public Block getPolymerReplacement(Block block, PacketContext context) {
        if (TreeChopPolymerClients.hasMod(PolymerCommonUtils.getPlayer(context)))
            return this;

        // Same empty-leaves block type either way. Waterlogging is a blockState, not a
        // new block.
        return TreeChopPolymer.emptyStandIn(false).getBlock();
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new ChoppedLogDisplay();
    }
}
