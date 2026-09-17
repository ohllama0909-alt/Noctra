package de.snenjih.noctra.menu;

import de.snenjih.noctra.cosmetics.api.CosmeticEntry;
import de.snenjih.noctra.cosmetics.api.CosmeticType;
import de.snenjih.noctra.cosmetics.network.CosmeticNetworkHandler;
import de.snenjih.noctra.cosmetics.storage.CosmeticRegistry;
import de.snenjih.noctra.cosmetics.sync.CosmeticSyncService;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CosmeticsScreen extends Screen {

    // ---- Color palette (matches MainMenuScreen/ModMenuScreen) ---------------
    private static final int COL_OVERLAY    = 0x88000000;
    private static final int COL_PANEL_BG   = 0xCC0D1B2A;
    private static final int COL_BORDER     = 0xFF1E3A5F;
    private static final int COL_BORDER_ACT = 0xFF4A7CF8;
    private static final int COL_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COL_TEXT_GRAY  = 0xFF8899AA;
    private static final int COL_BTN_DEF    = 0xFF1A2A40;
    private static final int COL_BTN_HOVER  = 0xFF243350;
    private static final int COL_BTN_ACCENT = 0xFF2A4A7F;
    private static final int COL_ENABLED    = 0xFF44FF88;
    private static final int COL_CARD_BG    = 0xCC162030;

    // ---- Layout -------------------------------------------------------
    private static final int CARD_W    = 88;
    private static final int CARD_H    = 80;
    private static final int CARD_GAP  = 8;
    private static final int TAB_H     = 22;
    private static final int PADDING   = 10;

    private final Screen parent;

    // State
    private CosmeticType activeTab = CosmeticType.CAPE;
    private float scrollOffset = 0f;
    private float previewRotation = 0f;
    private boolean draggingPreview = false;
    private double lastMouseX;
    private int lastRenderMouseX;
    private int lastRenderMouseY;

    public CosmeticsScreen(Screen parent) {
        super(Text.translatable("noctra.cosmetics.title"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() { return true; }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        lastRenderMouseX = mouseX;
        lastRenderMouseY = mouseY;

        // Full-screen dim
        ctx.fill(0, 0, width, height, COL_OVERLAY);

        // Panel geometry
        int panelW = (int) (width  * 0.80f);
        int panelH = (int) (height * 0.82f);
        int panelX = (width  - panelW) / 2;
        int panelY = (height - panelH) / 2;

        // Panel background + border
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, COL_PANEL_BG);
        ctx.drawStrokedRectangle(panelX, panelY, panelW, panelH, COL_BORDER);

        // Split: left 40%, right 60%
        int leftW  = (int) (panelW * 0.40f);
        int rightX = panelX + leftW;
        int rightW = panelW - leftW;

        // Divider line
        ctx.fill(rightX, panelY + 1, rightX + 1, panelY + panelH - 1, COL_BORDER);

        // ---- Left panel: back button, title, skin preview, equip status ----
        renderLeftPanel(ctx, panelX, panelY, leftW, panelH, mouseX, mouseY);

        // ---- Right panel: tabs + cosmetic grid ----
        renderRightPanel(ctx, rightX, panelY, rightW, panelH, mouseX, mouseY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderLeftPanel(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
        // Back button top-left
        boolean backHover = mouseX >= x + 6 && mouseX <= x + 28 && mouseY >= y + 6 && mouseY <= y + 22;
        ctx.fill(x + 6, y + 6, x + 28, y + 22, backHover ? COL_BTN_HOVER : COL_BTN_DEF);
        ctx.drawStrokedRectangle(x + 6, y + 6, 22, 16, backHover ? COL_BORDER_ACT : COL_BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer, "←", x + 17, y + 9, COL_TEXT_WHITE);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.translatable("noctra.cosmetics.title"),
            x + w / 2, y + 30, COL_TEXT_WHITE);

        // 3D Skin Preview
        int previewCenterX = x + w / 2;
        int previewCenterY = y + h / 2 - 10;
        int previewSize = Math.min(w - 40, 80);

        if (client != null && client.player != null) {
            // drawEntity(DrawContext, x1, y1, x2, y2, scale, deltaY, yRot, xRot, LivingEntity)
            InventoryScreen.drawEntity(
                ctx,
                previewCenterX - previewSize / 2, previewCenterY - previewSize,
                previewCenterX + previewSize / 2, previewCenterY + previewSize / 2,
                previewSize / 2,
                0f,
                (float) Math.toRadians(previewRotation),
                0f,
                client.player
            );
        } else {
            // No player (e.g. main menu) — placeholder box
            ctx.fill(previewCenterX - 20, previewCenterY - 40, previewCenterX + 20, previewCenterY, COL_BTN_DEF);
            ctx.drawCenteredTextWithShadow(textRenderer, "?", previewCenterX, previewCenterY - 22, COL_TEXT_GRAY);
        }

        // Equipped status summary
        int statusY = y + h - 90;
        ctx.drawTextWithShadow(textRenderer, "Equipped:", x + 12, statusY, COL_TEXT_GRAY);
        statusY += 12;
        for (CosmeticType type : CosmeticType.values()) {
            String equipped = CosmeticRegistry.getSelfEquipped(type);
            String typeName = capitalize(type.id());
            String val = equipped != null ? equipped : "—";
            int color = equipped != null ? COL_ENABLED : COL_TEXT_GRAY;
            ctx.drawTextWithShadow(textRenderer, typeName + ": " + val, x + 12, statusY, color);
            statusY += 11;
        }
    }

    private void renderRightPanel(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
        // ---- Tab bar -------------------------------------------------------
        CosmeticType[] types = CosmeticType.values();
        int tabW = (w - 2 * PADDING) / types.length;
        int tabY = y + 6;

        for (int i = 0; i < types.length; i++) {
            CosmeticType type = types[i];
            int tx = x + PADDING + i * tabW;
            boolean active = type == activeTab;
            boolean hover  = !active && mouseX >= tx && mouseX <= tx + tabW - 2
                             && mouseY >= tabY && mouseY <= tabY + TAB_H;

            int bg   = active ? COL_BTN_ACCENT : (hover ? COL_BTN_HOVER : COL_BTN_DEF);
            int bord = active ? COL_BORDER_ACT : COL_BORDER;
            ctx.fill(tx, tabY, tx + tabW - 2, tabY + TAB_H, bg);
            ctx.drawStrokedRectangle(tx, tabY, tabW - 2, TAB_H, bord);

            String label = capitalize(type.id().replace("_", " "));
            // Truncate label if too wide
            if (textRenderer.getWidth(label) > tabW - 8) {
                label = label.substring(0, 1).toUpperCase();
            }
            ctx.drawCenteredTextWithShadow(textRenderer, label, tx + (tabW - 2) / 2, tabY + 6, COL_TEXT_WHITE);
        }

        // ---- Content area --------------------------------------------------
        int contentY = tabY + TAB_H + 6;
        int contentH = h - (contentY - y) - 8;
        int contentX = x + PADDING;
        int contentW = w - 2 * PADDING;

        // Sync banner
        if (CosmeticSyncService.getInstance().isSyncing()) {
            ctx.fill(contentX, contentY, contentX + contentW, contentY + 18, 0xCC1A3A1A);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("noctra.cosmetics.syncing"),
                contentX + contentW / 2, contentY + 5, COL_TEXT_WHITE);
            contentY += 22;
            contentH -= 22;
        }

        // Grid of cosmetic cards
        List<CosmeticEntry> entries = CosmeticRegistry.getAvailableByType(activeTab);
        int totalCards = entries.size() + 1; // +1 for "None"

        int cols = Math.max(1, contentW / (CARD_W + CARD_GAP));
        int rows = (int) Math.ceil(totalCards / (float) cols);
        int totalGridH = rows * (CARD_H + CARD_GAP);

        // Clamp scroll
        float maxScroll = Math.max(0, totalGridH - contentH);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        // Scissor test for content area
        ctx.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);

        int drawY = contentY - (int) scrollOffset;
        for (int i = 0; i < totalCards; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = contentX + col * (CARD_W + CARD_GAP);
            int cy = drawY + row * (CARD_H + CARD_GAP);

            if (cy + CARD_H < contentY || cy > contentY + contentH) continue; // out of view

            boolean isNone = (i == 0);
            String cardId   = isNone ? null : entries.get(i - 1).id();
            String cardName = isNone
                ? Text.translatable("noctra.cosmetics.none").getString()
                : entries.get(i - 1).name();

            boolean isEquipped = isNone
                ? (CosmeticRegistry.getSelfEquipped(activeTab) == null)
                : cardId.equals(CosmeticRegistry.getSelfEquipped(activeTab));

            boolean hover = mouseX >= cx && mouseX <= cx + CARD_W
                          && mouseY >= cy && mouseY <= cy + CARD_H;

            // Card background + border
            ctx.fill(cx, cy, cx + CARD_W, cy + CARD_H, hover ? COL_BTN_HOVER : COL_CARD_BG);
            ctx.drawStrokedRectangle(cx, cy, CARD_W, CARD_H,
                isEquipped ? COL_BORDER_ACT : COL_BORDER);

            // Preview icon
            int iconX    = cx + 4;
            int iconY    = cy + 4;
            int iconSize = CARD_H - 28;
            if (!isNone) {
                Identifier tex = CosmeticRegistry.getTextureIdentifier(cardId);
                if (tex != null) {
                    ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, tex, iconX, iconY, iconSize, iconSize);
                } else {
                    ctx.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, COL_BTN_DEF);
                    ctx.drawCenteredTextWithShadow(textRenderer, "?",
                        iconX + iconSize / 2, iconY + iconSize / 2 - 4, COL_TEXT_GRAY);
                }
            } else {
                ctx.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, COL_BTN_DEF);
                ctx.drawCenteredTextWithShadow(textRenderer, "X",
                    iconX + iconSize / 2, iconY + iconSize / 2 - 4, COL_TEXT_GRAY);
            }

            // Card name (truncated)
            String displayName = cardName.length() > 10 ? cardName.substring(0, 9) + "…" : cardName;
            ctx.drawCenteredTextWithShadow(textRenderer, displayName,
                cx + CARD_W / 2, cy + CARD_H - 22, COL_TEXT_WHITE);

            // Equipped indicator
            if (isEquipped) {
                ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("noctra.cosmetics.equipped").getString(),
                    cx + CARD_W / 2, cy + CARD_H - 11, COL_ENABLED);
            }
        }

        ctx.disableScissor();
    }

    // ---- Input handling ---------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean releaseOnly) {
        if (releaseOnly || click.button() != 0) return super.mouseClicked(click, releaseOnly);
        double mx = click.x();
        double my = click.y();

        int panelW = (int) (width  * 0.80f);
        int panelH = (int) (height * 0.82f);
        int panelX = (width  - panelW) / 2;
        int panelY = (height - panelH) / 2;

        // Back button
        if (mx >= panelX + 6 && mx <= panelX + 28 && my >= panelY + 6 && my <= panelY + 22) {
            assert client != null;
            client.setScreen(parent);
            return true;
        }

        int leftW  = (int) (panelW * 0.40f);
        int rightX = panelX + leftW;

        // Preview drag start (left panel)
        if (mx >= panelX && mx <= rightX && my >= panelY && my <= panelY + panelH) {
            draggingPreview = true;
            lastMouseX = mx;
            return true;
        }

        // Tab clicks (right panel)
        CosmeticType[] types = CosmeticType.values();
        int rightW = panelW - leftW;
        int tabW   = (rightW - 2 * PADDING) / types.length;
        int tabY   = panelY + 6;
        for (int i = 0; i < types.length; i++) {
            int tx = rightX + PADDING + i * tabW;
            if (mx >= tx && mx <= tx + tabW - 2 && my >= tabY && my <= tabY + TAB_H) {
                activeTab    = types[i];
                scrollOffset = 0;
                return true;
            }
        }

        // Card clicks (right panel grid)
        int contentY = tabY + TAB_H + 6;
        if (CosmeticSyncService.getInstance().isSyncing()) contentY += 22;
        int contentX = rightX + PADDING;
        int contentW = rightW - 2 * PADDING;
        int contentH = panelH - (contentY - panelY) - 8;

        List<CosmeticEntry> entries = CosmeticRegistry.getAvailableByType(activeTab);
        int totalCards = entries.size() + 1;
        int cols   = Math.max(1, contentW / (CARD_W + CARD_GAP));
        int drawY  = contentY - (int) scrollOffset;

        if (mx >= contentX && mx <= contentX + contentW && my >= contentY && my <= contentY + contentH) {
            for (int i = 0; i < totalCards; i++) {
                int col = i % cols;
                int row = i / cols;
                int cx  = contentX + col * (CARD_W + CARD_GAP);
                int cy  = drawY + row * (CARD_H + CARD_GAP);
                if (mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H) {
                    if (i == 0) {
                        CosmeticRegistry.unequip(activeTab);
                    } else {
                        String cardId = entries.get(i - 1).id();
                        if (cardId.equals(CosmeticRegistry.getSelfEquipped(activeTab))) {
                            CosmeticRegistry.unequip(activeTab);
                        } else {
                            CosmeticRegistry.equip(activeTab, cardId);
                        }
                        CosmeticNetworkHandler.sendSelf();
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(click, releaseOnly);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingPreview) {
            previewRotation += (float) (click.x() - lastMouseX) * 1.5f;
            lastMouseX = click.x();
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingPreview = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Math.max(0, scrollOffset - (float) verticalAmount * 12f);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            assert client != null;
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
