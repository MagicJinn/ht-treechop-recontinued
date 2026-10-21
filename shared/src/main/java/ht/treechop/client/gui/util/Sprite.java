package ht.treechop.client.gui.util;

import ht.treechop.TreeChop;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public enum Sprite {
    CHOP_INDICATOR(0, 0, 20, 20),
    NO_FELL_INDICATOR(20, 0, 20, 20),
    INDICATOR_ERROR(60, 0, 12, 12),
    INDICATOR_WARNING(72, 0, 12, 12),
    TOGGLE_BUTTON_OFF(0, 20, 32, 20),
    TOGGLE_BUTTON_ON(32, 20, 32, 20),
    HIGHLIGHTED_TOGGLE_BUTTON_OFF(0, 40, 32, 20),
    HIGHLIGHTED_TOGGLE_BUTTON_ON(32, 40, 32, 20),
    LOCKED_TOGGLE_BUTTON_OFF(0, 60, 32, 20),
    LOCKED_TOGGLE_BUTTON_ON(32, 60, 32, 20),
    PAGE_ONE(0, 80, 32, 20),
    HIGHLIGHTED_PAGE_ONE(32, 80, 32, 20),
    PAGE_TWO(0, 100, 32, 20),
    HIGHLIGHTED_PAGE_TWO(32, 100, 32, 20),
    ;

    public static final Identifier TEXTURE_PATH = TreeChop.resource("textures/gui/widgets.png");
    public static final int TEXTURE_WIDTH = 64;
    public static final int TEXTURE_HEIGHT = 120;

    private final int u;
    private final int v;
    public final int width;
    public final int height;

    Sprite(int u, int v, int width, int height) {
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
    }

    public void blit(GuiGraphicsExtractor gui, int x, int y, float alpha) {
        blit(gui, x, y, width, height, alpha, false);
    }

    public void blitMirror(GuiGraphicsExtractor gui, int x, int y, float alpha, float scale, boolean mirror) {
        blit(gui, x, y, (int) (width * scale), (int) (height * scale), alpha, mirror);
    }

    public void blit(GuiGraphicsExtractor gui, int x, int y, int drawWidth, int drawHeight, float alpha,
            boolean mirror) {
        float u = mirror ? this.u + this.width : this.u;
        int regionWidth = mirror ? -this.width : this.width;
        int color = ARGB.white(alpha);
        gui.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE_PATH,
                x,
                y,
                u,
                v,
                drawWidth,
                drawHeight,
                regionWidth,
                this.height,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                color);
    }
}
