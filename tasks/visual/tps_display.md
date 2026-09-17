# TPS Display

**ID:** `tps_display`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/tps_display/TpsDisplayModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.tps_display`

## Description

Zeigt die geschätzten Ticks pro Sekunde (TPS) des Servers auf dem HUD an. Auf dem Client wird TPS nicht direkt übertragen — stattdessen wird die Änderung von `world.getTime()` über echte Zeitintervalle gemessen und daraus der TPS-Wert berechnet (gleitender Durchschnitt der letzten Sekunde). Ideal für Multiplayer um Server-Lag sofort zu erkennen.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `show_bar` | Boolean | `true` | — | "Show Bar" | TPS-Balken unter dem Text anzeigen |
| `good_threshold` | Int | `18` | `10–20` | "Good TPS (≥)" | Ab diesem Wert grüne Farbe |
| `bad_threshold` | Int | `15` | `5–19` | "Bad TPS (<)" | Unter diesem Wert rote Farbe |
| `color_good` | Color | `0xFF55FF55` | ARGB Hex | "Color Good" | Farbe bei gutem TPS |
| `color_warn` | Color | `0xFFFFFF55` | ARGB Hex | "Color Warn" | Farbe bei mittlerem TPS |
| `color_bad` | Color | `0xFFFF5555` | ARGB Hex | "Color Bad" | Farbe bei schlechtem TPS |
| `show_mspt` | Boolean | `false` | — | "Show MSPT" | Millisekunden pro Tick anzeigen (1000/TPS) |

## Implementation

### Event Hooks

- `onClientTick(MinecraftClient client)` — Weltzeit-Delta messen und TPS berechnen.
- `onRenderHud(DrawContext ctx, float tickDelta)` — TPS anzeigen.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
// Felder in der Modul-Klasse:
private long lastRealTime   = 0L     // System.currentTimeMillis() beim letzten Messpunkt
private long lastWorldTime  = 0L     // world.getTime() beim letzten Messpunkt
private double estimatedTps = 20.0   // gleitender Schätzwert
private int    ticksSinceUpdate = 0  // Zähler für Sample-Intervall

// Sample alle 20 Ticks (= ~1 Sekunde) für stabilen Wert
private static final int SAMPLE_INTERVAL = 20

onClientTick(client):
    if (client.world == null) return
    ticksSinceUpdate++
    if (ticksSinceUpdate < SAMPLE_INTERVAL) return
    ticksSinceUpdate = 0

    long nowReal  = System.currentTimeMillis()
    long nowWorld = client.world.getTime()

    if (lastRealTime != 0 && nowWorld > lastWorldTime):
        long realDelta  = nowReal  - lastRealTime    // ms vergangen
        long worldDelta = nowWorld - lastWorldTime   // Ticks vergangen

        // TPS = worldDelta / (realDelta / 1000.0), gedeckelt auf 20
        double rawTps = worldDelta / (realDelta / 1000.0)
        estimatedTps  = Math.min(20.0, rawTps)       // Server kann nicht schneller als 20 TPS
        // Glättung: Exponential Moving Average
        // estimatedTps = 0.7 * estimatedTps + 0.3 * rawTps (optional)

    lastRealTime  = nowReal
    lastWorldTime = nowWorld

onRenderHud(ctx, tickDelta):
    if (mc.world == null || mc.player == null) return

    int tpsRounded = (int) Math.round(estimatedTps)
    double mspt    = 1000.0 / Math.max(1, estimatedTps)

    // Farbe wählen
    int tpsColor = textColor.get()
    if (estimatedTps >= goodThreshold.get())     tpsColor = colorGood.get()
    else if (estimatedTps < badThreshold.get())  tpsColor = colorBad.get()
    else                                          tpsColor = colorWarn.get()

    String tpsText  = "TPS: " + tpsRounded
    String msptText = showMspt.get() ? String.format("MSPT: %.1f", mspt) : null

    int w = showBackground.get() ? getDefaultWidth() : 0
    int h = showBackground.get() ? (showBar.get() ? 26 : 18) : 0

    // Hintergrund
    if (showBackground.get()):
        ctx.fill(x, y, x + w, y + h, bgColor.get())
        ctx.drawStrokedRectangle(x, y, w, h, borderColor.get())

    // Text
    int ty = y + 4
    drawText(ctx, tpsText, x + 4, ty, tpsColor)
    if (msptText != null) drawText(ctx, msptText, x + 4 + mc.textRenderer.getWidth(tpsText) + 6, ty, textColor.get())

    // Balken (0–20 TPS)
    if (showBar.get()):
        int barX = x + 4; int barY = ty + 12; int barW = w - 8; int barH = 4
        int fill = (int) (barW * Math.min(1.0, estimatedTps / 20.0))
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF333333)
        if (fill > 0) ctx.fill(barX, barY, barX + fill, barY + barH, tpsColor)

// drawText-Hilfsmethode berücksichtigt text_shadow und text_scale
```

**Hinweis zu text_scale:** `MatrixStack` aus dem `DrawContext` nutzen um Text zu skalieren:
```java
ctx.getMatrices().push();
ctx.getMatrices().scale(textScale.get(), textScale.get(), 1.0f);
// Text zeichnen mit x/scale, y/scale
ctx.getMatrices().pop();
```

### Edge Cases

- Singleplayer: `world.getTime()` läuft gleichmäßig → TPS immer ~20. Kein Problem — wird korrekt angezeigt.
- Verbindungsunterbrechung: `world == null` Guard fängt ab.
- Server pausiert (z.B. Sleep-Phase): `worldDelta == 0` → Division durch 0 vermeiden: `if (worldDelta == 0) return`.
- Sehr schnelle Sample-Raten würden zittern — SAMPLE_INTERVAL = 20 stabilisiert.
- `realDelta == 0`: Guard `if (realDelta <= 0) return`.

## Translation Keys

```json
"noctra.tps_display.name": "TPS Display",
"noctra.tps_display.description": "Estimates and shows server ticks per second.",
"noctra.tps_display.bg_color": "Background Color",
"noctra.tps_display.border_color": "Border Color",
"noctra.tps_display.text_color": "Text Color",
"noctra.tps_display.text_shadow": "Text Shadow",
"noctra.tps_display.text_scale": "Text Scale",
"noctra.tps_display.show_background": "Show Background",
"noctra.tps_display.show_bar": "Show Bar",
"noctra.tps_display.good_threshold": "Good TPS (≥)",
"noctra.tps_display.bad_threshold": "Bad TPS (<)",
"noctra.tps_display.color_good": "Color Good",
"noctra.tps_display.color_warn": "Color Warn",
"noctra.tps_display.color_bad": "Color Bad",
"noctra.tps_display.show_mspt": "Show MSPT"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/tps_display.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Stilisiertes Zahnrad mit einer Uhr/Zifferblatt-Überlagerung. Grüner Tick-Symbol unten rechts.
