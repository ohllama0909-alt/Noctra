package de.snenjih.noctra.modules.api;

import net.minecraft.client.gui.DrawContext;

/**
 * A module that renders a movable, resizable HUD overlay implements this interface.
 * Position and size are managed by HudRegistry (persisted in config).
 */
public interface HudElement {

    /** Unique ID for config persistence. Should match the module ID. */
    String getHudId();

    /** Display name shown in HUD edit mode. */
    String getHudName();

    /** Default pixel width when first registered. */
    int getDefaultWidth();

    /** Default pixel height when first registered. */
    int getDefaultHeight();

    /**
     * Render the HUD element at the given position and size.
     * Only called when the owning module is enabled.
     *
     * @param ctx      DrawContext
     * @param tickDelta partial tick
     * @param x        screen X (top-left)
     * @param y        screen Y (top-left)
     * @param w        width to render within
     * @param h        height to render within
     */
    void renderHud(DrawContext ctx, float tickDelta, int x, int y, int w, int h);
}
