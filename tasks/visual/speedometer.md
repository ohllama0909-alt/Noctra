# Speedometer HUD

**ID:** `speedometer`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/speedometer/SpeedometerModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.speedometer`

## Description

Zeigt die aktuelle Bewegungsgeschwindigkeit des Spielers in Blocks per Second (BPS) auf dem HUD an. Die Geschwindigkeit wird aus dem Velocity-Vektor des Spielers berechnet (horizontale Komponente). Optional wird die vertikale Geschwindigkeit separat angezeigt (nützlich beim Elytra-Tauchen). Vergleich mit Vanilla-Richtwerten (Gehen: 4.3, Sprinten: 5.6, Elytra: variabel) ist konfigurierbar.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `show_vertical` | Boolean | `false` | — | "Show Vertical Speed" | Vertikale Geschwindigkeit separat anzeigen |
| `show_max` | Boolean | `false` | — | "Show Max Speed" | Maximale Geschwindigkeit dieser Session |
| `show_bar` | Boolean | `false` | — | "Show Bar" | Balken relativ zu einer Referenzgeschwindigkeit |
| `bar_max_speed` | Float | `30.0` | `5.0–200.0` | "Bar Max (BPS)" | BPS-Wert der dem vollen Balken entspricht |
| `decimal_places` | Int | `1` | `0–2` | "Decimal Places" | Nachkommastellen |
| `smoothing` | Float | `0.8` | `0.0–0.99` | "Smoothing" | Glättungsfaktor (0 = keine Glättung, 0.99 = sehr träge) |
| `color_slow` | Color | `0xFF8899AA` | ARGB Hex | "Color Slow (<2 BPS)" | Farbe bei sehr langsamer Bewegung |
| `color_walk` | Color | `0xFFFFFFFF` | ARGB Hex | "Color Walk (2-5 BPS)" | Farbe bei Geh-Tempo |
| `color_sprint` | Color | `0xFF55FF55` | ARGB Hex | "Color Sprint (5-8 BPS)" | Farbe bei Sprint-Tempo |
| `color_fast` | Color | `0xFFFFAA00` | ARGB Hex | "Color Fast (>8 BPS)" | Farbe bei hoher Geschwindigkeit (Elytra etc.) |
| `show_reference` | Boolean | `false` | — | "Show Reference" | Referenz-Geschwindigkeiten einblenden |
| `hide_when_still` | Boolean | `false` | — | "Hide When Still" | Ausblenden wenn Geschwindigkeit < 0.1 BPS |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Velocity aus Player lesen und anzeigen.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
// Geschwindigkeit berechnen aus Velocity:
// player.getVelocity() gibt Vec3d zurück (blocks per tick)
// 1 Tick = 1/20 Sekunde → BPS = velocity * 20

// Felder für Glättung:
private double smoothedHorizSpeed = 0.0
private double smoothedVertSpeed  = 0.0
private double maxSpeedSession     = 0.0

onRenderHud(ctx, tickDelta):
    if (mc.player == null) return
    
    var vel = mc.player.getVelocity()
    
    // Horizontale Geschwindigkeit (X + Z Ebene)
    double rawHoriz = Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20.0
    double rawVert  = vel.y * 20.0   // positiv = aufwärts, negativ = fallend
    
    // Glättung: Exponential Moving Average
    float alpha = 1.0f - smoothing.get()
    smoothedHorizSpeed = smoothedHorizSpeed * smoothing.get() + rawHoriz * alpha
    smoothedVertSpeed  = smoothedVertSpeed  * smoothing.get() + rawVert  * alpha
    
    // Max tracken (ungeglättet)
    maxSpeedSession = Math.max(maxSpeedSession, rawHoriz)
    
    // Ausblenden wenn stillstehend
    if (hideWhenStill.get() && smoothedHorizSpeed < 0.1) return
    
    // Farbe wählen
    int color
    if      (smoothedHorizSpeed < 2.0) color = colorSlow.get()
    else if (smoothedHorizSpeed < 5.0) color = colorWalk.get()
    else if (smoothedHorizSpeed < 8.0) color = colorSprint.get()
    else                                color = colorFast.get()
    
    // Formatierung
    String fmt = "%." + decimalPlaces.get() + "f"
    String speedStr = "Speed: " + String.format(fmt, smoothedHorizSpeed) + " BPS"
    
    // Rendering
    int lineY = y + 4
    drawText(ctx, speedStr, x + 4, lineY, color);  lineY += 10
    
    if (showVertical.get()):
        String sign = smoothedVertSpeed > 0 ? "+" : ""
        String vertStr = "Vert: " + sign + String.format(fmt, smoothedVertSpeed) + " BPS"
        drawText(ctx, vertStr, x + 4, lineY, 0xFF8899AA);  lineY += 10
    
    if (showMax.get()):
        drawText(ctx, "Max: " + String.format(fmt, maxSpeedSession), x + 4, lineY, colorFast.get());  lineY += 10
    
    if (showBar.get()):
        int barX = x + 4; int barY = lineY; int barW = getDefaultWidth() - 8; int barH = 4
        float ratio = (float) Math.min(1.0, smoothedHorizSpeed / barMaxSpeed.get())
        int fill = (int) (barW * ratio)
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF333333)
        if (fill > 0) ctx.fill(barX, barY, barX + fill, barY + barH, color)
        lineY += 8
    
    // Referenz-Werte (optional)
    if (showReference.get()):
        drawText(ctx, "Walk:4.3  Sprint:5.6", x + 4, lineY, 0xFF556677)

// Max-Reset bei Welt-Wechsel oder Modul-Toggle
onLeaveWorld():
    maxSpeedSession = 0.0
    smoothedHorizSpeed = 0.0
    smoothedVertSpeed = 0.0
```

**Vanilla-Richtwerte (zur Orientierung):**

| Bewegungsart | BPS (ungefähr) |
|---|---|
| Schleichen | 1.3 |
| Gehen | 4.3 |
| Sprinten | 5.6 |
| Sprinten + Sprung | 7.1 (kurz) |
| Pferd (langsam) | ~5 |
| Pferd (schnell) | ~14 |
| Elytra (flach) | ~12–20 |
| Elytra (Sturzflug) | 50+ |
| Elytra + Rakete | ~32 |

### Edge Cases

- `getVelocity()` gibt den Wert des **letzten Ticks** zurück (kann zwischen Frames unverändert sein) → Glättung macht den Wert visuell angenehmer.
- Teleportation: `velocity` springt kurzzeitig auf riesige Werte → `Math.min(rawHoriz, 1000.0)` als Sanity-Cap.
- Wasser/Lava: Verlangsamt Velocity → korrekt erfasst, keine Sonderbehandlung nötig.
- Ritt auf Entität: Velocity des Spielers entspricht der Reittier-Velocity → korrekt angezeigt.
- `smoothing = 0.0`: Raw-Wert, kann bei Sprüngen stark zittern → User sollte wissen dass höherer Wert = glätter.

## Translation Keys

```json
"noctra.speedometer.name": "Speedometer",
"noctra.speedometer.description": "Shows your current movement speed in blocks per second.",
"noctra.speedometer.bg_color": "Background Color",
"noctra.speedometer.border_color": "Border Color",
"noctra.speedometer.text_color": "Text Color",
"noctra.speedometer.text_shadow": "Text Shadow",
"noctra.speedometer.text_scale": "Text Scale",
"noctra.speedometer.show_background": "Show Background",
"noctra.speedometer.show_vertical": "Show Vertical Speed",
"noctra.speedometer.show_max": "Show Max Speed",
"noctra.speedometer.show_bar": "Show Bar",
"noctra.speedometer.bar_max_speed": "Bar Max (BPS)",
"noctra.speedometer.decimal_places": "Decimal Places",
"noctra.speedometer.smoothing": "Smoothing",
"noctra.speedometer.color_slow": "Color Slow (<2 BPS)",
"noctra.speedometer.color_walk": "Color Walk (2-5 BPS)",
"noctra.speedometer.color_sprint": "Color Sprint (5-8 BPS)",
"noctra.speedometer.color_fast": "Color Fast (>8 BPS)",
"noctra.speedometer.show_reference": "Show Reference",
"noctra.speedometer.hide_when_still": "Hide When Still"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/speedometer.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Stilisiertes Tachometer-Zifferblatt in Pixel-Art. Zeiger zeigt nach rechts (schnell). Grüne Beschleunigungsfarbe.
