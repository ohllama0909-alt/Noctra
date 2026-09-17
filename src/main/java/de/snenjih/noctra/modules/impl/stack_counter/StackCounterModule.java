package de.snenjih.noctra.modules.impl.stack_counter;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class StackCounterModule extends BaseHudModule {

    private final ModuleSetting<Boolean> includeOffhand;
    private final ModuleSetting<Boolean> showItemName;
    private final ModuleSetting<Boolean> showStacks;
    private final ModuleSetting<Boolean> hideWhenFullStack;
    private final ModuleSetting<Boolean> hideOnEmpty;
    private final ModuleSetting<Boolean> countNbtStacks;
    private final ModuleSetting<Integer> warnThreshold;
    private final ModuleSetting<Integer> colorWarn;

    public StackCounterModule() {
        super(
            "stack_counter",
            "Stack Counter",
            "Shows the total count of your held item type across all inventory slots.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/stack_counter")
        );
        includeOffhand    = addSetting(new BooleanSetting("include_offhand",     "Include Offhand",    true));
        showItemName      = addSetting(new BooleanSetting("show_item_name",      "Show Item Name",     true));
        showStacks        = addSetting(new BooleanSetting("show_stacks",         "Show Stack Count",   false));
        hideWhenFullStack = addSetting(new BooleanSetting("hide_when_full_stack","Hide If Single Stack",false));
        hideOnEmpty       = addSetting(new BooleanSetting("hide_on_empty",       "Hide When Empty",    true));
        countNbtStacks    = addSetting(new BooleanSetting("count_nbt_stacks",    "Count by Item Type", false));
        beginSection("Warn");
        warnThreshold     = addSetting(new IntSetting    ("warn_threshold",      "Warn Below",         0, 0, 2304));
        beginSection("Colors");
        colorWarn         = addSetting(new ColorSetting  ("color_warn",          "Warn Color",         0xFFFF5555));
    }

    @Override public String getHudId()      { return "stack_counter"; }
    @Override public String getHudName()    { return "Stack Counter"; }
    @Override public int getDefaultWidth()  { return 120; }
    @Override public int getDefaultHeight() { return 18; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) {
            if (hideOnEmpty.get()) return;
        }

        var inv   = mc.player.getInventory();
        int total = 0;
        int stackCount = 0;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack s = inv.getStack(slot);
            if (s.isEmpty()) continue;
            boolean match = countNbtStacks.get()
                ? ItemStack.areItemsAndComponentsEqual(held, s)
                : s.getItem() == held.getItem();
            if (match) { total += s.getCount(); stackCount++; }
        }
        if (includeOffhand.get()) {
            ItemStack off = inv.getStack(40);
            if (!off.isEmpty()) {
                boolean match = countNbtStacks.get()
                    ? ItemStack.areItemsAndComponentsEqual(held, off)
                    : off.getItem() == held.getItem();
                if (match) { total += off.getCount(); stackCount++; }
            }
        }

        if (hideWhenFullStack.get() && stackCount <= 1) return;

        int color = (warnThreshold.get() > 0 && total <= warnThreshold.get())
            ? colorWarn.get()
            : textColor.get();

        String countStr;
        if (showStacks.get() && !held.isEmpty()) {
            int maxStack = held.getMaxCount();
            if (maxStack > 1) {
                int fullStacks = total / maxStack;
                int rem        = total % maxStack;
                countStr = fullStacks > 0
                    ? (rem > 0 ? fullStacks + "×" + maxStack + "+" + rem : fullStacks + "×" + maxStack)
                    : String.valueOf(rem);
            } else {
                countStr = String.valueOf(total);
            }
        } else {
            countStr = String.valueOf(total);
        }

        String line = (showItemName.get() && !held.isEmpty())
            ? held.getName().getString() + ": " + countStr
            : "Total: " + countStr;

        var tr = mc.textRenderer;
        int lineW = Math.max(w, tr.getWidth(line) + 8);
        drawBackground(ctx, x, y, lineW, 18);
        ctx.drawTextWithShadow(tr, line, x + 4, y + 5, color);
    }
}
