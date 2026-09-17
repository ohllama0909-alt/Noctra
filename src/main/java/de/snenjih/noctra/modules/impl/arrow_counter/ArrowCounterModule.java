package de.snenjih.noctra.modules.impl.arrow_counter;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ArrowCounterModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showSpectral;
    private final ModuleSetting<Boolean> showTipped;
    private final ModuleSetting<Boolean> alwaysShow;

    public ArrowCounterModule() {
        super(
            "arrow_counter",
            "Arrow Counter",
            "Shows arrow count on the HUD when holding a bow or crossbow.",
            ModuleCategory.COMBAT,
            Identifier.of("noctra", "modules/arrow_counter")
        );
        showSpectral = addSetting(new BooleanSetting("show_spectral", "Count Spectral Arrows", true));
        showTipped   = addSetting(new BooleanSetting("show_tipped",   "Count Tipped Arrows",   true));
        alwaysShow   = addSetting(new BooleanSetting("always_show",   "Always Show",           false));
    }

    @Override public String getHudId()      { return "arrow_counter"; }
    @Override public String getHudName()    { return "Arrow Counter"; }
    @Override public int getDefaultWidth()  { return 80; }
    @Override public int getDefaultHeight() { return 24; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        ClientPlayerEntity player = mc.player;

        // Check if holding ranged weapon
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand  = player.getOffHandStack();

        boolean hasRangedWeapon =
            mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem
         || offHand.getItem()  instanceof BowItem || offHand.getItem()  instanceof CrossbowItem;

        if (!hasRangedWeapon && !alwaysShow.get()) return;

        // Count arrows in inventory
        int regularArrows  = 0;
        int spectralArrows = 0;
        int tippedArrows   = 0;

        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (item == Items.ARROW) {
                regularArrows += stack.getCount();
            } else if (item == Items.SPECTRAL_ARROW && showSpectral.get()) {
                spectralArrows += stack.getCount();
            } else if (item == Items.TIPPED_ARROW && showTipped.get()) {
                tippedArrows += stack.getCount();
            }
        }

        int totalArrows = regularArrows + spectralArrows + tippedArrows;

        // Color by count
        int color;
        if (totalArrows >= 64) {
            color = 0xFF55FF55; // Green: plenty
        } else if (totalArrows >= 16) {
            color = 0xFFFFFF55; // Yellow: low
        } else {
            color = 0xFFFF5555; // Red: very low / none
        }

        drawBackground(ctx, x, y, w, h);

        String text = totalArrows + " arrows";
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(text).styled(s -> s.withColor(color)),
            x + 4, y + 2, color);

        // Detail line if mixed arrow types
        if (totalArrows > 0 && (spectralArrows > 0 || tippedArrows > 0)) {
            StringBuilder detail = new StringBuilder();
            if (regularArrows > 0)  detail.append(regularArrows).append("N ");
            if (spectralArrows > 0) detail.append(spectralArrows).append("S ");
            if (tippedArrows > 0)   detail.append(tippedArrows).append("T");
            String detailStr = detail.toString().trim();
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(detailStr), x + 4, y + 14, 0xFFAAAAAA);
        }
    }
}
