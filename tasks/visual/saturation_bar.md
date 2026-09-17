# Saturation Bar

**ID:** `saturation_bar`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/saturation_bar/SaturationBarModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.saturation_bar`

## System Notes (Updated)

- Module Ordner: `modules/impl/saturation_bar/SaturationBarModule.java`
- Package: `de.snenjih.noctra.modules.impl.saturation_bar`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Macht den normalerweise unsichtbaren Nahrungs-Sättigungswert als Balken sichtbar, der direkt neben oder unter der Hunger-Leiste dargestellt wird. Sättigung bestimmt, wie lange der Hunger-Wert stabil bleibt, bevor er abnimmt — ein oft übersehener Mechanikeismus. Hilft dabei, Essen effizienter einzusetzen und unnötigen Nahrungsverbrauch zu vermeiden.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `10` | `0–1920` | "X Position" | Horizontale Position (linke Kante des Balkens) |
| `y_pos` | Int | `10` | `0–1080` | "Y Position" | Vertikale Position; Default = nahe Hunger-Leiste |
| `anchor_to_hunger` | Boolean | `true` | — | "Anchor to Hunger Bar" | Automatisch unter die Hunger-Leiste positionieren |
| `bar_width` | Int | `81` | `20–200` | "Bar Width" | Breite des Balkens (Default: gleich wie Hunger-Leiste) |
| `bar_height` | Int | `4` | `1–12` | "Bar Height" | Höhe des Balkens in Pixeln |
| `color_sat` | Int | `0xFFFFD700` | ARGB Hex | "Saturation Color" | Farbe des Sättigungsbalkens (Gold) |
| `show_label` | Boolean | `false` | — | "Show Label" | Numerischen Wert neben dem Balken anzeigen |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Sättigungswert aus `player.getHungerManager()` lesen und als Balken rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
onRenderHud(DrawContext ctx, float tickDelta):
    if (mc.player == null) return

    HungerManager hunger = mc.player.getHungerManager()
    float saturation = hunger.getSaturationLevel()
    int foodLevel    = hunger.getFoodLevel()           // 0–20

    // Maximale Sättigung = foodLevel (Vanilla-Mechanikeismus: Saturation kann nicht größer sein als FoodLevel)
    float maxSat = (float) foodLevel
    float fraction = maxSat > 0 ? Math.min(saturation / maxSat, 1.0f) : 0f

    int x, y

    if (anchorToHunger.get()):
        // Hunger-Leiste befindet sich bei (scaledWidth/2 + 91) von links, ca. scaledHeight - 49 von oben
        // Vanilla-Werte (von InGameHud): hungerBarX = scaledWidth/2 + 10, hungerBarY = scaledHeight - 49
        // Saturation-Bar direkt darunter
        int scaledW = mc.getWindow().getScaledWidth()
        int scaledH = mc.getWindow().getScaledHeight()
        x = scaledW / 2 + 10     // Vanilla hunger bar origin
        y = scaledH - 49 + 9     // Direkt unter der Hunger-Leiste
    else:
        x = xPos.get()
        y = yPos.get()

    int w = barWidth.get()
    int h = barHeight.get()

    // Hintergrund
    if (background.get()):
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x88000000)

    // Leerer Balken
    ctx.fill(x, y, x + w, y + h, 0xFF222222)

    // Sättigungs-Füllung
    int fillW = Math.round(fraction * w)
    if (fillW > 0):
        ctx.fill(x, y, x + fillW, y + h, colorSat.get())

    // Numerischer Wert (optional)
    if (showLabel.get()):
        String label = String.format("%.1f", saturation)
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(label), x + w + 3, y, 0xFFFFD700)
```

**Hinweis zu Vanilla-Koordinaten der Hunger-Leiste:** Die genaue Y-Position der Hunger-Leiste ist `scaledHeight - 49` (Vanilla `InGameHud` in Yarn 1.21.11). Die Hunger-Icons sind 10px hoch, also positioniert man den Saturation-Bar bei Y = `scaledHeight - 49 + 9` direkt darunter. Diese Werte sollten beim Testen verifiziert werden, da sie sich mit Fabrics API-Änderungen verschieben können.

### Edge Cases

- `mc.player == null`: Guard-Clause.
- `foodLevel == 0`: `maxSat = 0`, Division durch null vermieden via `fraction = 0f`.
- `saturation > foodLevel`: Kann nicht in Vanilla passieren (Mechanikeismus-Regel), aber `Math.min(..., 1.0f)` schützt dagegen.
- `anchorToHunger == true` im Menü (`mc.currentScreen != null`): Hunger-Leiste nicht sichtbar, aber Modul rendert trotzdem. Akzeptables Verhalten.
- Skalierter GUI-Maßstab geändert: `mc.getWindow().getScaledWidth/Height()` liefert immer die aktuelle skalierte Größe — korrekt.

## Translation Keys

```json
"noctra.saturation_bar.name": "Saturation Bar",
"noctra.saturation_bar.description": "Makes the hidden food saturation value visible as a bar.",
"noctra.saturation_bar.x_pos": "X Position",
"noctra.saturation_bar.y_pos": "Y Position",
"noctra.saturation_bar.anchor_to_hunger": "Anchor to Hunger Bar",
"noctra.saturation_bar.bar_width": "Bar Width",
"noctra.saturation_bar.bar_height": "Bar Height",
"noctra.saturation_bar.color_sat": "Saturation Color",
"noctra.saturation_bar.show_label": "Show Label",
"noctra.saturation_bar.background": "Background"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/saturation_bar.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Vanilla-Hunger-Drumstick-Icon mit einem goldenen Glitzern oder einem gelben Balken darunter. Goldene/Gelbe Farbtöne zur Unterscheidung von der roten Hunger-Leiste.
