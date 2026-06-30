package de.snenjih.mandatory.menu;

import de.snenjih.mandatory.menu.AccountScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class MainMenuScreen extends Screen {

    // Design colors
    private static final int COL_BORDER        = 0xFF1E3A5F;
    private static final int COL_BORDER_ACTIVE  = 0xFF4A7CF8;
    private static final int COL_TEXT_WHITE    = 0xFFFFFFFF;
    private static final int COL_TEXT_GRAY     = 0xFF8899AA;
    private static final int COL_BTN_DEFAULT   = 0xFF1A2A40;
    private static final int COL_BTN_HOVER     = 0xFF243350;
    private static final int COL_BTN_ACCENT    = 0xFF2A4A7F;

    private final Screen parent;

    // Button bounds (computed each render)
    private int btnModMenuX, btnModMenuY, btnModMenuW, btnModMenuH;
    private int btnScrX, btnScrY, btnScrW, btnScrH;
    private int btnCosX, btnCosY, btnCosW, btnCosH;
    private int btnAccX, btnAccY, btnAccW, btnAccH;

    public MainMenuScreen(Screen parent) {
        super(Text.translatable("mandatory.main.title"));
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
        // Semi-transparent overlay
        ctx.fill(0, 0, width, height, 0x88000000);

        // Big title "MANDATORY CLIENT" — scaled 2x using Matrix3x2fStack
        float scale = 2.0f;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);
        int titleY = (int)((height / 2 - 60) / scale);
        ctx.drawCenteredTextWithShadow(textRenderer, "MANDATORY CLIENT",
                (int)(width / 2 / scale), titleY, COL_TEXT_WHITE);
        ctx.getMatrices().popMatrix();

        // Sub-label
        ctx.drawCenteredTextWithShadow(textRenderer, "client-side enhancements",
                width / 2, height / 2 - 28, COL_TEXT_GRAY);

        // MOD MENU button (240×24)
        btnModMenuW = 240; btnModMenuH = 24;
        btnModMenuX = width / 2 - btnModMenuW / 2;
        btnModMenuY = height / 2 - 4;
        boolean hoverMod = mouseX >= btnModMenuX && mouseX <= btnModMenuX + btnModMenuW
                        && mouseY >= btnModMenuY && mouseY <= btnModMenuY + btnModMenuH;
        renderButton(ctx, btnModMenuX, btnModMenuY, btnModMenuW, btnModMenuH,
                Text.translatable("mandatory.main.mod_menu").getString(), hoverMod, true);

        // SCREENSHOTS button (240×24)
        btnScrW = 240; btnScrH = 24;
        btnScrX = width / 2 - btnScrW / 2;
        btnScrY = btnModMenuY + btnModMenuH + 8;
        boolean hoverScr = mouseX >= btnScrX && mouseX <= btnScrX + btnScrW
                        && mouseY >= btnScrY && mouseY <= btnScrY + btnScrH;
        renderButton(ctx, btnScrX, btnScrY, btnScrW, btnScrH,
                Text.translatable("mandatory.main.screenshots").getString(), hoverScr, false);

        // COSMETICS button (240×24)
        btnCosW = 240; btnCosH = 24;
        btnCosX = width / 2 - btnCosW / 2;
        btnCosY = btnScrY + btnScrH + 8;
        boolean hoverCos = mouseX >= btnCosX && mouseX <= btnCosX + btnCosW
                        && mouseY >= btnCosY && mouseY <= btnCosY + btnCosH;
        renderButton(ctx, btnCosX, btnCosY, btnCosW, btnCosH,
                Text.translatable("mandatory.main.cosmetics").getString(), hoverCos, false);

        // ACCOUNTS button (240×24)
        btnAccW = 240; btnAccH = 24;
        btnAccX = width / 2 - btnAccW / 2;
        btnAccY = btnCosY + btnCosH + 8;
        boolean hoverAcc = mouseX >= btnAccX && mouseX <= btnAccX + btnAccW
                        && mouseY >= btnAccY && mouseY <= btnAccY + btnAccH;
        renderButton(ctx, btnAccX, btnAccY, btnAccW, btnAccH,
                Text.translatable("mandatory.main.accounts").getString(), hoverAcc, false);

        // Version text (bottom-right)
        String version = "v" + getModVersion();
        ctx.drawTextWithShadow(textRenderer, version,
                width - textRenderer.getWidth(version) - 4, height - 12, COL_TEXT_GRAY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderButton(DrawContext ctx, int x, int y, int w, int h,
                               String label, boolean hovered, boolean accent) {
        int bg = hovered ? COL_BTN_HOVER : (accent ? COL_BTN_ACCENT : COL_BTN_DEFAULT);
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.drawStrokedRectangle(x, y, w, h, hovered ? COL_BORDER_ACTIVE : COL_BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + (h - 8) / 2, COL_TEXT_WHITE);
    }

    @Override
    public boolean mouseClicked(Click click, boolean releaseOnly) {
        if (releaseOnly || click.button() != 0) return super.mouseClicked(click, releaseOnly);
        double mx = click.x(), my = click.y();

        if (mx >= btnModMenuX && mx <= btnModMenuX + btnModMenuW
         && my >= btnModMenuY && my <= btnModMenuY + btnModMenuH) {
            assert client != null;
            client.setScreen(new ModMenuScreen(this));
            return true;
        }
        if (mx >= btnScrX && mx <= btnScrX + btnScrW
         && my >= btnScrY && my <= btnScrY + btnScrH) {
            assert client != null;
            client.setScreen(new ScreenshotGalleryScreen(this));
            return true;
        }
        if (mx >= btnCosX && mx <= btnCosX + btnCosW
         && my >= btnCosY && my <= btnCosY + btnCosH) {
            assert client != null;
            client.setScreen(new CosmeticsScreen(this));
            return true;
        }
        if (mx >= btnAccX && mx <= btnAccX + btnAccW
         && my >= btnAccY && my <= btnAccY + btnAccH) {
            assert client != null;
            client.setScreen(new AccountScreen(this));
            return true;
        }
        return super.mouseClicked(click, releaseOnly);
    }

    private static String getModVersion() {
        return FabricLoader.getInstance()
                .getModContainer("mandatory")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
    }
}
