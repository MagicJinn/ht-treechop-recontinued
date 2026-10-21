package ht.treechop.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * NeoForge's destroy overlay calls {@code collectParts(EMPTY, ZERO, ...)}, so dynamic
 * models cannot recover chopped-log radius/shape and fall back to a full cube.
 * <p>
 * Only the break position is fixed here. {@code EMPTY} is kept so models can tell this
 * is the destroy overlay (and skip neighbor faces that would paint cracks onto adjacent
 * blocks). Geometry still resolves the real client level internally.
 */
@Mixin(value = LevelRenderer.class, remap = false)
public class LevelRendererDestroyOverlayMixin {
    @WrapOperation(
            method = "submitBlockDestroyAnimation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;collectParts(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Ljava/util/List;)V"
            ),
            remap = false
    )
    private void collectPartsWithDestroyPos(
            BlockStateModel model,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> parts,
            Operation<Void> original,
            @Local(ordinal = 0) BlockPos destroyPos
    ) {
        original.call(model, level, destroyPos, state, random, parts);
    }
}
