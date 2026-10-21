package ht.treechop.client.model;

import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.properties.ChoppedLogShape;
import ht.tuber.math.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChoppedLogBakedModel {
    private static Material.Baked defaultParticleMaterial;
    protected static final Identifier DEFAULT_TEXTURE_RESOURCE = Identifier
            .withDefaultNamespace("block/stripped_oak_log");
    /**
     * Prefer cutout over translucent (shaders). Quads also carry layer in
     * {@link BakedQuad.MaterialInfo}.
     */
    public static final ChunkSectionLayer CHUNK_LAYER = ChunkSectionLayer.CUTOUT;

    public static void setDefaultParticleMaterial(Material.Baked material) {
        ChoppedLogBakedModel.defaultParticleMaterial = material;
    }

    public static Material.Baked getDefaultParticleMaterial() {
        if (defaultParticleMaterial == null)
            defaultParticleMaterial = getBlockModel(Blocks.STRIPPED_OAK_LOG.defaultBlockState()).particleMaterial();

        return defaultParticleMaterial;
    }

    private static BlockState getStrippedNeighbor(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        return ChopUtil.getStrippedState(level, neighborPos, level.getBlockState(neighborPos));
    }

    public static Map<Direction, BlockState> getStrippedNeighbors(BlockGetter level, BlockPos pos,
            ChoppedLogBlock.MyEntity entity) {
        if (entity.getOriginalState().isSolidRender()) {
            return entity.streamSolidSides(level, pos).collect(Collectors.toMap(
                    side -> side,
                    side -> getStrippedNeighbor(level, pos, side)
            ));
        } else {
            return Collections.emptyMap();
        }
    }

    protected static List<BakedQuad> getBlockQuads(BlockState blockState, Direction side, RandomSource rand) {
        BlockStateModel model = getBlockModel(blockState);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(rand, parts);

        List<BakedQuad> quads = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            quads.addAll(part.getQuads(side));
        }
        return quads;
    }

    public static BlockStateModel getBlockModel(BlockState blockState) {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState);
    }

    public static Material.Baked getDefaultSpriteAsMaterial() {
        if (defaultParticleMaterial != null)
            return defaultParticleMaterial;

        var sprite = Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(DEFAULT_TEXTURE_RESOURCE);
        return new Material.Baked(sprite, false);
    }

    /**
     * Build a single model part from generated chopped quads (all unculled,
     * matching old side==null emission).
     */
    public static BlockStateModelPart createModelPart(List<BakedQuad> quads, Material.Baked particleMaterial) {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (BakedQuad quad : quads) {
            builder.addUnculledFace(quad);
        }
        return new SimpleModelWrapper(builder.build(), true, particleMaterial);
    }

    public static Stream<BakedQuad> getQuads(BlockState strippedState, ChoppedLogShape shape, int radius,
            RandomSource random, Map<Direction, BlockState> strippedNeighbors) {
        return Stream.concat(
                getBodyQuads(strippedState, shape, radius, random),
                getStrippedNeighborQuads(random, strippedNeighbors));
    }

    public static Stream<BakedQuad> getBodyQuads(BlockState strippedState, ChoppedLogShape shape, int radius,
            RandomSource random) {
        final Direction[] allDirections = { Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, null };
        AABB box = shape.getBoundingBox(radius);
        Vector3 mins = new Vector3(box.minX, box.minY, box.minZ);
        Vector3 maxes = new Vector3(box.maxX, box.maxY, box.maxZ);

        return Arrays.stream(allDirections)
                .flatMap(side -> getBlockQuads(strippedState, side, random).stream()
                        .map(quad -> ModelUtil.trimQuad(quad, mins, maxes)))
                .filter(Objects::nonNull);
    }

    /**
     * Full faces of adjacent solid logs, placed on this block's interior sides.
     * Slightly inset so they win against the neighbor's bark (occlusion is
     * state-only in 26.3 and cannot express dynamic solid sides).
     */
    public static Stream<BakedQuad> getStrippedNeighborQuads(RandomSource random,
            Map<Direction, BlockState> strippedNeighbors) {
        // Match FaceShape's 0.01 block inset (model units are 0–16).
        final double inset = 0.16;

        return strippedNeighbors.entrySet().stream().flatMap(entry -> {
            Direction side = entry.getKey();
            BlockState strippedNeighbor = entry.getValue();
            Vec3i unit = side.getUnitVec3i();
            Vector3 transform = new Vector3(
                    unit.getX() * 16.0 - unit.getX() * inset,
                    unit.getY() * 16.0 - unit.getY() * inset,
                    unit.getZ() * 16.0 - unit.getZ() * inset);

            return getBlockQuads(strippedNeighbor, side.getOpposite(), random).stream()
                    .map(quad -> ModelUtil.translateQuad(quad, transform));
        }).filter(Objects::nonNull);
    }
}
