package ht.treechop.compat.polymer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Polymer hooks with no Polymer API imports (safe when Polymer is absent).
 */
public final class PolymerFx {
    @FunctionalInterface
    public interface Feedback {
        void play(ServerPlayer player, BlockPos pos, BlockState fxState);
    }

    @FunctionalInterface
    public interface DisplayRefresh {
        void refresh(Level level, BlockPos pos);
    }

    private static Feedback feedback = (player, pos, fxState) -> {
    };

    private static DisplayRefresh displayRefresh = (level, pos) -> {
    };

    private PolymerFx() {
    }

    public static void setFeedback(Feedback handler) {
        feedback = handler != null ? handler : ((player, pos, fxState) -> {
        });
    }

    public static void setDisplayRefresh(DisplayRefresh handler) {
        displayRefresh = handler != null ? handler : ((level, pos) -> {
        });
    }

    public static void play(ServerPlayer player, BlockPos pos, BlockState fxState) {
        feedback.play(player, pos, fxState);
    }

    public static void refreshDisplay(Level level, BlockPos pos) {
        displayRefresh.refresh(level, pos);
    }
}
