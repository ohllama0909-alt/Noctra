package de.snenjih.noctra.modules.impl.held_item_info;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class HeldItemInfoModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showStackSize;
    private final ModuleSetting<Boolean> showAttackDamage;
    private final ModuleSetting<Boolean> showMiningSpeed;
    private final ModuleSetting<Boolean> showFoodValue;
    private final ModuleSetting<Integer> labelColor;
    private final ModuleSetting<Boolean> showBackground;

    public HeldItemInfoModule() {
        super(
            "held_item_info",
            "Held Item Info",
            "Shows attack damage, stack size and food value of your held item.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/held_item_info")
        );
        showStackSize    = addSetting(new BooleanSetting("show_stack_size",    "Show Stack Size",    true));
        showAttackDamage = addSetting(new BooleanSetting("show_attack_damage", "Show Attack Damage", true));
        showMiningSpeed  = addSetting(new BooleanSetting("show_mining_speed",  "Show Mining Speed",  false));
        showFoodValue    = addSetting(new BooleanSetting("show_food_value",    "Show Food Value",    true));
        beginSection("Colors");
        labelColor       = addSetting(new ColorSetting  ("label_color",        "Label Color",        0xFFAAAAAA));
        showBackground   = addSetting(new BooleanSetting("background",         "Background",         true));
    }

    @Override public String getHudId()      { return "held_item_info"; }
    @Override public String getHudName()    { return "Held Item Info"; }
    @Override public int getDefaultWidth()  { return 130; }
    @Override public int getDefaultHeight() { return 60; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) return;

        var tr = mc.textRenderer;
        int currentY = y + 4;
        int tc = textColor.get();
        int lc = labelColor.get();

        // Collect lines first to measure height
        int lineCount = 1; // item name always shown
        if (showStackSize.get() && stack.getCount() > 1) lineCount++;
        Double attackDmg = getAttackDamage(stack);
        if (showAttackDamage.get() && attackDmg != null) lineCount++;
        Float miningSpd = getMiningSpeed(stack);
        if (showMiningSpeed.get() && miningSpd != null) lineCount++;
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (showFoodValue.get() && food != null) lineCount++;

        int totalH = 4 + lineCount * 10 + 4;

        if (showBackground.get()) {
            drawBackground(ctx, x, y, w, totalH);
        }

        // Item name
        ctx.drawTextWithShadow(tr, stack.getName(), x + 4, currentY, tc);
        currentY += 10;

        // Stack size
        if (showStackSize.get() && stack.getCount() > 1) {
            ctx.drawTextWithShadow(tr, "x" + stack.getCount(), x + 4, currentY, lc);
            currentY += 10;
        }

        // Attack damage
        if (showAttackDamage.get() && attackDmg != null) {
            String dmgStr = String.format("ATK: %.1f", attackDmg);
            ctx.drawTextWithShadow(tr, dmgStr, x + 4, currentY, lc);
            currentY += 10;
        }

        // Mining speed
        if (showMiningSpeed.get() && miningSpd != null) {
            String spdStr = String.format("SPD: %.1f", miningSpd);
            ctx.drawTextWithShadow(tr, spdStr, x + 4, currentY, lc);
            currentY += 10;
        }

        // Food value
        if (showFoodValue.get() && food != null) {
            String foodStr = "Food: +" + food.nutrition();
            ctx.drawTextWithShadow(tr, foodStr, x + 4, currentY, lc);
        }
    }

    private Double getAttackDamage(ItemStack stack) {
        AttributeModifiersComponent attrs = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (attrs == null) return null;
        for (var entry : attrs.modifiers()) {
            if (entry.attribute().equals(EntityAttributes.ATTACK_DAMAGE)) {
                return entry.modifier().value();
            }
        }
        return null;
    }

    private Float getMiningSpeed(ItemStack stack) {
        ToolComponent tool = stack.get(DataComponentTypes.TOOL);
        if (tool == null) return null;
        float speed = tool.defaultMiningSpeed();
        return speed > 1.0f ? speed : null;
    }
}
