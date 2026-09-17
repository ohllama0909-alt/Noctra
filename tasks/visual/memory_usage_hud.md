# Memory Usage HUD

**ID:** `memory_usage_hud`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/memory_usage_hud/MemoryUsageHudModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.memory_usage_hud`

## Description

Zeigt den aktuellen JVM-Heap-Speicherverbrauch auf dem HUD an. Liest `Runtime.getRuntime()` aus und berechnet Used/Max in Megabyte. Nützlich um Memory-Leaks zu erkennen, herauszufinden wann Garbage Collection fällig ist, und Minecraft-Speicher-Einstellungen zu optimieren. Optionaler Balken zeigt den Füllstand visuell.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `show_bar` | Boolean | `true` | — | "Show Bar" | Speicherbalken anzeigen |
| `show_max` | Boolean | `true` | — | "Show Max" | Maximalen Heap anzeigen (z.B. "512 / 2048 MB") |
| `show_percentage` | Boolean | `false` | — | "Show Percent" | Prozentanzeige statt MB |
| `warn_threshold` | Int | `80` | `30–99` | "Warn Threshold (%)" | Ab diesem % Warnfarbe |
| `crit_threshold` | Int | `95` | `50–100` | "Crit Threshold (%)" | Ab diesem % kritische Farbe |
| `color_good` | Color | `0xFF55FF55` | ARGB Hex | "Color Good" | Farbe bei normalem Verbrauch |
| `color_warn` | Color | `0xFFFFFF55` | ARGB Hex | "Color Warn" | Warnfarbe |
| `color_crit` | Color | `0xFFFF5555` | ARGB Hex | "Color Critical" | Kritische Farbe |
| `unit` | Int | `0` | `0=MB, 1=GB` | "Unit" | Anzeigeeinheit |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Speicher auslesen und rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
onRenderHud(ctx, tickDelta):
    if (mc.player == null) return

    Runtime rt    = Runtime.getRuntime()
    long used     = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)  // MB
    long max      = rt.maxMemory() / (1024 * 1024)                         // MB
    double pct    = (double) used / max * 100.0

    // GB-Umrechnung wenn unit == 1
    String usedStr = unit.get() == 1
        ? String.format("%.1fGB", used / 1024.0)
        : used + "MB"
    String maxStr  = unit.get() == 1
        ? String.format("%.1fGB", max / 1024.0)
        : max + "MB"

    // Farbe wählen
    int memColor = colorGood.get()
    if (pct >= critThreshold.get())     memColor = colorCrit.get()
    else if (pct >= warnThreshold.get()) memColor = colorWarn.get()

    // Anzeigetext
    String line
    if (showPercentage.get()):
        line = String.format("RAM: %.0f%%", pct)
    else if (showMax.get()):
        line = "RAM: " + usedStr + " / " + maxStr
    else:
        line = "RAM: " + usedStr

    int w = Math.max(100, mc.textRenderer.getWidth(line) + 8)
    int h = showBar.get() ? 26 : 18

    if (showBackground.get()):
        ctx.fill(x, y, x + w, y + h, bgColor.get())
        ctx.drawStrokedRectangle(x, y, w, h, borderColor.get())

    drawText(ctx, line, x + 4, y + 4, memColor)

    if (showBar.get()):
        int barX = x + 4; int barY = y + 16; int barW = w - 8; int barH = 4
        int fill = (int) (barW * Math.min(1.0, pct / 100.0))
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF333333)
        if (fill > 0) ctx.fill(barX, barY, barX + fill, barY + barH, memColor)
```

### Edge Cases

- `rt.maxMemory()` kann `Long.MAX_VALUE` zurückgeben wenn kein `-Xmx` gesetzt ist. Guard: `if (max == Long.MAX_VALUE / (1024*1024)) max = rt.totalMemory() / (1024*1024)`.
- Division durch 0: `if (max <= 0) return`.
- `unit = 1` (GB) bei kleinen Heaps: Zeigt "0.5GB" → das ist korrekt und gewollt.
- Garbage Collection: Wert kann kurzzeitig stark fallen → kein Problem, zeigt echten Zustand.

## Translation Keys

```json
"noctra.memory_usage_hud.name": "Memory Usage",
"noctra.memory_usage_hud.description": "Shows JVM heap memory usage on the HUD.",
"noctra.memory_usage_hud.bg_color": "Background Color",
"noctra.memory_usage_hud.border_color": "Border Color",
"noctra.memory_usage_hud.text_color": "Text Color",
"noctra.memory_usage_hud.text_shadow": "Text Shadow",
"noctra.memory_usage_hud.text_scale": "Text Scale",
"noctra.memory_usage_hud.show_background": "Show Background",
"noctra.memory_usage_hud.show_bar": "Show Bar",
"noctra.memory_usage_hud.show_max": "Show Max",
"noctra.memory_usage_hud.show_percentage": "Show Percent",
"noctra.memory_usage_hud.warn_threshold": "Warn Threshold (%)",
"noctra.memory_usage_hud.crit_threshold": "Crit Threshold (%)",
"noctra.memory_usage_hud.color_good": "Color Good",
"noctra.memory_usage_hud.color_warn": "Color Warn",
"noctra.memory_usage_hud.color_crit": "Color Critical",
"noctra.memory_usage_hud.unit": "Unit"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/memory_usage_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** RAM-Riegel Pixel-Art mit einem farbigen Füllbalken. Grün→Gelb→Rot Gradient.
