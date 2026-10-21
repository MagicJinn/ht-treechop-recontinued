package ht.treechop.client.gui.widget;

import ht.treechop.client.gui.util.GUIUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.function.Supplier;

public class StickyWidget extends AbstractWidget {

    public static final Identifier WIDGETS_LOCATION = Identifier.withDefaultNamespace("textures/gui/widgets.png");
    private final Supplier<State> stateSupplier;
    private final Runnable onPress;

    public StickyWidget(int x, int y, int width, int height, Component name, Runnable onPress, Supplier<State> stateSupplier) {
        super(x, y, Math.max(width, GUIUtil.getMinimumButtonWidth(name)), Math.max(height, GUIUtil.BUTTON_HEIGHT), name);
        this.onPress = onPress;
        this.stateSupplier = stateSupplier;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onPress.run();
    }

    // Taken from Forge's AbstractWidget
    public int getFGColor() {
        return this.active ? 16777215 : 10526880; // White : Light Grey
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTicks) {
        this.active = stateSupplier.get() == State.Up;
        this.height = Math.min(this.height, GUIUtil.BUTTON_HEIGHT);

        Minecraft minecraft = Minecraft.getInstance();

        if (stateSupplier.get() != State.Locked) {
            int i = this.getYImage(this.isHoveredOrFocused());
            int color = ARGB.white(this.alpha);
            gui.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_LOCATION, getX(), getY(), 0, 46 + i * 20, this.width / 2, this.height, 256, 256, color);
            gui.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_LOCATION, getX() + this.width / 2, getY(), 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height, 256, 256, color);
        }

        gui.centeredText(minecraft.font, this.getMessage(), getX() + this.width / 2, getY() + (this.height - 8) / 2, ARGB.color(this.alpha, getFGColor()));
    }

    private int getYImage(boolean hoveredOrFocused) {
        return active ? (hoveredOrFocused ? 2 : 1) : 0;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // TODO
    }

    public enum State {
        Up,
        Down,
        Locked;

        public static State of(boolean enabled, boolean canBeEnabled) {
            if (canBeEnabled) {
                return enabled ? Down : Up;
            } else {
                return State.Locked;
            }
        }
    }

}
