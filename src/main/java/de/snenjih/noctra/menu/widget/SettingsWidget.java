package de.snenjih.noctra.menu.widget;

import net.minecraft.client.gui.DrawContext;

/**
 * Common interface for all setting widgets rendered in ModuleSettingsScreen.
 * Each widget represents one setting row (or a section header).
 */
public interface SettingsWidget {

    /** Height of this widget in pixels. */
    int getHeight();

    /**
     * Render the widget.
     *
     * @param ctx    DrawContext
     * @param x      left edge
     * @param y      top edge
     * @param width  available width
     * @param mouseX screen mouse X
     * @param mouseY screen mouse Y
     */
    void render(DrawContext ctx, int x, int y, int width, int mouseX, int mouseY);

    default boolean mouseClicked(double mx, double my, int button)                          { return false; }
    default boolean mouseDragged(double mx, double my, int button, double dx, double dy)    { return false; }
    default boolean mouseReleased(double mx, double my, int button)                         { return false; }
    default boolean keyPressed(int keyCode, int scanCode, int modifiers)                    { return false; }
    default void    charTyped(char chr, int mods)                                           {}
}
