package ht.treechop.common.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ht.treechop.TreeChop;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.IntRangePredicate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Set;

public record CountBlockChopsLootItemCondition(IntRangePredicate range) implements LootItemCondition {
    public static final Identifier ID = TreeChop.resource("count_block_chops");
    public static final MapCodec<CountBlockChopsLootItemCondition> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                            IntRangePredicate.CODEC.fieldOf("range").forGetter(CountBlockChopsLootItemCondition::range)
                    )
                    .apply(instance, CountBlockChopsLootItemCondition::new)
    );
    public static final MapCodec<? extends LootItemCondition> TYPE = CODEC;

    @Override
    public MapCodec<? extends LootItemCondition> codec() {
        return CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(TreeChopLootContextParams.BLOCK_CHOP_COUNT);
    }

    @Override
    public boolean test(LootContext context) {
        Integer count = context.getOptional(TreeChopLootContextParams.BLOCK_CHOP_COUNT);
        return count != null && this.range.test(context, count);
    }
}
