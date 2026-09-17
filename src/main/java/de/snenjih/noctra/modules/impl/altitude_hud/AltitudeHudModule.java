package de.snenjih.noctra.modules.impl.altitude_hud;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Identifier;

public class AltitudeHudModule extends BaseHudModule {

    private final ModuleSetting<Integer> maxDepth;
    private final ModuleSetting<Integer> warnThreshold;
    private final ModuleSetting<Integer> critThreshold;
    private final ModuleSetting<Boolean> showSeaDiff;
    private final ModuleSetting<Boolean> hideWhenGrounded;
    private final ModuleSetting<Boolean> showBar;
    private final ModuleSetting<Boolean> onlyWhenFlying;
    private final ModuleSetting<Integer> colorSafe;
    private final ModuleSetting<Integer> colorWarn;
    private final ModuleSetting<Integer> colorDanger;

    public AltitudeHudModule() {
        super(
            "altitude_hud",
            "Altitude HUD",
            "Shows your height above the terrain below using a downward block scan.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/altitude_hud")
        );
        maxDepth         = addSetting(new IntSetting    ("max_raycast_depth",  "Max Scan Depth",       256, 16, 512));
        warnThreshold    = addSetting(new IntSetting    ("warn_threshold",     "Warn Below (blocks)",  10,  0, 100));
        critThreshold    = addSetting(new IntSetting    ("crit_threshold",     "Danger Below (blocks)",4,   0,  50));
        showSeaDiff      = addSetting(new BooleanSetting("show_sea_diff",      "Show Sea Level Diff",  false));
        hideWhenGrounded = addSetting(new BooleanSetting("hide_when_grounded", "Hide When Grounded",   false));
        showBar          = addSetting(new BooleanSetting("show_bar",           "Show Bar",             false));
        onlyWhenFlying   = addSetting(new BooleanSetting("only_when_flying",   "Only When Flying",     false));
        beginSection("Colors");
        colorSafe        = addSetting(new ColorSetting  ("color_safe",         "Color Safe",           0xFF55FF55));
        colorWarn        = addSetting(new ColorSetting  ("color_warn",         "Color Warn",           0xFFFFFF55));
        colorDanger      = addSetting(new ColorSetting  ("color_danger",       "Color Danger",         0xFFFF5555));
    }

    @Override public String getHudId()      { return "altitude_hud"; }
    @Override public String getHudName()    { return "Altitude HUD"; }
    @Override public int getDefaultWidth()  { return 120; }
    @Override public int getDefaultHeight() { return 28; }

    private int getAltitude(MinecraftClient mc) {
        int playerY  = (int) Math.floor(mc.player.getY());
        int worldMin = mc.world.getBottomY();
        int depth    = maxDepth.get();

        for (int dy = 1; dy <= depth; dy++) {
            int checkY = playerY - dy;
            if (checkY < worldMin) break;
            BlockPos   pos   = new BlockPos((int) mc.player.getX(), checkY, (int) mc.player.getZ());
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isAir() && state.isSolidBlock(mc.world, pos)) return dy - 1;
        }
        return -1;
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        if (hideWhenGrounded.get() && mc.player.isOnGround()) return;
        if (onlyWhenFlying.get()
                && !mc.player.isGliding()
                && !mc.player.getAbilities().flying) return;

        int altitude = getAltitude(mc);

        int color;
        if (altitude < 0)                           color = 0xFF888888;
        else if (altitude <= critThreshold.get())   color = colorDanger.get();
        else if (altitude <= warnThreshold.get())   color = colorWarn.get();
        else                                        color = colorSafe.get();

        String altStr = altitude < 0 ? "Alt: ?" : "Alt: " + altitude + " ▼";

        var tr = mc.textRenderer;
        int lineY  = y + 4;
        int extraH = (showSeaDiff.get() ? 10 : 0) + (showBar.get() ? 8 : 0);
        int totalH = 18 + extraH;
        int maxW   = Math.max(w, tr.getWidth(altStr) + 8);
        if (showSeaDiff.get()) {
            int seaDiff = (int) mc.player.getY() - 64;
            String seaStr = (seaDiff >= 0 ? "+" : "") + seaDiff + " MSL";
            maxW = Math.max(maxW, tr.getWidth(seaStr) + 8);
        }
        drawBackground(ctx, x, y, maxW, totalH);
        ctx.drawTextWithShadow(tr, altStr, x + 4, lineY, color);
        lineY += 10;

        if (showSeaDiff.get()) {
            int seaDiff = (int) mc.player.getY() - 64;
            String seaStr = (seaDiff >= 0 ? "+" : "") + seaDiff + " MSL";
            ctx.drawTextWithShadow(tr, seaStr, x + 4, lineY, 0xFF8899AA);
            lineY += 10;
        }

        if (showBar.get() && altitude >= 0) {
            int bx   = x + 4;
            int bw   = maxW - 8;
            float ratio = Math.min(1f, altitude / (float) maxDepth.get());
            int fill = (int) (bw * ratio);
            ctx.fill(bx, lineY, bx + bw, lineY + 4, 0xFF333333);
            if (fill > 0) ctx.fill(bx, lineY, bx + fill, lineY + 4, color);
        }
    }
}
