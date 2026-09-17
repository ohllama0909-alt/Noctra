package de.snenjih.noctra.menu;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.Module;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.impl.mod_settings.ModSettingsModule;
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
import java.util.stream.Collectors;

public class ModMenuScreen extends Screen {

    // Static design colors (non-theme)
    private static final int COL_TEXT_WHITE    = 0xFFFFFFFF;
    private static final int COL_TEXT_GRAY     = 0xFF8899AA;
    private static final int COL_TEXT_DIM      = 0xFF556677;
    private static final int COL_BORDER        = 0xFF1E3A5F;
    private static final int COL_BTN_DEFAULT   = 0xFF1A2A40;
    private static final int COL_BTN_HOVER     = 0xFF243350;

    // Disabled card colors
    private static final int COL_CARD_DIS_BG   = 0xAA0C1018;
    private static final int COL_CARD_DIS_BDR  = 0xFF333444;
    private static final int COL_TEXT_DIS_NAME = 0xFF778899;
    private static final int COL_TEXT_DIS_DESC = 0xFF445566;

    // Layout
    private static final int CARD_H    = 60;
    private static final int CARD_GAP  = 10;
    private static final int COLS      = 2;
    private static final int TAB_H     = 22;
    private static final int FOOTER_H  = 30;
    private static final int PADDING   = 12;

    // Settings button inside each card
    private static final int GEAR_BTN_W = 38;
    private static final int GEAR_BTN_H = 22;

    private final Screen parent;

    private ModuleCategory selectedCategory = null; // null = All
    private String         searchQuery      = "";
    private boolean        searchActive     = false;
    private float          scrollOffset     = 0;

    private List<Module> visibleModules = new ArrayList<>();

    // Panel geometry (computed in render)
    private int panelX, panelY, panelW, panelH;
    private int tabBarTop, tabBarBottom, contentTop, contentBottom, footerTop;

    // Search field bounds
    private int searchX, searchY, searchW, searchH;

    // Edit HUDs button
    private int hudBtnX, hudBtnY, hudBtnW, hudBtnH;

    public ModMenuScreen(Screen parent) {
        super(Text.translatable("noctra.modmenu.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        updateVisible();
    }

    @Override
    public boolean shouldPause() { return true; }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // -------------------------------------------------------------------------
    // Theme helpers — read live from ModSettingsModule
    // -------------------------------------------------------------------------

    private int panelBg() {
        if (ModSettingsModule.INSTANCE == null) return 0xAA0D1B2A;
        int alpha = (int)(ModSettingsModule.INSTANCE.getTransparency() * 255) & 0xFF;
        int rgb   = ModSettingsModule.INSTANCE.getMenuBgColor() & 0x00FFFFFF;
        return (alpha << 24) | rgb;
    }

    private int cardBg() {
        if (ModSettingsModule.INSTANCE == null) return 0xAA121E30;
        int alpha = (int)(ModSettingsModule.INSTANCE.getTransparency() * 220) & 0xFF;
        int rgb   = ModSettingsModule.INSTANCE.getMenuBgColor() & 0x00FFFFFF;
        // slightly lighter than panel
        int r = Math.min(255, ((rgb >> 16) & 0xFF) + 14);
        int g = Math.min(255, ((rgb >>  8) & 0xFF) + 14);
        int b = Math.min(255,  (rgb        & 0xFF) + 14);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    private int accent() {
        return ModSettingsModule.INSTANCE != null
            ? ModSettingsModule.INSTANCE.getPrimaryColor()
            : 0xFF4A7CF8;
    }

    private int accentBorder() { return accent(); }

    // -------------------------------------------------------------------------
    // Filtering
    // -------------------------------------------------------------------------

    private void updateVisible() {
        String q = searchQuery.toLowerCase();
        visibleModules = ModuleRegistry.getInstance().getAll().stream()
                .filter(m -> selectedCategory == null || m.getCategory() == selectedCategory)
                .filter(m -> q.isEmpty()
                        || m.getName().toLowerCase().contains(q)
                        || m.getDescription().toLowerCase().contains(q))
                .collect(Collectors.toList());
        scrollOffset = 0;
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Full-screen dim
        ctx.fill(0, 0, width, height, 0x88000000);

        // Panel — 68% x 78% centred (zoomed-out look)
        panelW = (int)(width  * 0.68f);
        panelH = (int)(height * 0.78f);
        panelX = (width  - panelW) / 2;
        panelY = (height - panelH) / 2;

        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, panelBg());
        ctx.drawStrokedRectangle(panelX, panelY, panelW, panelH, COL_BORDER);

        // Tab-bar
        tabBarTop    = panelY;
        tabBarBottom = panelY + TAB_H;
        renderTabBar(ctx, mouseX, mouseY);

        // Content area
        contentTop    = tabBarBottom;
        footerTop     = panelY + panelH - FOOTER_H;
        contentBottom = footerTop;

        // Grid of module cards
        ctx.enableScissor(panelX + 1, contentTop, panelX + panelW - 1, contentBottom);
        renderModuleGrid(ctx, mouseX, mouseY);
        ctx.disableScissor();

        // Footer
        renderFooter(ctx, mouseX, mouseY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderTabBar(DrawContext ctx, int mouseX, int mouseY) {
        ModuleCategory[] cats = ModuleCategory.values();
        int numTabs = cats.length + 1; // +1 for All

        searchW = 120;
        searchH = TAB_H - 4;
        searchX = panelX + panelW - searchW - 4;
        searchY = panelY + 2;

        int availW = searchX - panelX - 4;
        int tabW   = Math.min(60, availW / numTabs - 2);

        for (int i = 0; i < numTabs; i++) {
            ModuleCategory cat = (i == 0) ? null : cats[i - 1];
            boolean active  = (cat == selectedCategory);
            int tabX = panelX + 2 + i * (tabW + 2);
            int tabY = panelY;

            boolean hovered = mouseX >= tabX && mouseX <= tabX + tabW
                           && mouseY >= tabY && mouseY <= tabY + TAB_H;

            int bg = active  ? accent()
                   : hovered ? COL_BTN_HOVER
                   :           COL_BTN_DEFAULT;

            ctx.fill(tabX, tabY, tabX + tabW, tabY + TAB_H, bg);
            ctx.drawStrokedRectangle(tabX, tabY, tabW, TAB_H,
                    active ? accentBorder() : COL_BORDER);

            String label = (cat == null) ? "All"
                    : cat.name().charAt(0) + cat.name().substring(1).toLowerCase();
            ctx.drawCenteredTextWithShadow(textRenderer, label,
                    tabX + tabW / 2, tabY + (TAB_H - 8) / 2,
                    active ? COL_TEXT_WHITE : COL_TEXT_GRAY);
        }

        // Search field
        ctx.fill(searchX, searchY, searchX + searchW, searchY + searchH, COL_BTN_DEFAULT);
        ctx.drawStrokedRectangle(searchX, searchY, searchW, searchH,
                searchActive ? accentBorder() : COL_BORDER);

        String display = searchQuery.isEmpty() && !searchActive
                ? "Search..."
                : (searchQuery + (searchActive && System.currentTimeMillis() % 1000 < 500 ? "|" : ""));
        int textColor = searchQuery.isEmpty() && !searchActive ? COL_TEXT_DIM : COL_TEXT_WHITE;
        ctx.drawTextWithShadow(textRenderer, display, searchX + 4, searchY + (searchH - 8) / 2, textColor);
    }

    private void renderModuleGrid(DrawContext ctx, int mouseX, int mouseY) {
        if (visibleModules.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "No modules found.",
                    panelX + panelW / 2, contentTop + (contentBottom - contentTop) / 2, COL_TEXT_GRAY);
            return;
        }

        int colW   = (panelW - PADDING * 2 - CARD_GAP) / COLS;
        int startY = contentTop + PADDING - (int) scrollOffset;

        for (int i = 0; i < visibleModules.size(); i++) {
            int col   = i % COLS;
            int row   = i / COLS;
            int cardX = panelX + PADDING + col * (colW + CARD_GAP);
            int cardY = startY + row * (CARD_H + CARD_GAP);

            if (cardY + CARD_H < contentTop || cardY > contentBottom) continue;

            renderCard(ctx, visibleModules.get(i), cardX, cardY, colW, mouseX, mouseY);
        }
    }

    private void renderCard(DrawContext ctx, Module module, int x, int y, int w, int mx, int my) {
        boolean hovered  = mx >= x && mx <= x + w && my >= y && my <= y + CARD_H;
        boolean enabled  = module.isEnabled();

        int gearBtnX = x + w - GEAR_BTN_W - 6;
        int gearBtnY = y + (CARD_H - GEAR_BTN_H) / 2;
        boolean hoverGear = mx >= gearBtnX && mx <= gearBtnX + GEAR_BTN_W
                         && my >= gearBtnY && my <= gearBtnY + GEAR_BTN_H;

        // Card bg and border
        if (enabled) {
            int bg = hovered ? 0xBB1A2C40 : cardBg();
            ctx.fill(x, y, x + w, y + CARD_H, bg);
            ctx.drawStrokedRectangle(x, y, w, CARD_H, accentBorder());
        } else {
            ctx.fill(x, y, x + w, y + CARD_H, COL_CARD_DIS_BG);
            ctx.drawStrokedRectangle(x, y, w, CARD_H, COL_CARD_DIS_BDR);
        }

        // Icon (28x28), vertically centered with extra left padding
        try {
            ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, module.getIconTexture(),
                    x + 6, y + (CARD_H - 28) / 2, 28, 28);
        } catch (Exception ignored) {}

        // Name
        int nameColor = enabled ? COL_TEXT_WHITE : COL_TEXT_DIS_NAME;
        ctx.drawTextWithShadow(textRenderer, module.getName(),
                x + 40, y + 14, nameColor);

        // Description (truncated)
        String desc = module.getDescription();
        if (desc.length() > 44) desc = desc.substring(0, 44) + "…";
        int descColor = enabled ? COL_TEXT_GRAY : COL_TEXT_DIS_DESC;
        ctx.drawTextWithShadow(textRenderer, desc, x + 40, y + 26, descColor);

        // Enabled indicator dot (6x6) left of settings button
        int dotX = gearBtnX - 12;
        int dotY = y + CARD_H / 2 - 3;
        int dotColor = enabled ? 0xFF44DD88 : 0xFF666677;
        int dotBdr   = enabled ? 0xFF66FFAA : 0xFF444455;
        ctx.fill(dotX, dotY, dotX + 6, dotY + 6, dotColor);
        ctx.drawStrokedRectangle(dotX, dotY, 6, 6, dotBdr);

        // Settings button box
        int gearBg  = hoverGear ? COL_BTN_HOVER : COL_BTN_DEFAULT;
        int gearBdr = hoverGear ? accentBorder() : COL_BORDER;
        ctx.fill(gearBtnX, gearBtnY, gearBtnX + GEAR_BTN_W, gearBtnY + GEAR_BTN_H, gearBg);
        ctx.drawStrokedRectangle(gearBtnX, gearBtnY, GEAR_BTN_W, GEAR_BTN_H, gearBdr);
        ctx.drawCenteredTextWithShadow(textRenderer, "⚙",
                gearBtnX + GEAR_BTN_W / 2, gearBtnY + (GEAR_BTN_H - 8) / 2,
                hoverGear ? accent() : COL_TEXT_GRAY);
    }

    private void renderFooter(DrawContext ctx, int mouseX, int mouseY) {
        ctx.fill(panelX, footerTop, panelX + panelW, panelY + panelH, panelBg());
        ctx.drawStrokedRectangle(panelX, footerTop, panelW, FOOTER_H, COL_BORDER);

        // Edit HUDs button
        hudBtnW = 110;
        hudBtnH = 18;
        hudBtnX = panelX + PADDING;
        hudBtnY = footerTop + (FOOTER_H - hudBtnH) / 2;
        boolean hoverHud = mouseX >= hudBtnX && mouseX <= hudBtnX + hudBtnW
                        && mouseY >= hudBtnY && mouseY <= hudBtnY + hudBtnH;
        ctx.fill(hudBtnX, hudBtnY, hudBtnX + hudBtnW, hudBtnY + hudBtnH,
                hoverHud ? COL_BTN_HOVER : COL_BTN_DEFAULT);
        ctx.drawStrokedRectangle(hudBtnX, hudBtnY, hudBtnW, hudBtnH,
                hoverHud ? accentBorder() : COL_BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer, "✦ Edit HUDs",
                hudBtnX + hudBtnW / 2, hudBtnY + (hudBtnH - 8) / 2, COL_TEXT_WHITE);

        // Module count
        String count = visibleModules.size() + " modules";
        ctx.drawTextWithShadow(textRenderer, count,
                panelX + panelW - textRenderer.getWidth(count) - PADDING,
                footerTop + (FOOTER_H - 8) / 2, COL_TEXT_GRAY);
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean releaseOnly) {
        if (releaseOnly || click.button() != 0) return super.mouseClicked(click, releaseOnly);
        double mx = click.x(), my = click.y();

        // Tab bar
        if (my >= panelY && my <= tabBarBottom) {
            if (mx >= searchX && mx <= searchX + searchW
             && my >= searchY && my <= searchY + searchH) {
                searchActive = true;
                return true;
            }
            ModuleCategory[] cats = ModuleCategory.values();
            int numTabs = cats.length + 1;
            int availW  = searchX - panelX - 4;
            int tabW    = Math.min(60, availW / numTabs - 2);
            for (int i = 0; i < numTabs; i++) {
                int tabX = panelX + 2 + i * (tabW + 2);
                if (mx >= tabX && mx <= tabX + tabW && my >= panelY && my <= panelY + TAB_H) {
                    ModuleCategory cat = (i == 0) ? null : cats[i - 1];
                    selectedCategory = cat;
                    searchActive = false;
                    updateVisible();
                    return true;
                }
            }
        }

        // Deactivate search on click outside
        if (searchActive && !(mx >= searchX && mx <= searchX + searchW
                           && my >= searchY && my <= searchY + searchH)) {
            searchActive = false;
        }

        // Footer — HUD Edit button
        if (mx >= hudBtnX && mx <= hudBtnX + hudBtnW
         && my >= hudBtnY && my <= hudBtnY + hudBtnH) {
            assert client != null;
            client.setScreen(new HudEditScreen(this));
            return true;
        }

        // Module cards
        if (my >= contentTop && my <= contentBottom) {
            int colW   = (panelW - PADDING * 2 - CARD_GAP) / COLS;
            int startY = contentTop + PADDING - (int) scrollOffset;

            for (int i = 0; i < visibleModules.size(); i++) {
                int col   = i % COLS;
                int row   = i / COLS;
                int cardX = panelX + PADDING + col * (colW + CARD_GAP);
                int cardY = startY + row * (CARD_H + CARD_GAP);

                if (mx >= cardX && mx <= cardX + colW
                 && my >= cardY && my <= cardY + CARD_H) {
                    Module m = visibleModules.get(i);
                    // Gear button zone → settings screen
                    int gearBtnX = cardX + colW - GEAR_BTN_W - 6;
                    int gearBtnY = cardY + (CARD_H - GEAR_BTN_H) / 2;
                    if (mx >= gearBtnX && mx <= gearBtnX + GEAR_BTN_W
                     && my >= gearBtnY && my <= gearBtnY + GEAR_BTN_H
                     && m instanceof BaseModule bm) {
                        assert client != null;
                        client.setScreen(new ModuleSettingsScreen(this, bm));
                    } else {
                        ModuleRegistry.getInstance().toggle(m);
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(click, releaseOnly);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                  double horizontalAmount, double verticalAmount) {
        if (mouseX >= panelX && mouseX <= panelX + panelW
         && mouseY >= contentTop && mouseY <= contentBottom) {
            scrollOffset -= (float) verticalAmount * 20f;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (searchActive) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    updateVisible();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                searchActive = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchActive = false;
                searchQuery  = "";
                updateVisible();
                return true;
            }
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchActive && input.isValidChar()) {
            searchQuery += input.asString();
            updateVisible();
            return true;
        }
        return super.charTyped(input);
    }

    private void clampScroll() {
        int rows   = (int) Math.ceil((float) visibleModules.size() / COLS);
        float maxS = Math.max(0f, rows * (CARD_H + CARD_GAP) - (contentBottom - contentTop - PADDING));
        scrollOffset = Math.clamp(scrollOffset, 0f, maxS);
    }
}
