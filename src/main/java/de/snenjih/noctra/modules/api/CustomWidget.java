package de.snenjih.noctra.modules.api;

import net.minecraft.client.gui.DrawContext;

/**
 * A custom UI widget that a module can provide for its settings screen.
 * Rendered after all standard settings.
 */
public interface CustomWidget {
    int  getHeight();
    void render(DrawContext ctx, int x, int y, int width, int mouseX, int mouseY);
    default boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    default boolean mouseDragged(double mx, double my, int button, double dx, double dy) { return false; }
    default boolean mouseReleased(double mx, double my, int button) { return false; }
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    default void    charTyped(char chr, int modifiers) {}
}
