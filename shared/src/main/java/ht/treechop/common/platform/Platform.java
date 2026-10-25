package ht.treechop.common.platform;

import ht.treechop.api.ChopData;
import ht.treechop.api.ChopDataImmutable;
import ht.treechop.api.FellData;
import ht.treechop.api.TreeData;
import ht.treechop.common.chop.ChopResult;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public interface Platform {

    boolean isDedicatedServer();

    boolean uses(ModLoader loader);

    TreeData detectTreeEvent(Level level, ServerPlayer player, BlockPos blockPos, BlockState blockState, TreeData treeData);

    boolean startChopEvent(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState blockState, ChopData chopData, Object trigger);

    void finishChopEvent(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState blockState, ChopDataImmutable chopData, ChopResult chopResult);

    Block getChoppedLogBlock();

    BlockEntityType<?> getChoppedLogBlockEntity();

    Identifier getIdentifierForBlock(Block block);

    Identifier getIdentifierForItem(Item item);

    BlockState getStrippedState(BlockGetter level, BlockPos pos, BlockState state);

    boolean startFellTreeEvent(ServerPlayer player, Level level, BlockPos choppedPos, FellData fellData);

    void finishFellTreeEvent(ServerPlayer player, Level level, BlockPos choppedPos, FellData fellData);

    /**
     * Extra phase-change FX for clients without TreeChop (Polymer).
     * TreeChop clients already get sound/particles from client prediction.
     */
    default void playPhaseChangeFeedback(ServerPlayer player, BlockPos pos, BlockState fxState) {
    }

    /**
     * Refresh Polymer chopped-log block after BE original state / chops are set.
     */
    default void refreshChoppedLogDisplay(Level level, BlockPos pos) {
        // No-op when Polymer is absent.
    }
}
