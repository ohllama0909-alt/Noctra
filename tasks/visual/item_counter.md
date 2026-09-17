# Item Counter HUD

**ID:** `item_counter`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/item_counter/ItemCounterModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.item_counter`

## Description

Zeigt auf dem HUD die Gesamtanzahl von bis zu 6 frei konfigurierbaren Items im Inventar an. Der Spieler gibt Item-IDs (z.B. `minecraft:totem_of_undying`) als TextSetting ein — das Modul durchsucht dann Inventar, Hotbar, Offhand und optional Rüstungsslots nach diesen Items. Ideal für: Totem-Counter, Pfeile tracken, Nahrung zählen, TNT im Überblick behalten, oder jedes beliebige Item.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `item_1` | Text | `"minecraft:totem_of_undying"` | max 64 Zeichen | "Item 1 ID" | Erstes zu trackendes Item (Namespace:ID) |
| `label_1` | Text | `"Totem"` | max 20 Zeichen | "Item 1 Label" | Anzeigename für Item 1 |
| `warn_1` | Int | `1` | `0–64` | "Item 1 Warn ≤" | Warnfarbe wenn Anzahl ≤ diesem Wert |
| `item_2` | Text | `""` | max 64 Zeichen | "Item 2 ID" | Zweites zu trackendes Item (leer = inaktiv) |
| `label_2` | Text | `""` | max 20 Zeichen | "Item 2 Label" | Anzeigename für Item 2 |
| `warn_2` | Int | `0` | `0–64` | "Item 2 Warn ≤" | Warnfarbe wenn Anzahl ≤ diesem Wert |
| `item_3` | Text | `""` | max 64 Zeichen | "Item 3 ID" | Drittes Item (leer = inaktiv) |
| `label_3` | Text | `""` | max 20 Zeichen | "Item 3 Label" | Anzeigename für Item 3 |
| `warn_3` | Int | `0` | `0–64` | "Item 3 Warn ≤" | Warnfarbe |
| `item_4` | Text | `""` | max 64 Zeichen | "Item 4 ID" | Viertes Item (leer = inaktiv) |
| `label_4` | Text | `""` | max 20 Zeichen | "Item 4 Label" | Anzeigename für Item 4 |
| `warn_4` | Int | `0` | `0–64` | "Item 4 Warn ≤" | Warnfarbe |
| `item_5` | Text | `""` | max 64 Zeichen | "Item 5 ID" | Fünftes Item (leer = inaktiv) |
| `label_5` | Text | `""` | max 20 Zeichen | "Item 5 Label" | Anzeigename für Item 5 |
| `warn_5` | Int | `0` | `0–64` | "Item 5 Warn ≤" | Warnfarbe |
| `item_6` | Text | `""` | max 64 Zeichen | "Item 6 ID" | Sechstes Item (leer = inaktiv) |
| `label_6` | Text | `""` | max 20 Zeichen | "Item 6 Label" | Anzeigename für Item 6 |
| `warn_6` | Int | `0` | `0–64` | "Item 6 Warn ≤" | Warnfarbe |
| `include_offhand` | Boolean | `true` | — | "Include Offhand" | Offhand-Slot mitsuchen |
| `include_armor` | Boolean | `false` | — | "Include Armor" | Rüstungsslots mitsuchen |
| `color_warn` | Color | `0xFFFF5555` | ARGB Hex | "Warn Color" | Farbe wenn Anzahl ≤ warn threshold |
| `color_ok` | Color | `0xFF55FF55` | ARGB Hex | "OK Color" | Farbe wenn genug vorhanden |
| `hide_empty` | Boolean | `true` | — | "Hide Empty Slots" | Zeilen mit leerem Item-ID ausblenden |
| `show_icons` | Boolean | `false` | — | "Show Item Icons" | Item-Sprite neben dem Text (16x16) |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Inventar durchsuchen und Anzahlen anzeigen.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
// Hilfsmethode: Zählt alle Items eines Typs im Inventar
int countItem(PlayerInventory inv, Identifier itemId):
    int total = 0
    // Hauptinventar + Hotbar (Slots 0–35)
    for (ItemStack stack : inv.main):
        if (Registries.ITEM.getId(stack.getItem()).equals(itemId)):
            total += stack.getCount()
    // Offhand
    if (includeOffhand.get()):
        for (ItemStack stack : inv.offHand):
            if (Registries.ITEM.getId(stack.getItem()).equals(itemId)):
                total += stack.getCount()
    // Rüstung
    if (includeArmor.get()):
        for (ItemStack stack : inv.armor):
            if (Registries.ITEM.getId(stack.getItem()).equals(itemId)):
                total += stack.getCount()
    return total

// Item-ID parsen
Identifier parseId(String raw):
    try:
        return Identifier.of(raw.trim().toLowerCase())
    catch:
        return null   // Ungültige ID

onRenderHud(ctx, tickDelta):
    if (mc.player == null) return

    // Alle 6 Slots aufbauen
    String[] ids     = {item1.get(), item2.get(), item3.get(), item4.get(), item5.get(), item6.get()}
    String[] labels  = {label1.get(), label2.get(), label3.get(), label4.get(), label5.get(), label6.get()}
    int[]    warns   = {warn1.get(), warn2.get(), warn3.get(), warn4.get(), warn5.get(), warn6.get()}

    List<TrackedItem> active = new ArrayList<>()
    for (int i = 0; i < 6; i++):
        if (ids[i].isEmpty()) continue   // Slot inaktiv
        Identifier id = parseId(ids[i])
        if (id == null) continue
        if (!Registries.ITEM.containsId(id)) continue   // Item existiert nicht
        
        int count = countItem(mc.player.getInventory(), id)
        if (hideEmpty.get() && ids[i].isEmpty()) continue
        
        String displayLabel = labels[i].isEmpty() ? id.getPath() : labels[i]
        boolean warning = count <= warns[i]
        active.add(new TrackedItem(displayLabel, count, warning))

    if (active.isEmpty()) return   // Nichts zu zeigen

    // Hintergrund skalieren an Inhalt
    int rows = active.size()
    int totalH = rows * 12 + 8
    int totalW = getDefaultWidth()

    if (showBackground.get()):
        ctx.fill(x, y, x + totalW, y + totalH, bgColor.get())
        ctx.drawStrokedRectangle(x, y, totalW, totalH, borderColor.get())

    int lineY = y + 4
    for (TrackedItem item : active):
        int color = item.warning ? colorWarn.get() : colorOk.get()
        String line = item.label + ": " + item.count

        if (showIcons.get()):
            // 16x16 Item-Sprite links, Text daneben
            // drawItem(stack, x + 4, lineY - 4)  — stack aus Registries.ITEM.get(id)
            ctx.drawItem(new ItemStack(Registries.ITEM.get(id)), x + 4, lineY - 2)
            drawText(ctx, line, x + 24, lineY + 2, color)
        else:
            drawText(ctx, line, x + 4, lineY, color)

        lineY += 12

// Inner record/class für Rendering
record TrackedItem(String label, int count, boolean warning) {}
```

### Edge Cases

- Ungültige Item-ID (Tippfehler): `parseId()` gibt null zurück → Zeile wird übersprungen. Kein Crash.
- Item nicht im Registry: `Registries.ITEM.containsId()` prüfen → überspringen.
- Alle 6 Slots leer: `active.isEmpty()` → HUD wird komplett ausgeblendet.
- `hideEmpty = false` + leerem Item-ID: Zeigt "---: 0" (oder leere Zeile) — `hide_empty = true` empfohlen als Default.
- Stack-Größen: Items mit Stacks >64 (z.B. durch Mods) werden korrekt addiert.
- Inventar ändert sich zwischen Frames: Kein Caching nötig — Durchsuchen ist O(36+2+4) = trivial.

## Translation Keys

```json
"noctra.item_counter.name": "Item Counter",
"noctra.item_counter.description": "Tracks and displays counts of up to 6 custom items in your inventory.",
"noctra.item_counter.bg_color": "Background Color",
"noctra.item_counter.border_color": "Border Color",
"noctra.item_counter.text_color": "Text Color",
"noctra.item_counter.text_shadow": "Text Shadow",
"noctra.item_counter.text_scale": "Text Scale",
"noctra.item_counter.show_background": "Show Background",
"noctra.item_counter.item_1": "Item 1 ID",
"noctra.item_counter.label_1": "Item 1 Label",
"noctra.item_counter.warn_1": "Item 1 Warn ≤",
"noctra.item_counter.item_2": "Item 2 ID",
"noctra.item_counter.label_2": "Item 2 Label",
"noctra.item_counter.warn_2": "Item 2 Warn ≤",
"noctra.item_counter.item_3": "Item 3 ID",
"noctra.item_counter.label_3": "Item 3 Label",
"noctra.item_counter.warn_3": "Item 3 Warn ≤",
"noctra.item_counter.item_4": "Item 4 ID",
"noctra.item_counter.label_4": "Item 4 Label",
"noctra.item_counter.warn_4": "Item 4 Warn ≤",
"noctra.item_counter.item_5": "Item 5 ID",
"noctra.item_counter.label_5": "Item 5 Label",
"noctra.item_counter.warn_5": "Item 5 Warn ≤",
"noctra.item_counter.item_6": "Item 6 ID",
"noctra.item_counter.label_6": "Item 6 Label",
"noctra.item_counter.warn_6": "Item 6 Warn ≤",
"noctra.item_counter.include_offhand": "Include Offhand",
"noctra.item_counter.include_armor": "Include Armor",
"noctra.item_counter.color_warn": "Warn Color",
"noctra.item_counter.color_ok": "OK Color",
"noctra.item_counter.hide_empty": "Hide Empty Slots",
"noctra.item_counter.show_icons": "Show Item Icons"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/item_counter.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Inventar-Slot-Rahmen mit einem Totem-Icon und einer Zahl "2" in der Ecke. Pixel-Art.
