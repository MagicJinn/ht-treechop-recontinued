package ht.treechop.mixin;

import ht.treechop.TreeChopException;
import ht.treechop.api.TreeData;
import ht.treechop.client.Client;
import ht.treechop.client.gui.screen.ChopIndicator;
import ht.treechop.common.chop.ChopUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client mining feedback for TreeChop. When a chop does not fell the tree, the
 * block stays at the same position. Vanilla {@code continueDestroyBlock} then
 * resumes via {@code sameDestroyTarget} without calling
 * {@code startDestroyBlock}, so hit particles, break sounds, and
 * START_DESTROY_BLOCK never fire again until the player releases and re-clicks
 * (MC-255057). Require {@code isDestroying} so a new start runs after each
 * chop.
 */
@Mixin(value = MultiPlayerGameMode.class, remap = false)
public abstract class MultiPlayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private boolean isDestroying;

    @Shadow
    private boolean sameDestroyTarget(BlockPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true, remap = false)
    public void dontPredictBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> info) {
        try {
            Player player = minecraft.player;
            Level level = minecraft.level;
            if (player != null && level != null) {
                if (ChopUtil.playerWantsToChop(player, Client.getChopSettings()) && ChopIndicator.blockCanBeChopped(pos)) {
                    TreeData tree = Client.treeCache.getTree(level, pos);
                    BlockState state = level.getBlockState(pos);
                    boolean felled = tree.readyToFell(tree.getChops() + ChopUtil.getNumChopsByTool(player.getMainHandItem(), state));

                    if (!felled) {
                        ChopUtil.thwack(player, level, pos, state);
                        info.setReturnValue(false);
                    }
                }
            }
        } catch (TreeChopException e) {
            // Ignore
        }
    }

    @Redirect(method = "continueDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;sameDestroyTarget(Lnet/minecraft/core/BlockPos;)Z"), remap = false)
    private boolean sameDestroyTargetOnlyWhileDestroying(MultiPlayerGameMode self, BlockPos pos) {
        return sameDestroyTarget(pos) && this.isDestroying;
    }
}
