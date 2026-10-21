package ht.treechop.client.model;

import ht.treechop.TreeChop;
import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.properties.ChoppedLogShape;
import ht.treechop.common.NeoForgeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.common.extensions.IBlockGetterExtension;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class NeoForgeChoppedLogBakedModel implements DynamicBlockStateModel {
    public static ModelProperty<Map<Direction, BlockState>> STRIPPED_NEIGHBORS = new ModelProperty<>();
    public static ModelProperty<BlockState> STRIPPED_BLOCK_STATE = new ModelProperty<>();
    public static ModelProperty<Integer> RADIUS = new ModelProperty<>();
    public static ModelProperty<ChoppedLogShape> CHOPPED_LOG_SHAPE = new ModelProperty<>();

    private final Material.Baked fallbackParticle;

    public NeoForgeChoppedLogBakedModel(Material.Baked fallbackParticle) {
        this.fallbackParticle = fallbackParticle;
    }

    public static void overrideBlockStateModels(ModelEvent.ModifyBakingResult event) {
        var blockStateModels = event.getBakingResult().blockStateModels();
        var oakModel = blockStateModels.get(Blocks.OAK_LOG.defaultBlockState());
        Material.Baked particle = oakModel != null
                ? oakModel.particleMaterial()
                : ChoppedLogBakedModel.getDefaultSpriteAsMaterial();
        ChoppedLogBakedModel.setDefaultParticleMaterial(particle);

        for (BlockState blockState : NeoForgeRegistry.Blocks.CHOPPED_LOG.get().getStateDefinition().getPossibleStates()) {
            var existingModel = blockStateModels.get(blockState);
            if (existingModel == null) {
                TreeChop.LOGGER.warn("Did not find the expected vanilla baked model(s) for treechop:chopped_log in registry");
            } else if (existingModel instanceof NeoForgeChoppedLogBakedModel) {
                TreeChop.LOGGER.warn("Tried to replace ChoppedLogBakedModel twice");
            } else {
                blockStateModels.put(blockState, new NeoForgeChoppedLogBakedModel(particle));
            }
        }
    }

    public static ModelData getModelData(ChoppedLogBlock.MyEntity entity) {
        BlockState strippedState = ChopUtil.getStrippedState(entity.getLevel(), entity.getBlockPos(), entity.getOriginalState());

        ModelData.Builder builder = ModelData.builder();
        builder.with(STRIPPED_NEIGHBORS, ChoppedLogBakedModel.getStrippedNeighbors(entity.getLevel(), entity.getBlockPos(), entity));
        builder.with(STRIPPED_BLOCK_STATE, strippedState);
        builder.with(RADIUS, entity.getRadius());
        builder.with(CHOPPED_LOG_SHAPE, entity.getShape());

        return builder.build();
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        // Destroy overlay passes EMPTY (no BEs) plus a real pos. Prefer client level +
        // BE so crack geometry matches the chopped hitbox. Omit stripped-neighbor faces
        // on that path so the crack stays on this block, not adjacent logs.
        boolean destroyOverlay = !(level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity);
        BlockAndTintGetter geometryLevel = resolveGeometryLevel(level, pos);
        ChoppedLogBlock.MyEntity entity = geometryLevel.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity e
                ? e
                : null;
        ModelData modelData = modelData(geometryLevel, pos);

        BlockState strippedState;
        ChoppedLogShape shape;
        int radius;
        Map<Direction, BlockState> strippedNeighbors;

        if (entity != null) {
            strippedState = ChopUtil.getStrippedState(geometryLevel, pos, entity.getOriginalState());
            shape = entity.getShape();
            radius = entity.getRadius();
            strippedNeighbors = destroyOverlay
                    ? Collections.emptyMap()
                    : ChoppedLogBakedModel.getStrippedNeighbors(geometryLevel, pos, entity);
        } else {
            strippedState = or(modelData.get(STRIPPED_BLOCK_STATE), Blocks.STRIPPED_OAK_LOG.defaultBlockState());
            shape = or(modelData.get(CHOPPED_LOG_SHAPE), ChoppedLogShape.PILLAR_Y);
            radius = or(modelData.get(RADIUS), 8);
            strippedNeighbors = destroyOverlay
                    ? Collections.emptyMap()
                    : or(modelData.get(STRIPPED_NEIGHBORS), Collections.emptyMap());
        }

        List<BakedQuad> quads = ChoppedLogBakedModel.getQuads(
                strippedState,
                shape,
                radius,
                random,
                strippedNeighbors
        ).collect(Collectors.toList());

        parts.add(ChoppedLogBakedModel.createModelPart(quads, particleMaterial(geometryLevel, pos, state)));
    }

    private static BlockAndTintGetter resolveGeometryLevel(BlockAndTintGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity) {
            return level;
        }
        ModelData data = modelData(level, pos);
        if (data.has(RADIUS) || data.has(STRIPPED_BLOCK_STATE)) {
            return level;
        }
        var clientLevel = Minecraft.getInstance().level;
        return clientLevel != null ? clientLevel : level;
    }

    @Override
    public Material.Baked particleMaterial() {
        return fallbackParticle;
    }

    // NeoForge BlockStateModelExtension (interface-injected on BlockStateModel)
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        BlockAndTintGetter geometryLevel = resolveGeometryLevel(level, pos);
        if (geometryLevel.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity entity) {
            BlockState strippedState = ChopUtil.getStrippedState(geometryLevel, pos, entity.getOriginalState());
            return ChoppedLogBakedModel.getBlockModel(strippedState).particleMaterial();
        }
        ModelData data = modelData(geometryLevel, pos);
        BlockState strippedState = or(data.get(STRIPPED_BLOCK_STATE), Blocks.STRIPPED_OAK_LOG.defaultBlockState());
        return ChoppedLogBakedModel.getBlockModel(strippedState).particleMaterial();
    }

    @Override
    public int materialFlags() {
        return 0;
    }

    // NeoForge BlockStateModelExtension (interface-injected on BlockStateModel)
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        BlockAndTintGetter geometryLevel = resolveGeometryLevel(level, pos);
        if (geometryLevel.getBlockEntity(pos) instanceof ChoppedLogBlock.MyEntity entity) {
            return List.of(
                    entity.getRadius(),
                    entity.getShape(),
                    entity.getOriginalState(),
                    ChoppedLogBakedModel.getStrippedNeighbors(geometryLevel, pos, entity)
            );
        }
        ModelData data = modelData(geometryLevel, pos);
        return List.of(
                Objects.requireNonNullElse(data.get(RADIUS), 8),
                Objects.requireNonNullElse(data.get(CHOPPED_LOG_SHAPE), ChoppedLogShape.PILLAR_Y),
                Objects.requireNonNullElse(data.get(STRIPPED_BLOCK_STATE), Blocks.STRIPPED_OAK_LOG.defaultBlockState()),
                Objects.requireNonNullElse(data.get(STRIPPED_NEIGHBORS), Collections.emptyMap())
        );
    }

    private static ModelData modelData(BlockAndTintGetter level, BlockPos pos) {
        return ((IBlockGetterExtension) level).getModelData(pos);
    }

    private <T> T or(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
