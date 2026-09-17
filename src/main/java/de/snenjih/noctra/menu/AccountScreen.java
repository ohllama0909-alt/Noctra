package de.snenjih.noctra.menu;

import de.snenjih.noctra.auth.AccountEntry;
import de.snenjih.noctra.auth.AccountManager;
import de.snenjih.noctra.auth.SkinCache;
import de.snenjih.noctra.hud.NotificationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;

public class AccountScreen extends Screen {

    // Colors
    private static final int COL_BORDER      = 0xFF1E3A5F;
    private static final int COL_BORDER_ACC  = 0xFF4A7CF8;
    private static final int COL_TEXT        = 0xFFFFFFFF;
    private static final int COL_SUBTEXT     = 0xFF8899AA;
    private static final int COL_BTN_DEF     = 0xFF1A2A40;
    private static final int COL_BTN_HOV     = 0xFF243350;
    private static final int COL_CARD_BG     = 0xFF0D1B2A;
    private static final int COL_ACTIVE_CHIP = 0xFF4A7CF8;
    private static final int COL_DEL         = 0xFFFF5555;
    private static final int COL_DEL_BG      = 0xFF3D1A1A;
    private static final int COL_ADD         = 0xFF44FF88;
    private static final int COL_WARN        = 0xFFFFAA44;

    private static final int CARD_H    = 56;
    private static final int CARD_GAP  = 6;
    private static final int MARGIN    = 20;
    private static final int HEAD_SIZE = 32;
    private static final int HEAD_PAD  = 12;

    private final Screen parent;

    private List<AccountEntry> accounts;
    private AccountEntry       active;

    // Animation
    private float   openProgress = 0f;
    private float[] cardHover;
    private float[] switchBtnH;
    private float   addBtnHover  = 0f;
    private float   backBtnHover = 0f;

    // Switching state
    private UUID    switchingUuid = null;
    private String  switchError   = null;
    private long    switchErrUntil;

    // Confirm-delete state
    private UUID confirmDeleteUuid  = null;
    private long confirmDeleteUntil = 0;
    private float confirmYesH = 0f;
    private float confirmNoH  = 0f;

    // Scroll
    private int scrollOffset = 0;

    // Back button
    private int backX, backY, backW = 60, backH = 18;

    // Add button
    private int addX, addY, addW = 140, addH = 22;

    public AccountScreen(Screen parent) {
        super(Text.literal(""));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshAccounts();
    }

    private void refreshAccounts() {
        accounts = AccountManager.getInstance().getAll();
        active   = AccountManager.getInstance().getActive();
        int n    = accounts.size();
        if (cardHover  == null || cardHover.length  != n) cardHover  = new float[n];
        if (switchBtnH == null || switchBtnH.length != n) switchBtnH = new float[n];
    }

    @Override
    public boolean shouldPause() { return true; }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // ---- Render -------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        openProgress = Math.min(1f, openProgress + 0.07f);
        int a = (int)(0xFF * openProgress);

        // Background overlay
        ctx.fill(0, 0, width, height, (int)(0xCC * openProgress) << 24 | 0x050E18);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("noctra.accounts.title").getString(),
                width / 2, 14, withAlpha(COL_TEXT, a));

        // Back button (top-left)
        backX = 8; backY = 8;
        boolean backHov = mouseX >= backX && mouseX <= backX + backW && mouseY >= backY && mouseY <= backY + backH;
        backBtnHover += (backHov ? 1f : -1f) * 0.18f;
        backBtnHover  = Math.max(0f, Math.min(1f, backBtnHover));
        ctx.fill(backX, backY, backX + backW, backY + backH,
                withAlpha(lerpColor(COL_BTN_DEF, COL_BTN_HOV, backBtnHover), a));
        ctx.drawStrokedRectangle(backX, backY, backW, backH,
                withAlpha(lerpColor(COL_BORDER, COL_BORDER_ACC, backBtnHover), a));
        ctx.drawCenteredTextWithShadow(textRenderer, "← Back",
                backX + backW / 2, backY + 5, withAlpha(COL_TEXT, a));

        // Refresh account list every frame (so switches/removals reflect immediately)
        refreshAccounts();

        if (accounts.isEmpty()) {
            renderEmptyState(ctx, a);
        } else {
            renderAccountList(ctx, mouseX, mouseY, a);
        }

        // "Add Account" button (bottom-center)
        addW = 140; addH = 22;
        addX = width / 2 - addW / 2;
        addY = height - 36;
        boolean addHov = mouseX >= addX && mouseX <= addX + addW && mouseY >= addY && mouseY <= addY + addH;
        addBtnHover += (addHov ? 1f : -1f) * 0.18f;
        addBtnHover  = Math.max(0f, Math.min(1f, addBtnHover));
        ctx.fill(addX, addY, addX + addW, addY + addH,
                withAlpha(lerpColor(COL_BTN_DEF, COL_BTN_HOV, addBtnHover), a));
        ctx.drawStrokedRectangle(addX, addY, addW, addH,
                withAlpha(lerpColor(COL_BORDER, COL_BORDER_ACC, addBtnHover), a));
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("noctra.accounts.add").getString(),
                addX + addW / 2, addY + 7, withAlpha(COL_ADD, a));

        // Switch error toast (inline, above add button)
        if (switchError != null && System.currentTimeMillis() < switchErrUntil) {
            ctx.drawCenteredTextWithShadow(textRenderer, switchError,
                    width / 2, addY - 14, withAlpha(COL_WARN, a));
        } else if (System.currentTimeMillis() >= switchErrUntil) {
            switchError = null;
        }

        // Auto-reset confirm-delete after 4 s of inactivity
        if (confirmDeleteUuid != null && System.currentTimeMillis() > confirmDeleteUntil) {
            confirmDeleteUuid = null;
            confirmYesH = 0f;
            confirmNoH  = 0f;
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderEmptyState(DrawContext ctx, int a) {
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("noctra.accounts.no_accounts").getString(),
                width / 2, height / 2 - 10, withAlpha(COL_SUBTEXT, a));
    }

    private void renderAccountList(DrawContext ctx, int mx, int my, int a) {
        int listX = MARGIN;
        int listW = width - MARGIN * 2;
        int startY = 40 - scrollOffset;
        int n      = accounts.size();
        if (n != cardHover.length) { cardHover = new float[n]; switchBtnH = new float[n]; }

        // Scissor the list area
        ctx.enableScissor(0, 38, width, height - 40);

        for (int i = 0; i < n; i++) {
            AccountEntry entry = accounts.get(i);
            int cardY = startY + i * (CARD_H + CARD_GAP);
            boolean isActive    = active != null && entry.uuid().equals(active.uuid());
            boolean isSwitching = entry.uuid().equals(switchingUuid);
            boolean isConfirm   = entry.uuid().equals(confirmDeleteUuid);

            // Hover animation
            boolean hovered = mx >= listX && mx <= listX + listW
                           && my >= cardY  && my <= cardY + CARD_H;
            cardHover[i] += ((hovered && !isConfirm) ? 1f : -1f) * 0.15f;
            cardHover[i]  = Math.max(0f, Math.min(1f, cardHover[i]));

            // Card background
            int cardBg = lerpColor(COL_CARD_BG, 0xFF1A2A40, cardHover[i]);
            int border  = isActive ? COL_BORDER_ACC : lerpColor(COL_BORDER, COL_BORDER_ACC, cardHover[i] * 0.4f);
            ctx.fill(listX, cardY, listX + listW, cardY + CARD_H, withAlpha(cardBg, a));
            ctx.drawStrokedRectangle(listX, cardY, listW, CARD_H, withAlpha(border, a));

            if (isConfirm) {
                renderConfirmDelete(ctx, entry, listX, cardY, listW, mx, my, a);
                continue;
            }

            // Skin head
            int headX = listX + HEAD_PAD;
            int headY = cardY + (CARD_H - HEAD_SIZE) / 2;
            SkinCache cache = SkinCache.getInstance();
            if (cache != null && cache.isLoaded(entry.uuid())) {
                Identifier skinId = cache.getTextureId(entry.uuid());
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, skinId,
                        headX, headY, 8f, 8f, HEAD_SIZE, HEAD_SIZE, 8, 8, 64, 64);
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, skinId,
                        headX, headY, 40f, 8f, HEAD_SIZE, HEAD_SIZE, 8, 8, 64, 64);
            } else {
                ctx.fill(headX, headY, headX + HEAD_SIZE, headY + HEAD_SIZE, withAlpha(0x334455, a));
                ctx.drawCenteredTextWithShadow(textRenderer, "?",
                        headX + HEAD_SIZE / 2, headY + 12, withAlpha(COL_SUBTEXT, a));
            }

            // Username
            int textX = headX + HEAD_SIZE + 8;
            int textY = cardY + (CARD_H / 2) - 10;
            ctx.drawTextWithShadow(textRenderer, entry.username(),
                    textX, textY, withAlpha(COL_TEXT, a));

            // UUID (small, gray)
            String uuidShort = entry.uuid().toString().substring(0, 8) + "…";
            ctx.drawTextWithShadow(textRenderer, uuidShort,
                    textX, textY + 12, withAlpha(COL_SUBTEXT, a));

            // Active chip
            if (isActive) {
                ctx.drawTextWithShadow(textRenderer,
                        Text.translatable("noctra.accounts.active").getString(),
                        textX, textY + 24, withAlpha(COL_ACTIVE_CHIP, a));
            }

            // Switch / Switching button (right side, non-active only)
            int btnRightEdge = listX + listW - 8;
            int btnY = cardY + (CARD_H - 18) / 2;

            if (!isActive) {
                boolean inGame = MinecraftClient.getInstance().world != null;
                String  btnLbl = isSwitching ? "..." : Text.translatable("noctra.accounts.switch").getString();
                int     btnW   = 60, btnH = 18;
                int     btnX   = btnRightEdge - btnW;

                boolean hovSwitch = !isSwitching && mx >= btnX && mx <= btnX + btnW
                                 && my >= btnY && my <= btnY + btnH;
                switchBtnH[i] += (hovSwitch ? 1f : -1f) * 0.18f;
                switchBtnH[i]  = Math.max(0f, Math.min(1f, switchBtnH[i]));

                int swBg = inGame ? 0xFF1A1A2A
                         : lerpColor(COL_BTN_DEF, COL_BTN_HOV, switchBtnH[i]);
                ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, withAlpha(swBg, a));
                ctx.drawStrokedRectangle(btnX, btnY, btnW, btnH,
                        withAlpha(inGame ? COL_BORDER : lerpColor(COL_BORDER, COL_BORDER_ACC, switchBtnH[i]), a));
                ctx.drawCenteredTextWithShadow(textRenderer, btnLbl,
                        btnX + btnW / 2, btnY + 5,
                        withAlpha(inGame ? COL_SUBTEXT : COL_TEXT, a));

                btnRightEdge = btnX - 6;
            }

            // Delete button
            int delW = 18, delH = 18;
            int delX = btnRightEdge - delW;
            int delY = cardY + (CARD_H - delH) / 2;
            boolean only   = accounts.size() == 1;
            boolean hovDel = !only && mx >= delX && mx <= delX + delW && my >= delY && my <= delY + delH;

            ctx.fill(delX, delY, delX + delW, delY + delH,
                    withAlpha(hovDel ? COL_DEL_BG : COL_BTN_DEF, a));
            ctx.drawStrokedRectangle(delX, delY, delW, delH,
                    withAlpha(hovDel ? COL_DEL : COL_BORDER, a));
            ctx.drawCenteredTextWithShadow(textRenderer, "✕",
                    delX + delW / 2, delY + 5, withAlpha(only ? COL_SUBTEXT : (hovDel ? COL_DEL : COL_TEXT), a));
        }

        ctx.disableScissor();
    }

    private void renderConfirmDelete(DrawContext ctx, AccountEntry entry,
                                     int cardX, int cardY, int cardW, int mx, int my, int a) {
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("noctra.accounts.delete_confirm").getString(),
                cardX + cardW / 2, cardY + 8, withAlpha(COL_WARN, a));

        // Yes button
        int btnW = 80, btnH = 18;
        int yesX = cardX + cardW / 2 - btnW - 4;
        int yesY = cardY + CARD_H - 26;
        boolean hovY = mx >= yesX && mx <= yesX + btnW && my >= yesY && my <= yesY + btnH;
        confirmYesH += (hovY ? 1f : -1f) * 0.18f;
        confirmYesH  = Math.max(0f, Math.min(1f, confirmYesH));
        ctx.fill(yesX, yesY, yesX + btnW, yesY + btnH, withAlpha(lerpColor(COL_DEL_BG, 0xFF5A2A2A, confirmYesH), a));
        ctx.drawStrokedRectangle(yesX, yesY, btnW, btnH, withAlpha(lerpColor(COL_BORDER, COL_DEL, confirmYesH), a));
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("noctra.accounts.delete_yes").getString(),
                yesX + btnW / 2, yesY + 5, withAlpha(COL_DEL, a));

        // No button
        int noX = cardX + cardW / 2 + 4;
        int noY = yesY;
        boolean hovN = mx >= noX && mx <= noX + btnW && my >= noY && my <= noY + btnH;
        confirmNoH += (hovN ? 1f : -1f) * 0.18f;
        confirmNoH  = Math.max(0f, Math.min(1f, confirmNoH));
        ctx.fill(noX, noY, noX + btnW, noY + btnH, withAlpha(lerpColor(COL_BTN_DEF, COL_BTN_HOV, confirmNoH), a));
        ctx.drawStrokedRectangle(noX, noY, btnW, btnH, withAlpha(lerpColor(COL_BORDER, COL_BORDER_ACC, confirmNoH), a));
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("noctra.accounts.delete_no").getString(),
                noX + btnW / 2, noY + 5, withAlpha(COL_TEXT, a));
    }

    // ---- Mouse events -------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean releaseOnly) {
        if (releaseOnly || click.button() != 0) return super.mouseClicked(click, releaseOnly);
        int mx = (int) click.x(), my = (int) click.y();

        // Back
        if (mx >= backX && mx <= backX + backW && my >= backY && my <= backY + backH) {
            close(); return true;
        }
        // Add account
        if (mx >= addX && mx <= addX + addW && my >= addY && my <= addY + addH) {
            assert client != null;
            client.setScreen(new AddAccountScreen(this));
            return true;
        }

        // Card hits
        int listX  = MARGIN;
        int listW  = width - MARGIN * 2;
        int startY = 40 - scrollOffset;
        refreshAccounts();
        int n = accounts.size();

        for (int i = 0; i < n; i++) {
            AccountEntry entry = accounts.get(i);
            int cardY = startY + i * (CARD_H + CARD_GAP);
            if (my < cardY || my > cardY + CARD_H) continue;
            if (mx < listX || mx > listX + listW) continue;

            boolean isActive  = active != null && entry.uuid().equals(active.uuid());
            boolean isConfirm = entry.uuid().equals(confirmDeleteUuid);

            if (isConfirm) {
                handleConfirmDeleteClick(entry, listX, cardY, listW, mx, my);
                return true;
            }

            // Locate buttons (mirror of render logic)
            int btnRightEdge = listX + listW - 8;
            int btnY = cardY + (CARD_H - 18) / 2;

            if (!isActive) {
                int btnW = 60, btnH = 18;
                int btnX = btnRightEdge - btnW;
                if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                    if (MinecraftClient.getInstance().world != null) {
                        switchError = Text.translatable("noctra.accounts.in_game_warning").getString();
                        switchErrUntil = System.currentTimeMillis() + 3000;
                    } else {
                        doSwitch(entry);
                    }
                    return true;
                }
                btnRightEdge = btnX - 6;
            }

            // Delete button
            int delW = 18, delH = 18;
            int delX = btnRightEdge - delW;
            int delY = cardY + (CARD_H - delH) / 2;
            if (accounts.size() > 1 && mx >= delX && mx <= delX + delW && my >= delY && my <= delY + delH) {
                confirmDeleteUuid  = entry.uuid();
                confirmDeleteUntil = System.currentTimeMillis() + 4000;
                confirmYesH = 0f;
                confirmNoH  = 0f;
                return true;
            }
        }

        return super.mouseClicked(click, releaseOnly);
    }

    private void handleConfirmDeleteClick(AccountEntry entry, int cardX, int cardY, int cardW, int mx, int my) {
        int btnW = 80, btnH = 18;
        int yesX = cardX + cardW / 2 - btnW - 4;
        int yesY = cardY + CARD_H - 26;
        int noX  = cardX + cardW / 2 + 4;

        if (mx >= yesX && mx <= yesX + btnW && my >= yesY && my <= yesY + btnH) {
            AccountManager.getInstance().removeAccount(entry.uuid());
            confirmDeleteUuid = null;
            refreshAccounts();
        } else if (mx >= noX && mx <= noX + btnW && my >= yesY && my <= yesY + btnH) {
            confirmDeleteUuid = null;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalH = accounts.size() * (CARD_H + CARD_GAP);
        int visible = height - 80;
        int maxScroll = Math.max(0, totalH - visible);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(verticalAmount * 16)));
        return true;
    }

    // ---- Switch logic -------------------------------------------------------

    private void doSwitch(AccountEntry entry) {
        switchingUuid = entry.uuid();
        switchError   = null;

        AccountManager.getInstance().switchToAsync(
            entry,
            () -> {
                // Success — refresh and close or stay
                switchingUuid = null;
                refreshAccounts();
            },
            errMsg -> {
                switchingUuid  = null;
                switchError    = resolveError(errMsg);
                switchErrUntil = System.currentTimeMillis() + 4000;
                refreshAccounts();
            }
        );
    }

    private String resolveError(String code) {
        if (code == null) return "Switch failed";
        if (code.startsWith("no_network"))   return Text.translatable("noctra.add_account.error.no_network").getString();
        if (code.startsWith("session"))      return Text.translatable("noctra.add_account.error.refresh_failed").getString();
        return "Error: " + code;
    }

    // ---- Helpers ------------------------------------------------------------

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
