package ht.treechop.compat;

import ht.treechop.TreeChop;
import ht.treechop.common.block.ChoppedLogBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;

import java.util.LinkedList;
import java.util.List;

/** Client-only Jade tooltip provider. Kept off the plugin entrypoint for dedicated servers. */
public class JadeProvider implements IBlockComponentProvider {

    public static final JadeProvider INSTANCE = new JadeProvider();

    private static final Identifier UID = TreeChop.resource("plugin");

    private JadeProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        changeBlockName(tooltip, accessor);

        Level level = accessor.getLevel();
        BlockPos pos = accessor.getPosition();
        boolean showNumBlocks = config.get(Jade.SHOW_TREE_BLOCKS);
        boolean showChopsRemaining = config.get(Jade.SHOW_NUM_CHOPS_REMAINING);

        if (WailaUtil.playerWantsTreeInfo(level, pos, showNumBlocks, showChopsRemaining)) {
            // Insert under the name. Polymer may have already rebuilt the tooltip.
            final int[] insertAt = { Math.min(1, tooltip.size()) };
            List<Element> tiles = new LinkedList<>();
            WailaUtil.addTreeInfo(
                    level,
                    pos,
                    showNumBlocks,
                    showChopsRemaining,
                    chops -> tooltip.add(insertAt[0]++, chops),
                    stack -> tiles.add(JadeUI.item(stack, 1f, Integer.toString(stack.getCount())).offset(0, -2))
            );
            if (!tiles.isEmpty())
                tooltip.add(insertAt[0], tiles);
        }
    }

    private static void changeBlockName(ITooltip tooltip, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof ChoppedLogBlock.MyEntity choppedEntity) {
            Component newName = IThemeHelper.get().title(WailaUtil.getPrefixedBlockName(choppedEntity));
            if (!tooltip.replace(JadeIds.CORE_OBJECT_NAME, newName))
                tooltip.add(0, JadeUI.text(newName).tag(JadeIds.CORE_OBJECT_NAME));
        }
    }

    @Override
    public Element getIcon(BlockAccessor accessor, IPluginConfig config, Element currentIcon) {
        if (accessor.getBlock() instanceof ChoppedLogBlock) {
            ItemStack stack = accessor.getPickedResult();
            if (!stack.isEmpty())
                return JadeUI.item(stack);
        }
        return currentIcon;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    /**
     * Must run after Polymer's Jade compat (priority 99999), which clears the
     * tooltip for Polymer blocks and would wipe chops/yield.
     */
    @Override
    public int getDefaultPriority() {
        return 100000;
    }
}
