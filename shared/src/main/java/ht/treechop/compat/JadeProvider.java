package ht.treechop.compat;

import ht.treechop.TreeChop;
import ht.treechop.common.block.ChoppedLogBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
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
    private static final Identifier OBJECT_NAME = Identifier.fromNamespaceAndPath("jade", "object_name");

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
            List<Element> tiles = new LinkedList<>();
            WailaUtil.addTreeInfo(
                    level,
                    pos,
                    showNumBlocks,
                    showChopsRemaining,
                    tooltip::add,
                    stack -> tiles.add(JadeUI.item(stack, 1f, Integer.toString(stack.getCount())).offset(0, -2))
            );
            tooltip.add(tiles);
        }
    }

    private static void changeBlockName(ITooltip tooltip, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof ChoppedLogBlock.MyEntity choppedEntity) {
            // There's no API function to change the message, so let's replace it
            tooltip.clear();
            Component newName = WailaUtil.getPrefixedBlockName(choppedEntity);
            tooltip.add(JadeUI.text(IThemeHelper.get().title(newName)).tag(OBJECT_NAME));
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
