package de.snenjih.noctra.modules.impl.item_counter;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import de.snenjih.noctra.modules.api.settings.TextSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ItemCounterModule extends BaseHudModule {

    private record TrackedItem(String label, int count, boolean warning) {}

    private final ModuleSetting<String>  item1,  item2,  item3,  item4,  item5,  item6;
    private final ModuleSetting<String>  label1, label2, label3, label4, label5, label6;
    private final ModuleSetting<Integer> warn1,  warn2,  warn3,  warn4,  warn5,  warn6;
    private final ModuleSetting<Boolean> includeOffhand;
    private final ModuleSetting<Boolean> includeArmor;
    private final ModuleSetting<Boolean> hideEmpty;
    private final ModuleSetting<Boolean> showIcons;
    private final ModuleSetting<Integer> colorWarn;
    private final ModuleSetting<Integer> colorOk;

    public ItemCounterModule() {
        super(
            "item_counter",
            "Item Counter",
            "Tracks and displays counts of up to 6 custom items in your inventory.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/item_counter")
        );
        beginSection("Slot 1");
        item1  = addSetting(new TextSetting   ("item_1",  "Item 1 ID",    "minecraft:totem_of_undying", 64));
        label1 = addSetting(new TextSetting   ("label_1", "Item 1 Label", "Totem",                       20));
        warn1  = addSetting(new IntSetting    ("warn_1",  "Item 1 Warn ≤",1, 0, 64));
        beginSection("Slot 2");
        item2  = addSetting(new TextSetting   ("item_2",  "Item 2 ID",    "",  64));
        label2 = addSetting(new TextSetting   ("label_2", "Item 2 Label", "",  20));
        warn2  = addSetting(new IntSetting    ("warn_2",  "Item 2 Warn ≤",0, 0, 64));
        beginSection("Slot 3");
        item3  = addSetting(new TextSetting   ("item_3",  "Item 3 ID",    "",  64));
        label3 = addSetting(new TextSetting   ("label_3", "Item 3 Label", "",  20));
        warn3  = addSetting(new IntSetting    ("warn_3",  "Item 3 Warn ≤",0, 0, 64));
        beginSection("Slot 4");
        item4  = addSetting(new TextSetting   ("item_4",  "Item 4 ID",    "",  64));
        label4 = addSetting(new TextSetting   ("label_4", "Item 4 Label", "",  20));
        warn4  = addSetting(new IntSetting    ("warn_4",  "Item 4 Warn ≤",0, 0, 64));
        beginSection("Slot 5");
        item5  = addSetting(new TextSetting   ("item_5",  "Item 5 ID",    "",  64));
        label5 = addSetting(new TextSetting   ("label_5", "Item 5 Label", "",  20));
        warn5  = addSetting(new IntSetting    ("warn_5",  "Item 5 Warn ≤",0, 0, 64));
        beginSection("Slot 6");
        item6  = addSetting(new TextSetting   ("item_6",  "Item 6 ID",    "",  64));
        label6 = addSetting(new TextSetting   ("label_6", "Item 6 Label", "",  20));
        warn6  = addSetting(new IntSetting    ("warn_6",  "Item 6 Warn ≤",0, 0, 64));
        beginSection("Options");
        includeOffhand = addSetting(new BooleanSetting("include_offhand", "Include Offhand", true));
        includeArmor   = addSetting(new BooleanSetting("include_armor",   "Include Armor",   false));
        hideEmpty      = addSetting(new BooleanSetting("hide_empty",      "Hide Empty Slots",true));
        showIcons      = addSetting(new BooleanSetting("show_icons",      "Show Item Icons", false));
        beginSection("Colors");
        colorWarn = addSetting(new ColorSetting("color_warn", "Warn Color", 0xFFFF5555));
        colorOk   = addSetting(new ColorSetting("color_ok",   "OK Color",   0xFF55FF55));
    }

    @Override public String getHudId()      { return "item_counter"; }
    @Override public String getHudName()    { return "Item Counter"; }
    @Override public int getDefaultWidth()  { return 160; }
    @Override public int getDefaultHeight() { return 80; }

    private int countItem(net.minecraft.entity.player.PlayerInventory inv, Item item) {
        int total = 0;
        // Main inventory + hotbar: slots 0–35
        for (int slot = 0; slot < 36; slot++) {
            ItemStack s = inv.getStack(slot);
            if (s.getItem() == item) total += s.getCount();
        }
        // Offhand: slot 40
        if (includeOffhand.get()) {
            ItemStack s = inv.getStack(40);
            if (s.getItem() == item) total += s.getCount();
        }
        // Armor: slots 36–39
        if (includeArmor.get()) {
            for (int slot = 36; slot <= 39; slot++) {
                ItemStack s = inv.getStack(slot);
                if (s.getItem() == item) total += s.getCount();
            }
        }
        return total;
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        String[]  ids    = { item1.get(),  item2.get(),  item3.get(),  item4.get(),  item5.get(),  item6.get()  };
        String[]  labels = { label1.get(), label2.get(), label3.get(), label4.get(), label5.get(), label6.get() };
        int[]     warns  = { warn1.get(),  warn2.get(),  warn3.get(),  warn4.get(),  warn5.get(),  warn6.get()  };

        List<TrackedItem>   active     = new ArrayList<>();
        List<Identifier>    activeIds  = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            String raw = ids[i].trim();
            if (raw.isEmpty()) continue;
            Identifier id;
            try { id = Identifier.of(raw.toLowerCase()); } catch (Exception e) { continue; }
            if (!Registries.ITEM.containsId(id)) continue;

            Item item  = Registries.ITEM.get(id);
            int  count = countItem(mc.player.getInventory(), item);
            if (hideEmpty.get() && count == 0) continue;

            String display = labels[i].isEmpty() ? id.getPath() : labels[i];
            active.add(new TrackedItem(display, count, count <= warns[i]));
            activeIds.add(id);
        }

        if (active.isEmpty()) return;

        int rowH   = showIcons.get() ? 18 : 12;
        int totalH = active.size() * rowH + 8;
        int maxW   = w;
        var tr = mc.textRenderer;
        for (TrackedItem t : active) maxW = Math.max(maxW, tr.getWidth(t.label() + ": " + t.count()) + (showIcons.get() ? 28 : 8));
        drawBackground(ctx, x, y, maxW, totalH);

        int lineY = y + 4;
        for (int i = 0; i < active.size(); i++) {
            TrackedItem t     = active.get(i);
            int         color = t.warning() ? colorWarn.get() : colorOk.get();
            String      line  = t.label() + ": " + t.count();
            if (showIcons.get()) {
                Item item = Registries.ITEM.get(activeIds.get(i));
                ctx.drawItem(new ItemStack(item), x + 4, lineY);
                ctx.drawTextWithShadow(tr, line, x + 24, lineY + 4, color);
                lineY += 18;
            } else {
                ctx.drawTextWithShadow(tr, line, x + 4, lineY, color);
                lineY += 12;
            }
        }
    }
}
