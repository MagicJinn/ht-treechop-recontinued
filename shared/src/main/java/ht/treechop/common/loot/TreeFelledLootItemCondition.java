package ht.treechop.common.loot;

import com.mojang.serialization.MapCodec;
import ht.treechop.TreeChop;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Collections;
import java.util.Set;

public record TreeFelledLootItemCondition() implements LootItemCondition {
    public static final Identifier ID = TreeChop.resource("tree_felled");
    static final TreeFelledLootItemCondition INSTANCE = new TreeFelledLootItemCondition();
    public static final MapCodec<TreeFelledLootItemCondition> CODEC = MapCodec.unit(INSTANCE);
    public static final MapCodec<? extends LootItemCondition> TYPE = CODEC;

    @Override
    public MapCodec<? extends LootItemCondition> codec() {
        return CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Collections.emptySet();
    }

    @Override
    public boolean test(LootContext context) {
        Boolean destroying = context.getOptional(TreeChopLootContextParams.DESTROY_BLOCK);
        return destroying == null || destroying;
    }
}
