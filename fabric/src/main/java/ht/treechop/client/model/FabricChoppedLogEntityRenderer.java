package ht.treechop.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import ht.treechop.common.block.FabricChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sodium (no Indium) fallback. Chunk mesh is empty
 * ({@link HiddenChoppedLogBakedModel}), so chopped geometry is submitted from
 * the BER each frame.
 */
public class FabricChoppedLogEntityRenderer implements
        BlockEntityRenderer<FabricChoppedLogBlock.MyEntity, FabricChoppedLogEntityRenderer.ChoppedLogRenderState> {
    private final RandomSource random = RandomSource.create();

    public FabricChoppedLogEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public ChoppedLogRenderState createRenderState() {
        return new ChoppedLogRenderState();
    }

    @Override
    public void extractRenderState(FabricChoppedLogBlock.MyEntity entity, ChoppedLogRenderState state,
            float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);

        Level level = entity.getLevel();
        state.parts.clear();
        if (level == null)
            return;

        BlockPos pos = entity.getBlockPos();
        var strippedState = ChopUtil.getStrippedState(level, pos, entity.getOriginalState());
        Map<net.minecraft.core.Direction, net.minecraft.world.level.block.state.BlockState> neighbors = ChoppedLogBakedModel
                .getStrippedNeighbors(level, pos, entity);

        List<BakedQuad> quads = ChoppedLogBakedModel.getQuads(
                strippedState,
                entity.getShape(),
                entity.getRadius(),
                random,
                neighbors).toList();

        state.parts.add(ChoppedLogBakedModel.createModelPart(quads,
                ChoppedLogBakedModel.getBlockModel(strippedState).particleMaterial()));
    }

    @Override
    public void submit(ChoppedLogRenderState state, @NotNull PoseStack poseStack,
            @NotNull SubmitNodeCollector collector, @NotNull CameraRenderState camera) {
        if (state.parts.isEmpty()) {
            return;
        }
        collector.submitBlockModel(
                poseStack,
                RenderTypes.cutoutMovingBlock(),
                state.parts,
                new int[0],
                state.lightCoords,
                0,
                0);
    }

    public static class ChoppedLogRenderState extends BlockEntityRenderState {
        public final List<BlockStateModelPart> parts = new ArrayList<>();
    }
}
