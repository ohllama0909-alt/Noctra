# Coordinates HUD

**ID:** `coordinates_hud`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/coordinates_hud/CoordinatesHudModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.coordinates_hud`

## System Notes (Updated)

- Module Ordner: `modules/impl/coordinates_hud/CoordinatesHudModule.java`
- Package: `de.snenjih.noctra.modules.impl.coordinates_hud`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`
- Neue Setting-Typen verfügbar: `KeybindSetting`, `ColorSetting`, `TextSetting`
- Direction HUD ist als `showDirection` Setting integriert (kein separates Modul)

## Description

Zeigt die aktuellen X/Y/Z-Koordinaten des Spielers als HUD-Overlay auf dem Bildschirm an. Optional werden auch Blickrichtung (Himmelsrichtung) und der aktuelle Chunk angezeigt. Hilfreich beim Navigieren, Markieren von Orten und beim Bauen.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `5` | `0–1920` | "X Position" | Horizontale Position des HUD-Elements |
| `y_pos` | Int | `5` | `0–1080` | "Y Position" | Vertikale Position des HUD-Elements |
| `text_color` | Int | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Farbe der Koordinatenanzeige (ARGB) |
| `show_direction` | Boolean | `true` | — | "Show Direction" | Himmelsrichtung zusätzlich anzeigen |
| `show_chunk` | Boolean | `false` | — | "Show Chunk" | Chunk-Koordinaten zusätzlich anzeigen |
| `decimal_places` | Int | `0` | `0–2` | "Decimal Places" | Nachkommastellen der Koordinaten |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund zeichnen |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Koordinaten jedes Frame aus `mc.player` lesen und an konfigurierter Position rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
1. In onRenderHud: Guard — if (mc.player == null || mc.world == null) return
2. Koordinaten lesen:
     double x = mc.player.getX()
     double y = mc.player.getY()
     double z = mc.player.getZ()
3. Blickrichtung berechnen (optional):
     float yaw = mc.player.getYaw() % 360  (normalisieren auf 0–360)
     String dir = yawToCardinal(yaw)   // "N", "NE", "E", "SE", "S", "SW", "W", "NW"
4. Chunk-Koordinaten (optional):
     int cx = (int) Math.floor(x) >> 4
     int cy = (int) Math.floor(y) >> 4
     int cz = (int) Math.floor(z) >> 4
5. Formatierung je nach decimalPlaces:
     String format = decimalPlaces == 0 ? "%.0f" : (decimalPlaces == 1 ? "%.1f" : "%.2f")
     String line1 = "XYZ: " + format(x) + " / " + format(y) + " / " + format(z)
6. Background-Rechteck zeichnen (optional):
     ctx.fill(xPos - 2, yPos - 2, xPos + textWidth + 2, yPos + lineCount * 10 + 2, 0x88000000)
7. Text zeichnen:
     ctx.drawTextWithShadow(mc.textRenderer, Text.literal(line1), xPos, yPos, textColor)
     if (showDirection) ctx.drawTextWithShadow(..., yPos + 10, ...)
     if (showChunk) ctx.drawTextWithShadow(..., yPos + 20, ...)
8. yawToCardinal(float yaw):
     Normalisiere yaw in [0, 360); teile in 8 Sektoren à 45°; index = (int)((yaw + 22.5) / 45) % 8
     String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"}  (Minecraft-Yaw: 0 = Süd)
```

### Edge Cases

- `mc.player == null`: Guard-Clause ganz am Anfang von `onRenderHud`, sofort `return`.
- Inventar-Screen offen (`mc.currentScreen != null`): HUD trotzdem rendern — Koordinaten sind auch im Inventar sinnvoll.
- Negative Koordinaten: `%.0f` formatiert korrekt mit Minuszeichen.
- `decimalPlaces == 0` → ganzzahlige Cast vermeiden, Format-String verwenden (behält Vorzeichen).
- Ping nur auf Multiplayer verfügbar; Koordinaten-HUD hat keine Netzwerkabhängigkeit.

## Translation Keys

```json
"noctra.coordinates_hud.name": "Coordinates HUD",
"noctra.coordinates_hud.description": "Displays your X/Y/Z coordinates on screen.",
"noctra.coordinates_hud.x_pos": "X Position",
"noctra.coordinates_hud.y_pos": "Y Position",
"noctra.coordinates_hud.text_color": "Text Color",
"noctra.coordinates_hud.show_direction": "Show Direction",
"noctra.coordinates_hud.show_chunk": "Show Chunk",
"noctra.coordinates_hud.decimal_places": "Decimal Places",
"noctra.coordinates_hud.background": "Background"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/coordinates_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Kompass-Rose oder drei Achsen (X/Y/Z) als farbige Pfeile (rot/grün/blau) in isometrischer Ansicht, vor dunkel-transparentem Hintergrund.
