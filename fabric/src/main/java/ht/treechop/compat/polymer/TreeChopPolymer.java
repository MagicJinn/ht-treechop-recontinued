package ht.treechop.compat.polymer;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.soundpatcher.api.SoundPatcher;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import ht.treechop.TreeChop;
import ht.treechop.common.block.PolymerFabricChoppedLogBlock;
import ht.treechop.common.chop.ChopUtil;
import ht.treechop.common.loot.CountBlockChopsLootItemCondition;
import ht.treechop.common.loot.TreeFelledLootItemCondition;
import ht.treechop.common.registry.FabricModBlocks;
import ht.treechop.server.FabricServer;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Empty (invisible) leaves (to prevent culling and BlockDisplay darkening).
 * BlockDisplay to display the chopped log. Break speed uses Polymer server
 * mining. Stand-in leaf sounds are muted via SoundPatcher. Mining uses wood.
 */
public final class TreeChopPolymer {
    private static @Nullable BlockState emptyLeaves;
    private static @Nullable BlockState emptyWaterlogged;

    public static void init() {
        PolymerResourcePackUtils.markAsRequired();
        PolymerResourcePackUtils.addModAssets(TreeChop.MOD_ID);

        emptyLeaves = PolymerBlockResourceUtils.requestEmpty(BlockModelType.LEAVES);
        try {
            emptyWaterlogged = PolymerBlockResourceUtils.requestEmpty(BlockModelType.LEAVES_WATERLOGGED);
        } catch (RuntimeException e) {
            emptyWaterlogged = emptyLeaves;
            TreeChop.LOGGER.warn("No empty waterlogged Polymer state!");
        }

        // Empty LEAVES pool starts at azalea (SoundType.AZALEA_LEAVES). Mute stand-in
        // hit/break so client prediction is silent. Send wood hits instead.
        muteStandInSounds(emptyLeaves);
        if (emptyWaterlogged != null && emptyWaterlogged != emptyLeaves)
            muteStandInSounds(emptyWaterlogged);

        PolymerBlockUtils.registerBlockEntity(FabricModBlocks.CHOPPED_LOG_ENTITY, new PolymerSyncedObject<>() {
            @Override
            public BlockEntityType<?> getPolymerReplacement(BlockEntityType<?> type, PacketContext context) {
                // null => Polymer omits this BE from chunk packets / BE updates.
                // TreeChop clients need the real BE on chunk enter (chops/radius/wood);
                // ServerUpdateChopsPacket only fires on setChanged, so without this they
                // render the stripped-oak radius-8 fallback until someone chops again.
                return canSyncRawToClient(context) ? type : null;
            }

            @Override
            public boolean canSyncRawToClient(PacketContext context) {
                return TreeChopPolymerClients.hasMod(
                        eu.pb4.polymer.common.api.PolymerCommonUtils.getPlayer(context));
            }
        });

        PolymerBlockUtils.BREAKING_PROGRESS_UPDATE.register((player, pos, state, progress) -> {
            if (progress < 0 || !(state.getBlock() instanceof PolymerFabricChoppedLogBlock))
                return;

            if (TreeChopPolymerClients.hasMod(player))
                return;

            BlockState fx = ChopUtil.phaseChangeFxState(player.level(), pos, state);
            spawnMiningChips(player, pos, fx);
            // Vanilla hits every 4 ticks.
            if (progress % 4 == 0)
                playWoodHit(player, pos, fx);
        });

        RegistrySyncUtils.setServerEntry(BuiltInRegistries.SOUND_EVENT, TreeChop.CHOP_WOOD_EVENT.get());
        RegistrySyncUtils.setServerEntry(BuiltInRegistries.LOOT_CONDITION_TYPE, CountBlockChopsLootItemCondition.ID);
        RegistrySyncUtils.setServerEntry(BuiltInRegistries.LOOT_CONDITION_TYPE, TreeFelledLootItemCondition.ID);

        FabricServer.setPacketFilter(TreeChopPolymerClients::shouldSendPacket);
        PolymerFx.setFeedback(TreeChopPolymer::playPhaseChangeFeedback);
        PolymerFx.setDisplayRefresh(TreeChopPolymer::refreshChoppedLogDisplay);

        TreeChop.LOGGER.info("Polymer compatibility enabled (empty leaves + BlockDisplay).");
    }

    private static void refreshChoppedLogDisplay(Level level, BlockPos pos) {
        BlockBoundAttachment attachment = BlockBoundAttachment.get(level, pos);
        if (attachment != null && attachment.holder() instanceof ChoppedLogDisplay display)
            display.refresh();
    }

    /**
     * Silences client-predicted hits for the Polymer empty stand-in's SoundType.
     */
    private static void muteStandInSounds(@Nullable BlockState standIn) {
        if (standIn == null)
            return;

        SoundType soundType = standIn.getSoundType();
        SoundPatcher.convertIntoServerSound(soundType.getHitSound());
        SoundPatcher.convertIntoServerSound(soundType.getBreakSound());
    }

    public static BlockState emptyStandIn(boolean waterlogged) {
        if (waterlogged && emptyWaterlogged != null)
            return emptyWaterlogged;

        if (emptyLeaves != null)
            return emptyLeaves;

        return Blocks.OAK_LEAVES.defaultBlockState();
    }

    public static void playPhaseChangeFeedback(ServerPlayer player, BlockPos pos, BlockState fxState) {
        if (TreeChopPolymerClients.hasMod(player))
            return;

        if (!(player.level() instanceof ServerLevel level))
            return;

        Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(TreeChop.CHOP_WOOD_EVENT.get());
        player.connection.send(new ClientboundSoundPacket(
                sound,
                SoundSource.BLOCKS,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                0.3f,
                1f,
                level.getRandom().nextLong()));
    }

    private static void playWoodHit(ServerPlayer player, BlockPos pos, BlockState fxState) {
        if (!(player.level() instanceof ServerLevel level))
            return;

        SoundType soundType = fxState.getSoundType();
        Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundType.getHitSound());
        player.connection.send(new ClientboundSoundPacket(
                sound,
                SoundSource.BLOCKS,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                (soundType.getVolume() + 1f) / 16f,
                soundType.getPitch() * 0.5f,
                level.getRandom().nextLong()));
    }

    private static void spawnMiningChips(ServerPlayer miner, BlockPos pos, BlockState fxState) {
        sendParticlesNear(miner.level(), new BlockParticleOption(ParticleTypes.BLOCK, fxState),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                0.4f, 0.4f, 0.4f, 0.15f, 4);
    }

    private static void sendParticlesNear(
            ServerLevel level, BlockParticleOption particle,
            double x, double y, double z,
            float xDist, float yDist, float zDist, float speed, int count) {
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                particle, false, false, x, y, z, xDist, yDist, zDist, speed, count);
        for (ServerPlayer player : level.players()) {
            if (TreeChopPolymerClients.hasMod(player) || player.distanceToSqr(x, y, z) > 64 * 64)
                continue;

            player.connection.send(packet);
        }
    }
}
