package ht.treechop.mixin;

import ht.treechop.client.gui.screen.ChopIndicator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Hud.class, remap = false)
public class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"), remap = false)
    private void injectChopIndicator(GuiGraphicsExtractor gui, DeltaTracker delta, CallbackInfo info) {
        ChopIndicator.render(gui, gui.guiWidth(), gui.guiHeight());
    }
}
