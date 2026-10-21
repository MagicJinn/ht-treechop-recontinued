package ht.treechop.client.model;

import ht.treechop.common.registry.FabricModBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class ChoppedLogModelLoadingPlugin implements ModelLoadingPlugin {
    private final Function<BlockStateModel, BlockStateModel> modelFactory;

    public ChoppedLogModelLoadingPlugin(Function<BlockStateModel, BlockStateModel> modelFactory) {
        this.modelFactory = modelFactory;
    }

    @Override
    public void initialize(Context pluginContext) {
        pluginContext.modifyBlockModelAfterBake().register((model, context) -> {
            if (context.state().getBlock() == FabricModBlocks.CHOPPED_LOG)
                return modelFactory.apply(model);

            return model;
        });
    }
}
