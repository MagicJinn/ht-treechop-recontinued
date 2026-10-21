package ht.treechop.client.model;

import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.properties.ChoppedLogShape;
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
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Empty chunk mesh used with Sodium so a BER can draw chopped geometry instead.
 * Still emits quads for the destroy-crack overlay (Fabric passes
 * {@link BlockAndTintGetter#EMPTY}).
 */
public class HiddenChoppedLogBakedModel implements BlockStateModel {
    private final Material.Baked particleMaterial;

    public HiddenChoppedLogBakedModel() {
        this(ChoppedLogBakedModel.getDefaultSpriteAsMaterial());
    }

    public HiddenChoppedLogBakedModel(Material.Baked particleMaterial) {
        this.particleMaterial = particleMaterial;
    }

    public HiddenChoppedLogBakedModel(BlockStateModel ignoredWrapped) {
        this();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        // Chunk meshing stays empty. BER draws the block. Overlay uses emitQuads.
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state,
            RandomSource random, Predicate<@Nullable Direction> cullTest) {
        // emitQuads is only used for the destroy-crack overlay here (chunk mesh is
        // empty). Omit stripped-neighbor faces so the crack stays on this block.
        BlockAndTintGetter geometryLevel = resolveGeometryLevel(level, pos);
        Stream<BakedQuad> quads;
        if (geometryLevel.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity entity) {
            BlockState strippedState = ChopUtil.getStrippedState(geometryLevel, pos, entity.getOriginalState());
            quads = ChoppedLogBakedModel.getQuads(
                    strippedState,
                    entity.getShape(),
                    entity.getRadius(),
                    random,
                    Map.of());
        } else {
            quads = ChoppedLogBakedModel.getQuads(
                    Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                    ChoppedLogShape.PILLAR_Y,
                    8,
                    random,
                    Map.of());
        }
        // Destroy overlay only. Keep crack on this block (no stripped-neighbor faces).
        // Do not cull by neighbor solidity: shrunk faces stay visible in the gap.
        quads.forEach(quad -> {
            Direction lightFace = quad.direction();
            emitter.fromBakedQuad(quad);
            emitter.cullFace(null);
            emitter.nominalFace(lightFace);
            emitter.emit();
        });
    }

    private static BlockAndTintGetter resolveGeometryLevel(BlockAndTintGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity) {
            return level;
        }
        var clientLevel = Minecraft.getInstance().level;
        return clientLevel != null ? clientLevel : level;
    }

    @Override
    public Material.Baked particleMaterial() {
        return particleMaterial;
    }

    @Override
    public int materialFlags() {
        return 0;
    }
}
