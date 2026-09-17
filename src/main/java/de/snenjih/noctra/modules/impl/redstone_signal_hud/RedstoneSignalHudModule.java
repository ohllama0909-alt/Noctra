package de.snenjih.noctra.modules.impl.redstone_signal_hud;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Set;

public class RedstoneSignalHudModule extends BaseHudModule {

    private static final Set<Block> REDSTONE_BLOCKS = Set.of(
        Blocks.REDSTONE_WIRE,
        Blocks.REDSTONE_TORCH,
        Blocks.REDSTONE_WALL_TORCH,
        Blocks.REPEATER,
        Blocks.COMPARATOR,
        Blocks.LEVER,
        Blocks.STONE_BUTTON,
        Blocks.OAK_BUTTON,
        Blocks.SPRUCE_BUTTON,
        Blocks.BIRCH_BUTTON,
        Blocks.JUNGLE_BUTTON,
        Blocks.ACACIA_BUTTON,
        Blocks.DARK_OAK_BUTTON,
        Blocks.MANGROVE_BUTTON,
        Blocks.CHERRY_BUTTON,
        Blocks.BAMBOO_BUTTON,
        Blocks.CRIMSON_BUTTON,
        Blocks.WARPED_BUTTON,
        Blocks.POLISHED_BLACKSTONE_BUTTON,
        Blocks.DAYLIGHT_DETECTOR,
        Blocks.OBSERVER,
        Blocks.TARGET,
        Blocks.REDSTONE_BLOCK,
        Blocks.SCULK_SENSOR,
        Blocks.CALIBRATED_SCULK_SENSOR,
        Blocks.TRAPPED_CHEST
    );

    private final ModuleSetting<Boolean> onlyWhenTargeting;
    private final ModuleSetting<Boolean> showBar;
    private final ModuleSetting<Boolean> showBlockName;
    private final ModuleSetting<Boolean> showAllSides;
    private final ModuleSetting<Boolean> alwaysShowValue;
    private final ModuleSetting<Integer> colorZero;
    private final ModuleSetting<Integer> colorLow;
    private final ModuleSetting<Integer> colorHigh;

    public RedstoneSignalHudModule() {
        super(
            "redstone_signal_hud",
            "Redstone Signal",
            "Shows the redstone signal strength of the targeted block.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/redstone_signal_hud")
        );
        onlyWhenTargeting = addSetting(new BooleanSetting("only_when_targeting", "Only When Targeting", true));
        showBar           = addSetting(new BooleanSetting("show_bar",            "Show Bar",            true));
        showBlockName     = addSetting(new BooleanSetting("show_block_name",     "Show Block Name",     true));
        showAllSides      = addSetting(new BooleanSetting("show_all_sides",      "Show All Sides",      false));
        alwaysShowValue   = addSetting(new BooleanSetting("always_show_value",   "Always Show",         false));
        beginSection("Colors");
        colorZero = addSetting(new ColorSetting("color_zero", "Color Zero",        0xFF666666));
        colorLow  = addSetting(new ColorSetting("color_low",  "Color Low (1-7)",   0xFFFF5555));
        colorHigh = addSetting(new ColorSetting("color_high", "Color High (8-15)", 0xFFFF2222));
    }

    @Override public String getHudId()       { return "redstone_signal_hud"; }
    @Override public String getHudName()     { return "Redstone Signal"; }
    @Override public int getDefaultWidth()   { return 130; }
    @Override public int getDefaultHeight()  { return 40; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        HitResult hitResult = mc.crosshairTarget;
        if (hitResult == null || !(hitResult instanceof BlockHitResult blockHit)) return;

        BlockPos pos = blockHit.getBlockPos();
        World world  = mc.world;
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return;
        Block block = state.getBlock();

        boolean isRedstone = REDSTONE_BLOCKS.contains(block)
                          || block instanceof AbstractRedstoneGateBlock;

        if (onlyWhenTargeting.get() && !isRedstone) return;

        int power = world.getEmittedRedstonePower(pos, blockHit.getSide());

        if (!alwaysShowValue.get() && power == 0 && !isRedstone) return;

        int color;
        if (power == 0)      color = colorZero.get();
        else if (power <= 7) color = colorLow.get();
        else                 color = colorHigh.get();

        var tr = mc.textRenderer;
        int lineY = y + 4;

        // Calculate needed width
        String signalStr = "Signal: " + power + "/15";
        int neededW = Math.max(w, tr.getWidth(signalStr) + 8);
        if (showBlockName.get()) {
            String blockName = block.getName().getString();
            neededW = Math.max(neededW, tr.getWidth(blockName) + 8);
        }

        drawBackground(ctx, x, y, neededW, h);

        if (showBlockName.get()) {
            String blockName = block.getName().getString();
            ctx.drawTextWithShadow(tr, blockName, x + 4, lineY, 0xFFAAAAAA);
            lineY += 10;
        }

        ctx.drawTextWithShadow(tr, signalStr, x + 4, lineY, color);
        lineY += 10;

        if (showBar.get()) {
            int barX  = x + 4;
            int barW  = neededW - 8;
            int barH  = 4;
            int fill  = (int) (barW * (power / 15.0f));
            ctx.fill(barX, lineY, barX + barW, lineY + barH, 0xFF333333);
            if (fill > 0) ctx.fill(barX, lineY, barX + fill, lineY + barH, color);
            lineY += 8;
        }

        if (showAllSides.get()) {
            for (Direction dir : Direction.values()) {
                int sidePower = world.getEmittedRedstonePower(pos, dir);
                if (sidePower > 0) {
                    ctx.drawTextWithShadow(tr, dir.name().toLowerCase() + ": " + sidePower, x + 4, lineY, color);
                    lineY += 9;
                }
            }
        }
    }
}
