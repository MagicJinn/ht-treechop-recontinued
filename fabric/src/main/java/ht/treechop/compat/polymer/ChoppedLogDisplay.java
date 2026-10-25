package ht.treechop.compat.polymer;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Scaled stripped-log BlockDisplay over the Polymer empty stand-in.
 * <p>
 * The display is not attached until {@link #refresh()} / first successful
 * {@code apply}, so place-time watchers never get a hardcoded oak before
 * {@code setOriginalState}.
 */
public class ChoppedLogDisplay extends ElementHolder {
    private final BlockDisplayElement display = new BlockDisplayElement();
    private int lastKey = Integer.MIN_VALUE;
    private boolean displayAdded;

    public ChoppedLogDisplay() {
        this.display.setInterpolationDuration(0);
        this.display.setTeleportDuration(0);
        // Empty picking box so the display never steals mining clicks from the
        // stand-in.
        this.display.setDisplaySize(0f, 0f);
        this.display.setShadowRadius(0f);
        this.display.setShadowStrength(0f);
    }

    @Override
    public boolean startWatching(ServerGamePacketListenerImpl player) {
        if (TreeChopPolymerClients.hasMod(player.getPlayer()))
            return false;

        return super.startWatching(player);
    }

    @Override
    protected void onTick() {
        refreshFromWorld();
    }

    @Override
    public void notifyUpdate(HolderAttachment.UpdateType updateType) {
        if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE)
            refreshFromWorld();
    }

    /**
     * Apply BE wood/radius now and flush dirty display data to current watchers.
     */
    public void refresh() {
        refreshFromWorld();
        if (getAttachment() == null)
            return;

        for (var element : getElements()) {
            element.tick();
        }
    }

    private void refreshFromWorld() {
        if (!(getAttachment() instanceof BlockAwareAttachment attachment))
            return;

        if (attachment.getWorld().getBlockEntity(attachment.getBlockPos()) instanceof ChoppedLogBlock.MyEntity entity)
            apply(entity.getOriginalState(), entity.getRadius(), attachment.getWorld(), attachment.getBlockPos());
    }

    private void apply(BlockState original, int radius, Level level, BlockPos pos) {
        radius = Mth.clamp(radius, 1, 8);
        BlockState shown = ChopUtil.getStrippedState(level, pos, original, original);
        int key = System.identityHashCode(shown.getBlock()) * 31 + radius + Block.getId(shown);
        if (key == lastKey)
            return;

        lastKey = key;

        float scaleXZ = radius / 8f;
        float inset = (1f - scaleXZ) / 2f;
        this.display.setBlockState(shown);
        this.display.setScale(new Vector3f(scaleXZ, 1f, scaleXZ));
        this.display.setTranslation(new Vector3f(-0.5f + inset, -0.5f, -0.5f + inset));

        // Defer attach until wood type is known so setBlock watchers never spawn
        // stripped oak.
        if (!displayAdded) {
            addElement(this.display);
            displayAdded = true;
        }
    }
}
