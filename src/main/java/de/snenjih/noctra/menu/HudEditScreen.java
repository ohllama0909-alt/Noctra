package de.snenjih.noctra.menu;

import de.snenjih.noctra.config.ModConfig;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.HudElement;
import de.snenjih.noctra.modules.api.HudRegistry;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {

    private static final int RESIZE_HANDLE = 8; // px square in bottom-right corner
    private static final int MIN_W = 40;
    private static final int MIN_H = 16;

    private final Screen parent;

    // Drag/resize state
    private HudElement dragging  = null;
    private HudElement resizing  = null;
    private int dragOffsetX, dragOffsetY;

    public HudEditScreen(Screen parent) {
        super(Text.literal("HUD Edit"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        // Must NOT pause — we want to see live game HUD
        return false;
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Very light overlay only (no renderBackground — show live game)
        ctx.fill(0, 0, width, height, 0x22000000);

        // Draw only active HUD elements + edit overlays
        for (HudRegistry.HudEntry entry : HudRegistry.getAll()) {
            HudElement element = entry.element();
            if (element instanceof BaseModule bm && !bm.isEnabled()) continue;
            ModConfig.HudElementState state = getOrInitState(entry);

            try {
                element.renderHud(ctx, delta, state.x(), state.y(), state.w(), state.h());
            } catch (Exception ignored) {}

            // Highlight border
            ctx.fill(state.x(), state.y(), state.x() + state.w(), state.y() + state.h(),
                     0x22FFFF00);
            ctx.drawStrokedRectangle(state.x(), state.y(), state.w(), state.h(), 0xAAFFFF00);

            // Element name label above box
            String name = element.getHudName();
            ctx.fill(state.x(), state.y() - 10, state.x() + textRenderer.getWidth(name) + 4,
                     state.y(), 0xAA000000);
            ctx.drawTextWithShadow(textRenderer, name, state.x() + 2, state.y() - 9, 0xFFFFFF00);

            // Resize handle in bottom-right corner
            int rhX = state.x() + state.w() - RESIZE_HANDLE;
            int rhY = state.y() + state.h() - RESIZE_HANDLE;
            ctx.fill(rhX, rhY, state.x() + state.w(), state.y() + state.h(), 0xAAFFFFFF);
        }

        // Instruction bar at the bottom
        String instruction = "HUD Edit Mode — Drag to move, corner to resize, ESC when done";
        int iw = textRenderer.getWidth(instruction);
        int ix = (width - iw) / 2;
        int iy = height - 14;
        ctx.fill(ix - 4, iy - 2, ix + iw + 4, iy + 10, 0xAA000000);
        ctx.drawTextWithShadow(textRenderer, instruction, ix, iy, 0xFFFFFFFF);

        // No super.render() call — we don't want vanilla widgets drawn
    }

    // -------------------------------------------------------------------------
    // Mouse
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean releaseOnly) {
        if (releaseOnly || click.button() != 0) return super.mouseClicked(click, releaseOnly);
        double mx = click.x(), my = click.y();

        // Iterate in reverse so topmost (last registered) is picked first
        var entries = HudRegistry.getAll();
        for (int i = entries.size() - 1; i >= 0; i--) {
            HudRegistry.HudEntry entry = entries.get(i);
            HudElement element = entry.element();
            if (element instanceof BaseModule bm && !bm.isEnabled()) continue;
            ModConfig.HudElementState state = getOrInitState(entry);

            int rhX = state.x() + state.w() - RESIZE_HANDLE;
            int rhY = state.y() + state.h() - RESIZE_HANDLE;

            if (mx >= rhX && mx <= state.x() + state.w()
             && my >= rhY && my <= state.y() + state.h()) {
                // Resize handle
                resizing  = element;
                dragging  = null;
                dragOffsetX = state.x();
                dragOffsetY = state.y();
                return true;
            }

            if (mx >= state.x() && mx <= state.x() + state.w()
             && my >= state.y() && my <= state.y() + state.h()) {
                // Drag
                dragging    = element;
                resizing    = null;
                dragOffsetX = (int)(mx - state.x());
                dragOffsetY = (int)(my - state.y());
                return true;
            }
        }
        return super.mouseClicked(click, releaseOnly);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mx = click.x(), my = click.y();

        if (dragging != null) {
            HudRegistry.HudEntry entry = HudRegistry.getById(dragging.getHudId());
            if (entry != null) {
                ModConfig.HudElementState old = getOrInitState(entry);
                int nx = Math.clamp((int)(mx - dragOffsetX), 0, width - old.w());
                int ny = Math.clamp((int)(my - dragOffsetY), 0, height - old.h());
                ModConfig.getInstance().setHudState(dragging.getHudId(),
                        new ModConfig.HudElementState(nx, ny, old.w(), old.h(), old.visible()));
            }
            return true;
        }

        if (resizing != null) {
            HudRegistry.HudEntry entry = HudRegistry.getById(resizing.getHudId());
            if (entry != null) {
                ModConfig.HudElementState old = getOrInitState(entry);
                int nw = Math.max(MIN_W, (int)(mx - old.x()));
                int nh = Math.max(MIN_H, (int)(my - old.y()));
                ModConfig.getInstance().setHudState(resizing.getHudId(),
                        new ModConfig.HudElementState(old.x(), old.y(), nw, nh, old.visible()));
            }
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        resizing = null;
        return super.mouseReleased(click);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ModConfig.HudElementState getOrInitState(HudRegistry.HudEntry entry) {
        ModConfig cfg = ModConfig.getInstance();
        ModConfig.HudElementState state = cfg.getHudState(entry.element().getHudId());
        if (state == null) {
            cfg.initHudState(entry.element().getHudId(),
                    entry.defaultX(), entry.defaultY(),
                    entry.element().getDefaultWidth(), entry.element().getDefaultHeight());
            state = cfg.getHudState(entry.element().getHudId());
        }
        return state;
    }
}
