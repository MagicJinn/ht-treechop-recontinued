package ht.treechop.compat.polymer;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import ht.treechop.common.block.ChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.properties.ChoppedLogShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * Scaled stripped-log BlockDisplay over the Polymer empty stand-in.
 * <p>
 * Scale/translation follow {@link ChoppedLogShape#getBoundingBox(int)} so the
 * display shrinks toward open sides (same silhouette as collision / vanilla
 * models). UVs are stretched on non-uniform scales. The display is not attached
 * until {@link #refresh()} / first successful {@code apply}, so place-time
 * watchers never get a hardcoded oak before {@code setOriginalState}.
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
     * Apply BE wood/shape/radius now and flush dirty display data to current
     * watchers.
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
            apply(entity.getOriginalState(), entity.getShape(), entity.getRadius(), attachment.getWorld(),
                    attachment.getBlockPos());
    }

    private void apply(BlockState original, ChoppedLogShape shape, int radius, Level level, BlockPos pos) {
        radius = Mth.clamp(radius, 1, 8);
        BlockState shown = ChopUtil.getStrippedState(level, pos, original, original);
        int key = System.identityHashCode(shown.getBlock()) * 31 + radius + Block.getId(shown)
                + 31 * shape.ordinal();
        if (key == lastKey)
            return;

        lastKey = key;

        // Shape AABB is in 0–16 model units. Holder is at block center, so
        // translation is min-corner relative to center and scale is box size.
        AABB box = shape.getBoundingBox(radius);
        float scaleX = (float) (box.getXsize() / 16.0);
        float scaleY = (float) (box.getYsize() / 16.0);
        float scaleZ = (float) (box.getZsize() / 16.0);
        this.display.setBlockState(shown);
        this.display.setScale(new Vector3f(scaleX, scaleY, scaleZ));
        this.display.setTranslation(new Vector3f(
                (float) (box.minX / 16.0) - 0.5f,
                (float) (box.minY / 16.0) - 0.5f,
                (float) (box.minZ / 16.0) - 0.5f));

        // Defer attach until wood type is known so setBlock watchers never spawn
        // stripped oak.
        if (!displayAdded) {
            addElement(this.display);
            displayAdded = true;
        }
    }
}
