package ht.treechop.client.model;

import ht.treechop.common.block.ChoppedLogBlock.MyEntity;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.properties.ChoppedLogShape;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class FabricChoppedLogBakedModel extends WrapperBlockStateModel {
    public FabricChoppedLogBakedModel(BlockStateModel wrapped) {
        super(wrapped);
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state,
            RandomSource random, Predicate<@Nullable Direction> cullTest) {
        // Fabric's destroy overlay calls emitQuads with BlockAndTintGetter.EMPTY (no
        // BEs). Prefer the real client level so crack geometry matches the chopped log.
        // Do not emit stripped-neighbor faces on that path. They sit on interior sides
        // and would get the crack overlay painted onto adjacent logs.
        boolean destroyOverlay = !(level.getBlockEntity(pos) instanceof MyEntity);
        BlockAndTintGetter geometryLevel = resolveGeometryLevel(level, pos);
        MyEntity entity = geometryLevel.getBlockEntity(pos) instanceof MyEntity e
                ? e
                : null;

        Stream<BakedQuad> bodyQuads;
        Stream<BakedQuad> neighborQuads = Stream.empty();
        if (entity != null) {
            BlockState strippedState = ChopUtil.getStrippedState(geometryLevel, pos, entity.getOriginalState());
            bodyQuads = ChoppedLogBakedModel.getBodyQuads(
                    strippedState, entity.getShape(), entity.getRadius(), random);
            if (!destroyOverlay) {
                neighborQuads = ChoppedLogBakedModel.getStrippedNeighborQuads(
                        random, ChoppedLogBakedModel.getStrippedNeighbors(geometryLevel, pos, entity));
            }
        } else {
            bodyQuads = fallbackQuads(random);
        }

        // Never cull by neighbor solidity. The mesh shrinks as chops accumulate, so
        // faces that once touched a solid block become visible (NeoForge uses
        // addUnculledFace for the same reason). fromBakedQuad copies direction as
        // cullFace, which must be cleared or the mesher still drops them.
        Stream.concat(bodyQuads, neighborQuads).forEach(quad -> {
            Direction lightFace = quad.direction();
            emitter.fromBakedQuad(quad);
            emitter.cullFace(null);
            emitter.nominalFace(lightFace);
            emitter.emit();
        });
    }

    /**
     * Geometry used when no chopped-log BE is available. Required for the cracking
     * overlay. Fabric cancels collectParts and only uses emitQuads.
     */
    private static Stream<BakedQuad> fallbackQuads(RandomSource random) {
        return ChoppedLogBakedModel.getQuads(
                Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                ChoppedLogShape.PILLAR_Y,
                8,
                random,
                Map.of());
    }

    private static BlockAndTintGetter resolveGeometryLevel(BlockAndTintGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MyEntity)
            return level;

        var clientLevel = Minecraft.getInstance().level;
        return clientLevel != null ? clientLevel : level;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        List<BakedQuad> quads = fallbackQuads(random).collect(Collectors.toList());
        parts.add(ChoppedLogBakedModel.createModelPart(quads, particleMaterial()));
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof MyEntity entity) {
            BlockState strippedState = ChopUtil.getStrippedState(level, pos, entity.getOriginalState());
            return ChoppedLogBakedModel.getBlockModel(strippedState).particleMaterial();
        }
        return particleMaterial();
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof MyEntity entity)
            return List.of(
                    entity.getRadius(),
                    entity.getShape(),
                    entity.getOriginalState(),
                    ChoppedLogBakedModel.getStrippedNeighbors(level, pos, entity));

        return Objects.hash(pos, state);
    }
}
