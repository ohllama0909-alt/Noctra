package de.snenjih.noctra.modules.impl.item_info_hud;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ItemInfoHudModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showName;
    private final ModuleSetting<Boolean> showDurability;
    private final ModuleSetting<Boolean> showEnchantments;
    private final ModuleSetting<Boolean> alwaysShow;
    private final ModuleSetting<Integer> displaySeconds;
    private final ModuleSetting<Integer> barColorFull;
    private final ModuleSetting<Integer> barColorLow;

    private int lastSelectedSlot = -1;
    private int displayTimer     = 0;

    public ItemInfoHudModule() {
        super(
            "item_info_hud",
            "Item Info HUD",
            "Shows held item name, durability, and enchantments as a HUD overlay.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/item_info_hud")
        );
        showName         = addSetting(new BooleanSetting("show_name",         "Show Name",            true));
        showDurability   = addSetting(new BooleanSetting("show_durability",   "Show Durability",      true));
        showEnchantments = addSetting(new BooleanSetting("show_enchantments", "Show Enchantments",    true));
        alwaysShow       = addSetting(new BooleanSetting("always_show",       "Always Show",          false));
        displaySeconds   = addSetting(new IntSetting    ("display_seconds",   "Display Duration (s)", 3, 1, 10));
        beginSection("Colors");
        barColorFull     = addSetting(new ColorSetting  ("bar_color_full",    "Bar Color Full",       0xFF55FF55));
        barColorLow      = addSetting(new ColorSetting  ("bar_color_low",     "Bar Color Low",        0xFFFF5555));
    }

    @Override public String getHudId()      { return "item_info_hud"; }
    @Override public String getHudName()    { return "Item Info HUD"; }
    @Override public int getDefaultWidth()  { return 140; }
    @Override public int getDefaultHeight() { return 70; }

    @Override
    public void onClientTick(MinecraftClient mc) {
        if (mc.player == null) return;
        int slot = mc.player.getInventory().getSelectedSlot();
        if (slot != lastSelectedSlot) {
            lastSelectedSlot = slot;
            displayTimer = displaySeconds.get() * 20;
        } else if (displayTimer > 0) {
            displayTimer--;
        }
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) return;
        if (!alwaysShow.get() && displayTimer <= 0) return;

        drawBackground(ctx, x, y, w, h);

        int currentY = y + 2;
        int tc       = textColor.get();

        if (showName.get()) {
            ctx.drawTextWithShadow(mc.textRenderer, stack.getName(), x + 2, currentY, tc);
            currentY += 11;
        }

        if (showDurability.get() && stack.isDamageable()) {
            int   maxDmg    = stack.getMaxDamage();
            int   remaining = maxDmg - stack.getDamage();
            float fraction  = (float) remaining / maxDmg;
            int   barColor  = fraction > 0.5f ? barColorFull.get() : barColorLow.get();

            int barW = w - 4;
            ctx.fill(x + 2, currentY, x + 2 + barW, currentY + 5, 0xFF222222);
            ctx.fill(x + 2, currentY, x + 2 + Math.round(fraction * barW), currentY + 5, barColor);
            currentY += 8;

            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(remaining + " / " + maxDmg), x + 2, currentY, barColor);
            currentY += 11;
        }

        if (showEnchantments.get()) {
            ItemEnchantmentsComponent enchants = stack.getEnchantments();
            if (!enchants.isEmpty()) {
                for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
                    int  level = enchants.getLevel(entry);
                    Text name  = Enchantment.getName(entry, level);
                    ctx.drawTextWithShadow(mc.textRenderer, name, x + 2, currentY, 0xFFAAAAAA);
                    currentY += 10;
                }
            }
        }
    }
}
