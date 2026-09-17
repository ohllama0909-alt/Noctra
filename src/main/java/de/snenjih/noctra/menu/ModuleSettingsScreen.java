package de.snenjih.noctra.menu;

import de.snenjih.noctra.config.ModConfig;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.CustomWidget;
import de.snenjih.noctra.modules.api.settings.*;
import de.snenjih.noctra.menu.widget.*;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModuleSettingsScreen extends Screen {

    // Design colors
    private static final int COL_BG_PANEL      = 0xCC0D1B2A;
    private static final int COL_BORDER        = 0xFF1E3A5F;
    private static final int COL_BORDER_ACTIVE  = 0xFF4A7CF8;
    private static final int COL_TEXT_WHITE    = 0xFFFFFFFF;
    private static final int COL_TEXT_GRAY     = 0xFF8899AA;
    private static final int COL_BTN_DEFAULT   = 0xFF1A2A40;
    private static final int COL_BTN_HOVER     = 0xFF243350;

    private static final int PANEL_W   = 420;
    private static final int PANEL_H   = 340;
    private static final int HEADER_H  = 32;
    private static final int ROW_GAP   = 2;
    private static final int PADDING   = 8;

    private final Screen     parent;
    private final BaseModule module;

    // Widget list (SettingsWidget instances or CustomWidget wrappers)
    private final List<SettingsWidget> widgetList = new ArrayList<>();

    private float scrollOffset = 0;

    // Panel geometry
    private int panelX, panelY, panelW, panelH;
    private int contentTop, contentBottom;

    // Header button bounds
    private int backBtnX, backBtnY, backBtnW, backBtnH;
    private int resetBtnX, resetBtnY, resetBtnW, resetBtnH;

    public ModuleSettingsScreen(Screen parent, BaseModule module) {
        super(Text.literal(module.getName() + " Settings"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        widgetList.clear();

        CollapsibleSection currentSection = null;

        for (BaseModule.SettingEntry entry : module.getSettingEntries()) {
            if (entry instanceof BaseModule.SettingEntry.SectionHeader sh) {
                currentSection = new CollapsibleSection(sh.label());
                widgetList.add(currentSection);
            } else if (entry instanceof BaseModule.SettingEntry.Setting s) {
                SettingsWidget widget = createWidget(s.setting());
                if (widget != null) {
                    if (currentSection != null) {
                        currentSection.addWidget(widget);
                    } else {
                        widgetList.add(widget);
                    }
                }
            }
        }

        // Wrap CustomWidgets in an adapter
        for (CustomWidget cw : module.getCustomWidgets()) {
            widgetList.add(new CustomWidgetAdapter(cw));
        }
    }

    @SuppressWarnings("unchecked")
    private SettingsWidget createWidget(ModuleSetting<?> s) {
        if (s instanceof BooleanSetting bs)  return new ToggleWidget(bs, module);
        if (s instanceof IntSetting is)       return new SliderWidget(is, module);
        if (s instanceof FloatSetting fs)     return new SliderWidget(fs, module);
        if (s instanceof TextSetting ts)      return new TextInputWidget(ts, module);
        if (s instanceof KeybindSetting ks)   return new KeybindWidget(ks, module);
        if (s instanceof ColorSetting cs)     return new ColorPickerWidget(cs, module);
        if (s instanceof EnumSetting<?> es)   return new EnumCycleWidget<>((EnumSetting) es, module);
        return null;
    }

    @Override
    public boolean shouldPause() { return true; }

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
        // Dim background
        ctx.fill(0, 0, width, height, 0x88000000);

        panelW = Math.min(PANEL_W, width - 40);
        panelH = Math.min(PANEL_H, height - 40);
        panelX = (width  - panelW) / 2;
        panelY = (height - panelH) / 2;

        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, COL_BG_PANEL);
        ctx.drawStrokedRectangle(panelX, panelY, panelW, panelH, COL_BORDER);

        renderHeader(ctx, mouseX, mouseY);

        // Content area
        contentTop    = panelY + HEADER_H;
        contentBottom = panelY + panelH;

        // Check if we need a preview panel
        boolean hasPreview = hasCustomPreview();
        int previewH = hasPreview ? 82 : 0;
        int scrollContentBottom = contentBottom - previewH;

        ctx.enableScissor(panelX + 1, contentTop, panelX + panelW - 1, scrollContentBottom);
        renderWidgets(ctx, mouseX, mouseY);
        ctx.disableScissor();

        // Preview panel
        if (hasPreview) {
            int prevY = scrollContentBottom;
            ctx.fill(panelX, prevY, panelX + panelW, contentBottom, 0xCC0D1B2A);
            ctx.drawStrokedRectangle(panelX, prevY, panelW, previewH, COL_BORDER);
            module.renderSettingsPreview(ctx, panelX + 2, prevY + 2, panelW - 4, previewH - 4);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderHeader(DrawContext ctx, int mouseX, int mouseY) {
        int hy = panelY;
        ctx.fill(panelX, hy, panelX + panelW, hy + HEADER_H, 0xCC121E30);
        ctx.drawStrokedRectangle(panelX, hy, panelW, HEADER_H, COL_BORDER);

        // Back button
        backBtnW = 24; backBtnH = 16;
        backBtnX = panelX + 4;
        backBtnY = hy + (HEADER_H - backBtnH) / 2;
        boolean hoverBack = mouseX >= backBtnX && mouseX <= backBtnX + backBtnW
                         && mouseY >= backBtnY && mouseY <= backBtnY + backBtnH;
        ctx.fill(backBtnX, backBtnY, backBtnX + backBtnW, backBtnY + backBtnH,
                hoverBack ? COL_BTN_HOVER : COL_BTN_DEFAULT);
        ctx.drawStrokedRectangle(backBtnX, backBtnY, backBtnW, backBtnH,
                hoverBack ? COL_BORDER_ACTIVE : COL_BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer, "←",
                backBtnX + backBtnW / 2, backBtnY + (backBtnH - 8) / 2, COL_TEXT_WHITE);

        // Icon + Name centred
        int iconX = panelX + panelW / 2 - 30;
        try {
            ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, module.getIconTexture(),
                    iconX, hy + (HEADER_H - 16) / 2, 16, 16);
        } catch (Exception ignored) {}
        ctx.drawCenteredTextWithShadow(textRenderer, module.getName(),
                panelX + panelW / 2 + 6, hy + (HEADER_H - 8) / 2, COL_TEXT_WHITE);

        // Reset button
        resetBtnW = 24; resetBtnH = 16;
        resetBtnX = panelX + panelW - resetBtnW - 4;
        resetBtnY = hy + (HEADER_H - resetBtnH) / 2;
        boolean hoverReset = mouseX >= resetBtnX && mouseX <= resetBtnX + resetBtnW
                          && mouseY >= resetBtnY && mouseY <= resetBtnY + resetBtnH;
        ctx.fill(resetBtnX, resetBtnY, resetBtnX + resetBtnW, resetBtnY + resetBtnH,
                hoverReset ? COL_BTN_HOVER : COL_BTN_DEFAULT);
        ctx.drawStrokedRectangle(resetBtnX, resetBtnY, resetBtnW, resetBtnH,
                hoverReset ? COL_BORDER_ACTIVE : COL_BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer, "↺",
                resetBtnX + resetBtnW / 2, resetBtnY + (resetBtnH - 8) / 2, COL_TEXT_GRAY);
    }

    private void renderWidgets(DrawContext ctx, int mouseX, int mouseY) {
        if (widgetList.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "No settings available.",
                    panelX + panelW / 2,
                    contentTop + (contentBottom - contentTop) / 2, COL_TEXT_GRAY);
            return;
        }

        int x = panelX + PADDING;
        int w = panelW - PADDING * 2;
        int y = contentTop + PADDING - (int) scrollOffset;

        for (SettingsWidget widget : widgetList) {
            widget.render(ctx, x, y, w, mouseX, mouseY);
            y += widget.getHeight() + ROW_GAP;
        }
    }

    private boolean hasCustomPreview() {
        // Check if module overrides renderSettingsPreview (non-trivial heuristic:
        // we just check if the method is overridden by comparing class)
        try {
            return !module.getClass()
                          .getMethod("renderSettingsPreview",
                                     DrawContext.class, int.class, int.class, int.class, int.class)
                          .getDeclaringClass()
                          .equals(BaseModule.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Input dispatch
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean releaseOnly) {
        if (releaseOnly || click.button() != 0) return super.mouseClicked(click, releaseOnly);
        double mx = click.x(), my = click.y();

        // Back button
        if (mx >= backBtnX && mx <= backBtnX + backBtnW
         && my >= backBtnY && my <= backBtnY + backBtnH) {
            close();
            return true;
        }

        // Reset button
        if (mx >= resetBtnX && mx <= resetBtnX + resetBtnW
         && my >= resetBtnY && my <= resetBtnY + resetBtnH) {
            for (ModuleSetting<?> s : module.getSettings()) s.reset();
            ModConfig.getInstance().saveModuleSettings(module);
            return true;
        }

        // Widget dispatch
        for (SettingsWidget w : widgetList) {
            if (w.mouseClicked(mx, my, click.button())) return true;
        }

        return super.mouseClicked(click, releaseOnly);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        for (SettingsWidget w : widgetList) {
            if (w.mouseDragged(click.x(), click.y(), click.button(), deltaX, deltaY)) return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        for (SettingsWidget w : widgetList) {
            w.mouseReleased(click.x(), click.y(), click.button());
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                  double horizontalAmount, double verticalAmount) {
        if (mouseX >= panelX && mouseX <= panelX + panelW
         && mouseY >= contentTop && mouseY <= contentBottom) {
            scrollOffset -= (float) verticalAmount * 16f;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        // Dispatch to widgets first (e.g. keybind widget or text input)
        for (SettingsWidget w : widgetList) {
            if (w.keyPressed(keyCode, input.scancode(), input.modifiers())) return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (input.isValidChar()) {
            char chr = input.asString().charAt(0);
            for (SettingsWidget w : widgetList) {
                w.charTyped(chr, input.modifiers());
            }
        }
        return super.charTyped(input);
    }

    private void clampScroll() {
        int totalH = widgetList.stream().mapToInt(w -> w.getHeight() + ROW_GAP).sum();
        int viewH  = contentBottom - contentTop - PADDING;
        float maxS = Math.max(0f, totalH - viewH);
        scrollOffset = Math.clamp(scrollOffset, 0f, maxS);
    }

    // -------------------------------------------------------------------------
    // CustomWidget adapter
    // -------------------------------------------------------------------------

    /** Wraps a CustomWidget (module-provided) to implement SettingsWidget. */
    private static final class CustomWidgetAdapter implements SettingsWidget {
        private final CustomWidget wrapped;
        CustomWidgetAdapter(CustomWidget w) { this.wrapped = w; }

        @Override public int     getHeight()                                                  { return wrapped.getHeight(); }
        @Override public void    render(DrawContext ctx, int x, int y, int width, int mx, int my) { wrapped.render(ctx, x, y, width, mx, my); }
        @Override public boolean mouseClicked(double mx, double my, int btn)                  { return wrapped.mouseClicked(mx, my, btn); }
        @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) { return wrapped.mouseDragged(mx, my, btn, dx, dy); }
        @Override public boolean mouseReleased(double mx, double my, int btn)                 { return wrapped.mouseReleased(mx, my, btn); }
        @Override public boolean keyPressed(int kc, int sc, int mods)                        { return wrapped.keyPressed(kc, sc, mods); }
        @Override public void    charTyped(char chr, int mods)                               { wrapped.charTyped(chr, mods); }
    }
}
