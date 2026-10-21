package ht.treechop.compat;

import ht.treechop.TreeChop;
import ht.treechop.common.block.ChoppedLogBlock;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.ConfigIcon;

@WailaPlugin
public class Jade implements IWailaPlugin {

    public static final Identifier SHOW_TREE_BLOCKS = TreeChop.resource("show_tree_block_counts");
    public static final Identifier SHOW_NUM_CHOPS_REMAINING = TreeChop.resource("show_num_chops_remaining");
    private static final ConfigIcon NO_ICON = ConfigIcon.none();

    @Override
    public void registerClient(IWailaClientRegistration registrar) {
        registrar.registerBlockComponent(JadeProvider.INSTANCE, Block.class);
        registrar.registerBlockIcon(JadeProvider.INSTANCE, ChoppedLogBlock.class);
        registrar.addConfig(SHOW_TREE_BLOCKS, NO_ICON, true);
        registrar.addConfig(SHOW_NUM_CHOPS_REMAINING, NO_ICON, true);
    }
}
