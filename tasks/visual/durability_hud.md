# Durability HUD

**ID:** `durability_hud`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/durability_hud/DurabilityHudModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.durability_hud`

## System Notes (Updated)

- Module Ordner: `modules/impl/durability_hud/DurabilityHudModule.java`
- Package: `de.snenjih.noctra.modules.impl.durability_hud`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`
- Neue Setting-Typen verfügbar: `ColorSetting` statt `IntSetting` für Farbwerte

## Description

Zeigt die verbleibende Durability des aktuell in der Hand gehaltenen Items groß und deutlich als HUD-Overlay an — sowohl als absoluten Zahlenwert als auch als prozentualen Balken. Verhindert das versehentliche Brechen wertvoller Werkzeuge und Waffen durch klare visuelle Rückmeldung.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `10` | `0–1920` | "X Position" | Horizontale Position des HUD-Elements |
| `y_pos` | Int | `40` | `0–1080` | "Y Position" | Vertikale Position des HUD-Elements |
| `show_bar` | Boolean | `true` | — | "Show Bar" | Durability-Balken anzeigen |
| `show_numbers` | Boolean | `true` | — | "Show Numbers" | Absolute Durability-Zahl anzeigen |
| `show_percent` | Boolean | `false` | — | "Show Percent" | Prozentwert anzeigen |
| `bar_width` | Int | `80` | `20–300` | "Bar Width" | Breite des Balkens in Pixeln |
| `color_full` | Int | `0xFF55FF55` | ARGB Hex | "Color Full" | Balkenfarbe bei hoher Durability |
| `color_warn` | Int | `0xFFFFAA00` | ARGB Hex | "Color Warn" | Balkenfarbe bei mittlerer Durability |
| `color_crit` | Int | `0xFFFF5555` | ARGB Hex | "Color Critical" | Balkenfarbe bei kritisch niedriger Durability |
| `warn_threshold` | Float | `0.5` | `0.1–0.9` | "Warn Threshold" | Durability-Anteil für Warnfarbe |
| `crit_threshold` | Float | `0.2` | `0.05–0.5` | "Crit Threshold" | Durability-Anteil für kritische Farbe |
| `hide_when_full` | Boolean | `false` | — | "Hide When Full" | Ausblenden wenn Durability maximal ist |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund zeichnen |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Haupthand-Item lesen, Durability berechnen, rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
onRenderHud(DrawContext ctx, float tickDelta):
    if (mc.player == null) return

    ItemStack stack = mc.player.getMainHandStack()
    if (stack.isEmpty()) return
    if (!stack.isDamageable()) return   // Items ohne Durability-System ausblenden

    int maxDmg  = stack.getMaxDamage()
    int curDmg  = stack.getDamage()
    int remaining = maxDmg - curDmg    // verbleibende Haltbarkeit
    float fraction = (float) remaining / maxDmg

    if (hideWhenFull.get() && fraction >= 1.0f) return

    // Farbe bestimmen (Drei-Stufen-Ampel)
    int color
    if (fraction <= critThreshold.get())      color = colorCrit.get()
    else if (fraction <= warnThreshold.get()) color = colorWarn.get()
    else                                       color = colorFull.get()

    int x = xPos.get()
    int y = yPos.get()
    int currentY = y

    // Optionaler Hintergrund
    if (background.get()):
        int bgH = (showBar.get() ? 7 : 0) + (showNumbers.get() ? 10 : 0) + (showPercent.get() ? 10 : 0) + 4
        ctx.fill(x - 2, y - 2, x + barWidth.get() + 2, y + bgH, 0x88000000)

    // Durability-Balken
    if (showBar.get()):
        ctx.fill(x, currentY, x + barWidth.get(), currentY + 6, 0xFF222222)  // Hintergrund
        int fill = Math.round(fraction * barWidth.get())
        ctx.fill(x, currentY, x + fill, currentY + 6, color)                 // Füllung
        currentY += 9

    // Absoluter Zahlenwert
    if (showNumbers.get()):
        String text = remaining + " / " + maxDmg
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(text), x, currentY, color)
        currentY += 10

    // Prozentwert
    if (showPercent.get()):
        String pct = String.format("%.1f%%", fraction * 100)
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(pct), x, currentY, color)
```

### Edge Cases

- `stack.isEmpty()`: Kein Item in der Hand → nichts rendern.
- `!stack.isDamageable()`: Unzerstörbares Item (Block, Nahrung, etc.) → nichts rendern.
- `maxDmg == 0`: Division durch null vermeiden — durch `isDamageable()`-Check bereits abgedeckt.
- Item ist maximal beschädigt (`remaining == 0`): Balken leer, `colorCrit` aktiv.
- Spieler wechselt Slot: Nächster Frame zeigt automatisch das neue Item.
- `hideWhenFull == true` und Item ist neu: kein Rendering.

## Translation Keys

```json
"noctra.durability_hud.name": "Durability HUD",
"noctra.durability_hud.description": "Shows the held item's durability as a large bar and number.",
"noctra.durability_hud.x_pos": "X Position",
"noctra.durability_hud.y_pos": "Y Position",
"noctra.durability_hud.show_bar": "Show Bar",
"noctra.durability_hud.show_numbers": "Show Numbers",
"noctra.durability_hud.show_percent": "Show Percent",
"noctra.durability_hud.bar_width": "Bar Width",
"noctra.durability_hud.color_full": "Color Full",
"noctra.durability_hud.color_warn": "Color Warn",
"noctra.durability_hud.color_crit": "Color Critical",
"noctra.durability_hud.warn_threshold": "Warn Threshold",
"noctra.durability_hud.crit_threshold": "Crit Threshold",
"noctra.durability_hud.hide_when_full": "Hide When Full",
"noctra.durability_hud.background": "Background"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/durability_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Pixeliges Schwert oder Werkzeug mit einem horizontalen Balken darunter, der von links nach rechts von grün nach rot verläuft. Unteres Drittel des Balkens rot gefärbt als Warnhinweis.
