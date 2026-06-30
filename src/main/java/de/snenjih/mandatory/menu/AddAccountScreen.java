package de.snenjih.mandatory.menu;

import de.snenjih.mandatory.auth.AccountEntry;
import de.snenjih.mandatory.auth.AccountManager;
import de.snenjih.mandatory.auth.SkinCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AddAccountScreen extends Screen {

    // Colors (shared design language)
    private static final int COL_BORDER     = 0xFF1E3A5F;
    private static final int COL_BORDER_ACC = 0xFF4A7CF8;
    private static final int COL_TEXT       = 0xFFFFFFFF;
    private static final int COL_SUBTEXT    = 0xFF8899AA;
    private static final int COL_BTN_DEF    = 0xFF1A2A40;
    private static final int COL_BTN_HOV    = 0xFF243350;
    private static final int COL_SUCCESS    = 0xFF44FF88;
    private static final int COL_ERROR      = 0xFFFF5555;
    private static final int COL_PROG_FG    = 0xFF4A7CF8;
    private static final int COL_PROG_BG    = 0xFF1A2A40;
    private static final int COL_CODE       = 0xFFE0A000;

    private enum State { LOADING, AWAITING_USER, SUCCESS, ERROR }

    private final Screen parent;

    private State  state     = State.LOADING;
    private String userCode  = "";
    private int    expiresIn = 900;
    private long   startedAt = System.currentTimeMillis();
    private String errorMsg  = "";

    private AccountEntry addedEntry;
    private long         successAt;
    private long         copiedUntil;

    // Animation
    private float openProgress  = 0f;
    private float cancelHover   = 0f;
    private float retryHover    = 0f;
    private float copyHover     = 0f;

    // Button / box bounds (set during render, used in mouseClicked)
    private int cancelX, cancelY, cancelW = 100, cancelH = 20;
    private int retryX,  retryY,  retryW  = 100, retryH  = 20;
    private int copyX,   copyY,   copyW,   copyH = 20;

    public AddAccountScreen(Screen parent) {
        super(Text.literal(""));
        this.parent = parent;
    }

    @Override
    protected void init() {
        startAuthFlow();
    }

    // ---- Auth flow ----------------------------------------------------------

    private void startAuthFlow() {
        state     = State.LOADING;
        startedAt = System.currentTimeMillis();
        errorMsg  = "";

        AccountManager.getInstance().startAuth(
            // DeviceCodeHandler
            (code, url, expiresSeconds) -> {
                this.userCode  = code;
                this.expiresIn = expiresSeconds;
                this.startedAt = System.currentTimeMillis();
                this.state     = State.AWAITING_USER;
            },
            // onSuccess
            entry -> {
                addedEntry = entry;
                successAt  = System.currentTimeMillis();
                state      = State.SUCCESS;
            },
            // onError
            msg -> {
                if ("cancelled".equals(msg)) { close(); return; }
                errorMsg = msg;
                state    = State.ERROR;
            }
        );
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        AccountManager.getInstance().cancelAuth();
        assert client != null;
        client.setScreen(parent);
    }

    // ---- Render -------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        openProgress = Math.min(1f, openProgress + 0.07f);

        int oa = (int)(0xD0 * openProgress);
        ctx.fill(0, 0, width, height, (oa << 24) | 0x050E18);

        int panelW = 300;
        int panelH = panelHeight();
        int panelX = width  / 2 - panelW / 2;
        int panelY = height / 2 - panelH / 2;
        int cx     = panelX + panelW / 2;
        int a      = (int)(0xFF * openProgress);

        // Panel background + border
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, (a << 24) | 0x0D1B2A);
        ctx.drawStrokedRectangle(panelX, panelY, panelW, panelH, (a << 24) | 0x1E3A5F);

        // Title
        int y = panelY + 14;
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("mandatory.add_account.title").getString(), cx, y,
                withAlpha(COL_TEXT, a));
        y += 16;
        ctx.fill(panelX + 12, y, panelX + panelW - 12, y + 1, (a << 24) | 0x1E3A5F);
        y += 10;

        // State-specific content
        switch (state) {
            case LOADING      -> renderLoading(ctx, cx, y, panelX, panelW, panelY, panelH, a, mouseX, mouseY);
            case AWAITING_USER-> renderAwaiting(ctx, cx, y, panelX, panelW, panelY, panelH, a, mouseX, mouseY);
            case SUCCESS      -> renderSuccess(ctx, cx, y, a);
            case ERROR        -> renderError(ctx, cx, y, panelX, panelW, panelY, panelH, a, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private int panelHeight() {
        return switch (state) {
            case LOADING       -> 120;
            case AWAITING_USER -> 236;
            case SUCCESS       -> 140;
            case ERROR         -> 150;
        };
    }

    // Loading state
    private void renderLoading(DrawContext ctx, int cx, int y, int px, int pw,
                                int panelY, int panelH, int a, int mx, int my) {
        ctx.drawCenteredTextWithShadow(textRenderer, "Connecting to Microsoft...",
                cx, y + 16, withAlpha(COL_SUBTEXT, a));
        renderSpinner(ctx, cx, y + 46, a);
        renderCancelBtn(ctx, cx, panelY + panelH - 30, a, mx, my);
    }

    // Device-code state
    private void renderAwaiting(DrawContext ctx, int cx, int y, int panelX, int panelW,
                                 int panelY, int panelH, int a, int mx, int my) {
        int boxW = panelW - 40;
        int boxX = panelX + 20;

        // Visit URL
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("mandatory.add_account.visit").getString(),
                cx, y, withAlpha(COL_SUBTEXT, a));
        y += 14;

        // URL pill
        ctx.fill(boxX, y, boxX + boxW, y + 14, (a << 24) | 0x0D1B2A);
        ctx.drawStrokedRectangle(boxX, y, boxW, 14, (a << 24) | 0x1E3A5F);
        ctx.drawCenteredTextWithShadow(textRenderer, "microsoft.com/link",
                cx, y + 3, withAlpha(COL_TEXT, a));
        y += 22;

        // "Enter code" label
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("mandatory.add_account.code").getString(),
                cx, y, withAlpha(COL_SUBTEXT, a));
        y += 14;

        // Code box (also the copy button)
        copyW = boxW; copyH = 20; copyX = boxX; copyY = y;
        boolean hovCopy = mx >= copyX && mx <= copyX + copyW && my >= copyY && my <= copyY + copyH;
        copyHover += (hovCopy ? 1f : -1f) * 0.18f;
        copyHover  = Math.max(0f, Math.min(1f, copyHover));

        ctx.fill(copyX, copyY, copyX + copyW, copyY + copyH, (a << 24) | 0x0A1520);
        ctx.drawStrokedRectangle(copyX, copyY, copyW, copyH,
                withAlpha(lerpColor(COL_BORDER, COL_BORDER_ACC, copyHover), a));

        // Code text with spacing
        String displayed = userCode.replace("-", "  -  ");
        ctx.drawCenteredTextWithShadow(textRenderer, displayed,
                cx - 14, copyY + 6, withAlpha(COL_CODE, a));

        // Copy icon
        boolean copied = System.currentTimeMillis() < copiedUntil;
        ctx.drawTextWithShadow(textRenderer, copied ? "✔" : "⎘",
                copyX + copyW - 18, copyY + 6,
                withAlpha(copied ? COL_SUCCESS : COL_SUBTEXT, a));
        y += 28;

        // Expiry countdown
        long remaining = Math.max(0, (long) expiresIn * 1000 - (System.currentTimeMillis() - startedAt));
        int  remSecs   = (int)(remaining / 1000);
        String timerStr = String.format("%d:%02d", remSecs / 60, remSecs % 60);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("mandatory.add_account.expires_in", timerStr).getString(),
                cx, y, withAlpha(COL_SUBTEXT, a));
        y += 12;

        // Progress bar
        float pct = remaining / (float)(expiresIn * 1000L);
        ctx.fill(boxX, y, boxX + boxW, y + 4, (a << 24) | (COL_PROG_BG & 0xFFFFFF));
        if (pct > 0)
            ctx.fill(boxX, y, boxX + (int)(boxW * pct), y + 4, (a << 24) | (COL_PROG_FG & 0xFFFFFF));
        y += 14;

        // Waiting hint
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("mandatory.add_account.waiting").getString(),
                cx, y, withAlpha(COL_SUBTEXT, a));

        renderCancelBtn(ctx, cx, panelY + panelH - 30, a, mx, my);
    }

    // Success state
    private void renderSuccess(DrawContext ctx, int cx, int y, int a) {
        if (System.currentTimeMillis() - successAt > 1500) {
            assert client != null;
            client.setScreen(parent);
            return;
        }
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("mandatory.add_account.success").getString(),
                cx, y + 10, withAlpha(COL_SUCCESS, a));

        if (addedEntry != null) {
            SkinCache cache = SkinCache.getInstance();
            if (cache != null && cache.isLoaded(addedEntry.uuid())) {
                Identifier skinId = cache.getTextureId(addedEntry.uuid());
                int headS = 32, headX = cx - 16, headY = y + 30;
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, skinId,
                        headX, headY, 8f, 8f, headS, headS, 8, 8, 64, 64);
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, skinId,
                        headX, headY, 40f, 8f, headS, headS, 8, 8, 64, 64);
                ctx.drawCenteredTextWithShadow(textRenderer, addedEntry.username(),
                        cx, headY + headS + 4, withAlpha(COL_TEXT, a));
            } else {
                ctx.drawCenteredTextWithShadow(textRenderer, addedEntry.username(),
                        cx, y + 40, withAlpha(COL_TEXT, a));
            }
        }
    }

    // Error state
    private void renderError(DrawContext ctx, int cx, int y, int panelX, int panelW,
                              int panelY, int panelH, int a, int mx, int my) {
        ctx.drawCenteredTextWithShadow(textRenderer, mapError(errorMsg),
                cx, y + 10, withAlpha(COL_ERROR, a));

        retryW = 100; retryH = 20;
        retryX = cx - 50; retryY = y + 40;
        boolean hovR = mx >= retryX && mx <= retryX + retryW && my >= retryY && my <= retryY + retryH;
        retryHover += (hovR ? 1f : -1f) * 0.18f;
        retryHover  = Math.max(0f, Math.min(1f, retryHover));

        ctx.fill(retryX, retryY, retryX + retryW, retryY + retryH,
                (a << 24) | (lerpColor(COL_BTN_DEF, COL_BTN_HOV, retryHover) & 0xFFFFFF));
        ctx.drawStrokedRectangle(retryX, retryY, retryW, retryH,
                withAlpha(lerpColor(COL_BORDER, COL_BORDER_ACC, retryHover), a));
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("mandatory.add_account.retry").getString(),
                cx, retryY + 6, withAlpha(COL_TEXT, a));

        renderCancelBtn(ctx, cx, panelY + panelH - 30, a, mx, my);
    }

    private void renderCancelBtn(DrawContext ctx, int cx, int y, int a, int mx, int my) {
        cancelW = 100; cancelH = 20;
        cancelX = cx - 50; cancelY = y;
        boolean hov = mx >= cancelX && mx <= cancelX + cancelW && my >= cancelY && my <= cancelY + cancelH;
        cancelHover += (hov ? 1f : -1f) * 0.18f;
        cancelHover  = Math.max(0f, Math.min(1f, cancelHover));

        ctx.fill(cancelX, cancelY, cancelX + cancelW, cancelY + cancelH,
                (a << 24) | (lerpColor(COL_BTN_DEF, COL_BTN_HOV, cancelHover) & 0xFFFFFF));
        ctx.drawStrokedRectangle(cancelX, cancelY, cancelW, cancelH, (a << 24) | (COL_BORDER & 0xFFFFFF));
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("mandatory.add_account.cancel").getString(),
                cx, cancelY + 6, withAlpha(COL_TEXT, a));
    }

    private void renderSpinner(DrawContext ctx, int cx, int cy, int a) {
        int  tick = (int)(System.currentTimeMillis() / 80) % 8;
        int[] dx  = { 0, 4, 6, 4, 0,-4,-6,-4};
        int[] dy  = {-6,-4, 0, 4, 6, 4, 0,-4};
        for (int i = 0; i < 8; i++) {
            int dist = (i - tick + 8) % 8;
            int ia   = (int)(a * (1f - dist / 8f));
            ctx.fill(cx + dx[i] - 1, cy + dy[i] - 1,
                     cx + dx[i] + 1, cy + dy[i] + 1,
                     (ia << 24) | (COL_BORDER_ACC & 0xFFFFFF));
        }
    }

    // ---- Mouse --------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean releaseOnly) {
        if (releaseOnly || click.button() != 0) return super.mouseClicked(click, releaseOnly);
        int mx = (int) click.x(), my = (int) click.y();

        if (mx >= cancelX && mx <= cancelX + cancelW && my >= cancelY && my <= cancelY + cancelH) {
            close(); return true;
        }
        if (state == State.ERROR
                && mx >= retryX && mx <= retryX + retryW
                && my >= retryY && my <= retryY + retryH) {
            startAuthFlow(); return true;
        }
        if (state == State.AWAITING_USER
                && mx >= copyX && mx <= copyX + copyW
                && my >= copyY && my <= copyY + copyH
                && !userCode.isEmpty()) {
            assert client != null;
            client.keyboard.setClipboard(userCode);
            copiedUntil = System.currentTimeMillis() + 1500;
            return true;
        }
        return super.mouseClicked(click, releaseOnly);
    }

    // ---- Helpers ------------------------------------------------------------

    private String mapError(String code) {
        if (code == null) return "Unknown error";
        return switch (code) {
            case "no_network"   -> Text.translatable("mandatory.add_account.error.no_network").getString();
            case "no_license"   -> Text.translatable("mandatory.add_account.error.no_license").getString();
            case "code_expired" -> Text.translatable("mandatory.add_account.error.expired").getString();
            default             -> "Error: " + code;
        };
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16 & 0xFF), ag = (a >> 8 & 0xFF), ab = (a & 0xFF);
        int br = (b >> 16 & 0xFF), bg = (b >> 8 & 0xFF), bb = (b & 0xFF);
        return 0xFF000000
             | ((int)(ar + (br - ar) * t) << 16)
             | ((int)(ag + (bg - ag) * t) << 8)
             |  (int)(ab + (bb - ab) * t);
    }
}
