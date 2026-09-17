package de.snenjih.noctra.modules.impl.reach_display;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ReachDisplayModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showAttack;
    private final ModuleSetting<Boolean> showInteract;
    private final ModuleSetting<Integer> decimalPlaces;
    private final ModuleSetting<Integer> reachTextColor;

    public ReachDisplayModule() {
        super(
            "reach_display",
            "Reach Display",
            "Shows your current attack and interaction reach on the HUD.",
            ModuleCategory.COMBAT,
            Identifier.of("noctra", "modules/reach_display")
        );
        showAttack    = addSetting(new BooleanSetting("show_attack",    "Show Attack Reach",   true));
        showInteract  = addSetting(new BooleanSetting("show_interact",  "Show Interact Reach", true));
        decimalPlaces = addSetting(new IntSetting    ("decimal_places", "Decimal Places",      2, 0, 3));
        reachTextColor = addSetting(new ColorSetting ("text_color",     "Text Color",          0xFFFFFFFF));
    }

    @Override public String getHudId()      { return "reach_display"; }
    @Override public String getHudName()    { return "Reach Display"; }
    @Override public int getDefaultWidth()  { return 110; }
    @Override public int getDefaultHeight() { return 24; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        if (mc.options.hudHidden) return;

        drawBackground(ctx, x, y, w, h);

        String format = "%." + decimalPlaces.get() + "f";
        int lineY = y + 2;
        int color = reachTextColor.get();

        if (showAttack.get()) {
            double attackReach = getAttackReach(player);
            String label = "Attack: " + String.format(format, attackReach) + " b";
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(label), x + 4, lineY, color);
            lineY += 12;
        }

        if (showInteract.get()) {
            double blockReach = getBlockReach(player);
            String label = "Reach: " + String.format(format, blockReach) + " b";
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(label), x + 4, lineY, color);
        }
    }

    private double getAttackReach(ClientPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
        if (attr != null) {
            return attr.getValue();
        }
        return player.isCreative() ? 5.0 : 3.0;
    }

    private double getBlockReach(ClientPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
        if (attr != null) {
            return attr.getValue();
        }
        return player.isCreative() ? 5.0 : 4.5;
    }
}
