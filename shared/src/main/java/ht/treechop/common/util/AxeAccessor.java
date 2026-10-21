package ht.treechop.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AxeAccessor {
    private static final RandomSource RANDOM = RandomSource.create(0L);
    private static final BlockPos QUERY_POS = BlockPos.ZERO;

    private static Map<Block, BlockState> stripMap;
    private static Set<Block> strippedBlocks;

    private AxeAccessor() {
    }

    public static boolean isStrippable(Block block) {
        Map<Block, BlockState> map = stripMapOrNull();
        return map != null && map.containsKey(block);
    }

    public static boolean isStripped(Block block) {
        Set<Block> stripped = strippedBlocksOrNull();
        return stripped != null && stripped.contains(block);
    }

    public static BlockState getStripped(BlockState blockState) {
        Map<Block, BlockState> map = stripMapOrNull();
        if (map == null) {
            return null;
        }
        BlockState stripped = map.get(blockState.getBlock());
        return stripped == null ? null : BlockUtil.copyStateProperties(stripped, blockState);
    }

    /**
     * Builds strip mappings from {@link BlockTransformers#AXE} when a level with
     * datapack registries is available.
     */
    public static void warmFrom(Level level) {
        if (stripMap != null || level == null) {
            return;
        }

        Registry<BlockTransformer> registry = level.registryAccess()
                .lookup(Registries.BLOCK_TRANSFORMER)
                .orElse(null);
        if (registry == null)
            return;

        BlockTransformer axe = registry.getOptional(BlockTransformers.AXE).orElse(null);
        if (axe == null)
            return;

        Map<Block, BlockState> map = new HashMap<>();
        Set<Block> stripped = new HashSet<>();

        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            BlockState result = queryAxeTransform(axe, state);
            if (result != null && result.getBlock() != Blocks.AIR) {
                map.put(block, result);
                stripped.add(result.getBlock());
            }
        }

        stripMap = Collections.unmodifiableMap(map);
        strippedBlocks = Collections.unmodifiableSet(stripped);
    }

    private static Map<Block, BlockState> stripMapOrNull() {
        return stripMap;
    }

    private static Set<Block> strippedBlocksOrNull() {
        return strippedBlocks;
    }

    private static BlockState queryAxeTransform(BlockTransformer axe, BlockState state) {
        LevelAccessor fakeLevel = fakeLevel(state);
        for (BlockTransformer.BlockTransformData transform : axe.transforms()) {
            BlockState result = transform.blockStateProvider().value()
                    .getOptionalState(fakeLevel, RANDOM, QUERY_POS);
            if (result != null)
                return result;
        }
        return null;
    }

    /**
     * Creates a fake level accessor that returns the given block state when
     * {@code getBlockState} is called.
     */
    private static LevelAccessor fakeLevel(BlockState state) {
        return (LevelAccessor) Proxy.newProxyInstance(
                LevelAccessor.class.getClassLoader(),
                new Class<?>[] { LevelAccessor.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getBlockState".equals(name) && args != null && args.length == 1) 
                        return state;
                    
                    if ("equals".equals(name) && args != null && args.length == 1) 
                        return proxy == args[0];
                    
                    if ("hashCode".equals(name)) 
                        return System.identityHashCode(proxy);
                    
                    if ("toString".equals(name)) 
                        return "AxeAccessorFakeLevel(" + state + ")";
                    
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) 
                        return false;
                    
                    if (returnType == int.class) 
                        return 0;
                    
                    if (returnType == long.class) 
                        return 0L;
                    
                    if (returnType == float.class) 
                        return 0f;
                    
                    if (returnType == double.class) 
                        return 0d;
                    
                    return null;
                });
    }
}
