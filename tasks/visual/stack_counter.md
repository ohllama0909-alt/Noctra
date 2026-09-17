# Stack Counter HUD

**ID:** `stack_counter`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/stack_counter/StackCounterModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.stack_counter`

## Description

Zeigt die Gesamtanzahl des aktuell in der Hand gehaltenen Item-Typs über das gesamte Inventar an. Wenn der Spieler z.B. Pfeile hält, wird nicht nur die aktuelle Stack-Größe angezeigt, sondern alle Pfeile über alle Inventar-Slots summiert. Automatisch — kein manuelles Konfigurieren. Besonders nützlich für verstreute Ressourcen wie Nahrung, Munition oder Baumaterial.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `include_offhand` | Boolean | `true` | — | "Include Offhand" | Offhand-Slot mitzählen |
| `show_item_name` | Boolean | `true` | — | "Show Item Name" | Name des Items anzeigen |
| `show_stacks` | Boolean | `false` | — | "Show Stack Count" | Anzahl der Teil-Stacks anzeigen (z.B. "3 Stacks + 7") |
| `hide_when_full_stack` | Boolean | `false` | — | "Hide If Single Stack" | Ausblenden wenn alles in einem Stack |
| `hide_on_empty` | Boolean | `true` | — | "Hide When Empty" | Ausblenden wenn keine Hand leer |
| `warn_threshold` | Int | `0` | `0–2304` | "Warn Below" | Warnfarbe wenn Gesamtanzahl unter diesem Wert |
| `color_warn` | Color | `0xFFFF5555` | ARGB Hex | "Warn Color" | Warnfarbe |
| `count_nbt_stacks` | Boolean | `false` | — | "Count by Item Type" | Nur nach Item-ID zählen (ignoriert NBT/Enchants) |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Gehaltenes Item identifizieren, Inventar durchsuchen.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
onRenderHud(ctx, tickDelta):
    if (mc.player == null) return

    ItemStack held = mc.player.getMainHandStack()
    if (held.isEmpty()):
        if (hideOnEmpty.get()) return
        // Optional: leere Hand anzeigen

    var inv = mc.player.getInventory()
    int total = 0
    int stackCount = 0
    int remainder = 0

    // Zählmethode: nach Item-ID (ignoriert NBT) oder exakter Stack-Match
    for slot in 0..35 (main inventory):
        ItemStack stack = inv.getStack(slot)
        if (stack.isEmpty()) continue
        
        boolean match
        if (countNbtStacks.get()):
            // Exakter Match: Item + NBT müssen übereinstimmen
            match = ItemStack.areItemsAndComponentsEqual(held, stack)
        else:
            // Nur Item-Typ zählen
            match = stack.getItem() == held.getItem()
        
        if (match):
            total += stack.getCount()
            stackCount++
            remainder = stack.getCount()   // letzter Teilstack

    if (includeOffhand.get()):
        ItemStack offhand = inv.offHand.get(0)
        if (!offhand.isEmpty()):
            boolean match = countNbtStacks.get()
                ? ItemStack.areItemsAndComponentsEqual(held, offhand)
                : offhand.getItem() == held.getItem()
            if (match):
                total += offhand.getCount()
                stackCount++

    if (hideWhenFullStack.get() && stackCount <= 1) return

    // Display
    int maxStack = held.getMaxCount()
    String itemName = showItemName.get()
        ? held.getName().getString()
        : null

    int color = (warnThreshold.get() > 0 && total <= warnThreshold.get())
        ? colorWarn.get()
        : textColor.get()

    String countStr
    if (showStacks.get() && maxStack > 0):
        int fullStacks = total / maxStack
        int rem = total % maxStack
        countStr = fullStacks > 0
            ? (rem > 0 ? fullStacks + "×" + maxStack + "+" + rem : fullStacks + "×" + maxStack)
            : String.valueOf(rem)
    else:
        countStr = String.valueOf(total)

    String line = itemName != null
        ? itemName + ": " + countStr
        : "Total: " + countStr

    int w = Math.max(80, mc.textRenderer.getWidth(line) + 8)
    int h = 18

    if (showBackground.get()):
        ctx.fill(x, y, x + w, y + h, bgColor.get())
        ctx.drawStrokedRectangle(x, y, w, h, borderColor.get())

    drawText(ctx, line, x + 4, y + 4, color)
```

### Edge Cases

- `held.isEmpty()`: Kein Item in der Hand → entweder ausblenden (`hideOnEmpty`) oder "Hand: leer".
- Damageable Items (Werkzeuge, Rüstung): `areItemsAndComponentsEqual` berücksichtigt Schaden → verschiedene Haltbarkeitsstufen werden nicht zusammengezählt wenn `count_nbt_stacks = true`. Für Werkzeuge empfehle ich `false` (nur Item-Typ).
- Items ohne Stack (`maxCount == 1`): `show_stacks = false` empfohlen, da immer "1×1" angezeigt würde.
- Sehr viele Items (z.B. modded Mega-Stacks): `int` reicht für >2 Mio — kein Overflow bei normalem Gameplay.

## Translation Keys

```json
"noctra.stack_counter.name": "Stack Counter",
"noctra.stack_counter.description": "Shows the total count of your held item type across all inventory slots.",
"noctra.stack_counter.bg_color": "Background Color",
"noctra.stack_counter.border_color": "Border Color",
"noctra.stack_counter.text_color": "Text Color",
"noctra.stack_counter.text_shadow": "Text Shadow",
"noctra.stack_counter.text_scale": "Text Scale",
"noctra.stack_counter.show_background": "Show Background",
"noctra.stack_counter.include_offhand": "Include Offhand",
"noctra.stack_counter.show_item_name": "Show Item Name",
"noctra.stack_counter.show_stacks": "Show Stack Count",
"noctra.stack_counter.hide_when_full_stack": "Hide If Single Stack",
"noctra.stack_counter.hide_on_empty": "Hide When Empty",
"noctra.stack_counter.warn_threshold": "Warn Below",
"noctra.stack_counter.color_warn": "Warn Color",
"noctra.stack_counter.count_nbt_stacks": "Count by Item Type"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/stack_counter.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Drei übereinander gestapelte Inventar-Slots mit einer Summenzahl unten rechts. Einfache Pixel-Art.
