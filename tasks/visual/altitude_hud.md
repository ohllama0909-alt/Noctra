# Altitude Above Ground HUD

**ID:** `altitude_hud`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/altitude_hud/AltitudeHudModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.altitude_hud`

## Description

Zeigt die Höhe des Spielers über dem tatsächlichen Terrain in Echtzeit an — nicht die Y-Koordinate über Meeresspiegel, sondern die Distanz zum nächsten Boden-Block direkt darunter (Raycast nach unten). Unverzichtbar beim Elytra-Fliegen, Parkour und beim Abseilen: "32 Blöcke über Boden" ist viel nützlicher als "Y: 134". Optional: Warnfarbe wenn Höhe unter einem konfigurierbaren Schwellenwert fällt.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `max_raycast_depth` | Int | `256` | `16–512` | "Max Scan Depth" | Maximale Tiefe des Raycasts nach unten (Blöcke) |
| `warn_threshold` | Int | `10` | `0–100` | "Warn Below (blocks)" | Warnfarbe wenn Höhe unter diesem Wert |
| `crit_threshold` | Int | `4` | `0–50` | "Danger Below (blocks)" | Kritische Farbe (Sturzschaden-Gefahr) |
| `color_safe` | Color | `0xFF55FF55` | ARGB Hex | "Color Safe" | Farbe bei sicherer Höhe |
| `color_warn` | Color | `0xFFFFFF55` | ARGB Hex | "Color Warn" | Warnfarbe |
| `color_danger` | Color | `0xFFFF5555` | ARGB Hex | "Color Danger" | Kritische Farbe bei Absturzgefahr |
| `show_sea_diff` | Boolean | `false` | — | "Show Sea Level Diff" | Höhe über Meeresspiegel (Y-64) zusätzlich anzeigen |
| `hide_when_grounded` | Boolean | `false` | — | "Hide When Grounded" | Ausblenden wenn der Spieler auf dem Boden steht |
| `show_bar` | Boolean | `false` | — | "Show Bar" | Visuelle Balkenanzeige (Verhältnis Höhe/max_depth) |
| `only_when_flying` | Boolean | `false` | — | "Only When Flying" | Nur anzeigen wenn Spieler fliegt oder gleitet |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Raycast berechnen und rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
// Raycast-Ansatz: Blöcke direkt unter dem Spieler iterieren
int getAltitudeAboveGround(MinecraftClient mc):
    if (mc.world == null || mc.player == null) return -1
    
    int playerY    = (int) Math.floor(mc.player.getY())
    int worldMin   = mc.world.getBottomY()   // z.B. -64 in 1.18+
    int maxDepth   = maxRaycastDepth.get()
    
    // Nach unten scannen: erster fester Block unter dem Spieler
    for (int dy = 1; dy <= maxDepth; dy++):
        int checkY = playerY - dy
        if (checkY < worldMin) break
        
        BlockPos pos   = new BlockPos((int) mc.player.getX(), checkY, (int) mc.player.getZ())
        BlockState state = mc.world.getBlockState(pos)
        
        // Solider Block? Nicht Luft, Wasser, Glas etc. optional konfigurierbar
        if (!state.isAir() && state.isSolidBlock(mc.world, pos)):
            return dy - 1   // dy-1 weil Block bei dy, Player-Füße direkt darüber
    
    return -1   // Kein Boden gefunden (Void oder zu tief)

onRenderHud(ctx, tickDelta):
    if (mc.player == null) return
    
    // Bedingungen prüfen
    if (hideWhenGrounded.get() && mc.player.isOnGround()) return
    if (onlyWhenFlying.get() && !mc.player.isGliding() && !mc.player.isCreative()) return
    
    int altitude = getAltitudeAboveGround(mc)
    
    // Farbe
    int color = textColor.get()
    if (altitude < 0):
        color = 0xFF888888   // kein Boden gefunden → grau
    else if (altitude <= critThreshold.get()):
        color = colorDanger.get()
    else if (altitude <= warnThreshold.get()):
        color = colorWarn.get()
    else:
        color = colorSafe.get()
    
    String altStr = altitude < 0 ? "Alt: ?" : "Alt: " + altitude + " ▼"
    
    int lineY = y + 4
    drawText(ctx, altStr, x + 4, lineY, color);  lineY += 10
    
    if (showSeaDiff.get()):
        int seaDiff = (int) mc.player.getY() - 64
        String seaStr = (seaDiff >= 0 ? "+" : "") + seaDiff + " MSL"
        drawText(ctx, seaStr, x + 4, lineY, 0xFF8899AA);  lineY += 10
    
    if (showBar.get() && altitude >= 0):
        int barX = x + 4; int barY = lineY; int barW = getDefaultWidth() - 8; int barH = 4
        float ratio = Math.min(1f, altitude / (float) maxRaycastDepth.get())
        int fill = (int) (barW * ratio)
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF333333)
        if (fill > 0) ctx.fill(barX, barY, barX + fill, barY + barH, color)
```

**Performance-Hinweis:** Der Block-Scan wird jeden Frame ausgeführt. Bei `max_raycast_depth = 256` werden pro Frame bis zu 256 `getBlockState()`-Aufrufe gemacht. Das ist vergleichbar mit dem F3-Screen und in der Praxis kein Bottleneck, sollte aber nicht noch weiter erhöht werden. Alternativ: Scan nur jeden 2. Tick (nicht jeden Frame) und Ergebnis cachen.

### Edge Cases

- `altitude = -1` (kein Boden): Anzeige "Alt: ?" in Grau. Passiert im Void oder wenn `max_depth` zu klein.
- Spieler unter Y=0: `worldMin = mc.world.getBottomY()` (= -64) korrekt berücksichtigen.
- Wasser unter dem Spieler: `state.isAir()` ist false für Wasser — Wasser wird als Boden gewertet. Akzeptierbar. Optional: Wasser als transparent konfigurierbar.
- `isOnGround()` ist manchmal kurz falsch nach einem Sprung → kurze Falsch-Anzeige möglich, ignorierbar.
- Creative/Spectator Fliegen: `only_when_flying` mit `player.getAbilities().flying` kombinieren.

## Translation Keys

```json
"noctra.altitude_hud.name": "Altitude HUD",
"noctra.altitude_hud.description": "Shows your height above the terrain below using a downward block scan.",
"noctra.altitude_hud.bg_color": "Background Color",
"noctra.altitude_hud.border_color": "Border Color",
"noctra.altitude_hud.text_color": "Text Color",
"noctra.altitude_hud.text_shadow": "Text Shadow",
"noctra.altitude_hud.text_scale": "Text Scale",
"noctra.altitude_hud.show_background": "Show Background",
"noctra.altitude_hud.max_raycast_depth": "Max Scan Depth",
"noctra.altitude_hud.warn_threshold": "Warn Below (blocks)",
"noctra.altitude_hud.crit_threshold": "Danger Below (blocks)",
"noctra.altitude_hud.color_safe": "Color Safe",
"noctra.altitude_hud.color_warn": "Color Warn",
"noctra.altitude_hud.color_danger": "Color Danger",
"noctra.altitude_hud.show_sea_diff": "Show Sea Level Diff",
"noctra.altitude_hud.hide_when_grounded": "Hide When Grounded",
"noctra.altitude_hud.show_bar": "Show Bar",
"noctra.altitude_hud.only_when_flying": "Only When Flying"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/altitude_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Elytra-Silhouette oben, Terrain-Linie unten, vertikaler Pfeil dazwischen mit Zahlenbeschriftung.
