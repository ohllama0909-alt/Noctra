# Direction HUD

**ID:** `direction_hud`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/coordinates_hud/CoordinatesHudModule.java` (integriert als `showDirection` Setting)  
**Package:** `de.snenjih.noctra.modules.impl.coordinates_hud`

## System Notes (Updated)

- Direction HUD ist als `showDirection` Boolean-Setting in `CoordinatesHudModule` integriert.
- Kein separates Modul nötig — Himmelsrichtungs- und Yaw-Anzeige erfolgt über das Coordinates HUD.
- CoordinatesHudModule implementiert HudElement: `extends BaseModule implements HudElement`

## Description

Zeigt die aktuelle Blickrichtung als Himmelsrichtung (N/NE/E/SE/S/SW/W/NW) sowie den exakten Yaw- und Pitch-Winkel als HUD-Overlay an. Optional wird ein horizontaler Mini-Kompassstreifen gezeichnet, der die Richtung visuell markiert. Nützlich für Navigation ohne externe Karten und für präzises Ausrichten von Strukturen.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `2` | `0–1920` | "X Position" | Horizontale Position |
| `y_pos` | Int | `26` | `0–1080` | "Y Position" | Vertikale Position |
| `show_cardinal` | Boolean | `true` | — | "Show Cardinal" | Himmelsrichtung (N, NE…) anzeigen |
| `show_degrees` | Boolean | `true` | — | "Show Degrees" | Yaw-Grad-Wert anzeigen |
| `show_pitch` | Boolean | `false` | — | "Show Pitch" | Pitch-Winkel (Blick hoch/runter) anzeigen |
| `show_compass_bar` | Boolean | `true` | — | "Show Compass Bar" | Horizontalen Kompassstreifen anzeigen |
| `compass_width` | Int | `120` | `60–400` | "Compass Width" | Breite des Kompassstreifens in Pixeln |
| `text_color` | Int | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Textfarbe |
| `highlight_color` | Int | `0xFFFF5555` | ARGB Hex | "Highlight Color" | Farbe der aktuellen Himmelsrichtung im Kompassstreifen |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Yaw und Pitch des Spielers rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
// Himmelsrichtungs-Lookup (Minecraft-Yaw: 0° = Süd, +90° = West, -90° = Ost)
String[] CARDINALS     = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"}
float[]  CARDINAL_YAWS = {0f,  45f,  90f, 135f, 180f, 225f, 270f, 315f}  // positive Normalisierung

Hilfsmethode normalizeYaw(float yaw):
    // Minecraft-Yaw: Süd=0, West=90, Nord=180, Ost=270 (jeweils mod 360)
    return ((yaw % 360) + 360) % 360

Hilfsmethode yawToCardinal(float normalizedYaw):
    int index = (int)((normalizedYaw + 22.5f) / 45f) % 8
    return CARDINALS[index]

onRenderHud(DrawContext ctx, float tickDelta):
    if (mc.player == null) return

    float yaw   = normalizeYaw(mc.player.getYaw())
    float pitch = mc.player.getPitch()

    int x = xPos.get()
    int y = yPos.get()
    int currentY = y

    // Hintergrund-Vorberechnung
    // (wird erst nach allen Elementen gezeichnet, oder zuerst mit geschätzter Größe)
    if (background.get()):
        int bgH = (showCardinal.get() || showDegrees.get() ? 10 : 0)
                + (showPitch.get() ? 10 : 0)
                + (showCompassBar.get() ? 14 : 0) + 4
        ctx.fill(x - 2, y - 2, x + compassWidth.get() + 2, y + bgH, 0x88000000)

    // Kompassstreifen (zuerst, damit Text darüber liegt)
    if (showCompassBar.get()):
        drawCompassBar(ctx, x, currentY, compassWidth.get(), yaw)
        currentY += 14

    // Textzeile: "NW  270.3°"
    if (showCardinal.get() || showDegrees.get()):
        String cardinal = yawToCardinal(yaw)
        String line = ""
        if (showCardinal.get())  line += cardinal + "  "
        if (showDegrees.get())   line += String.format("%.1f°", yaw)
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(line.trim()), x, currentY, textColor.get())
        currentY += 10

    // Pitch
    if (showPitch.get()):
        String pitchLine = String.format("Pitch: %.1f°", pitch)
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(pitchLine), x, currentY, textColor.get())

Methode drawCompassBar(DrawContext ctx, int x, int y, int width, float yaw):
    // Zeichnet eine Reihe von Himmelsrichtungs-Labels über einen horizontalen Streifen
    // Hintergrundleiste
    ctx.fill(x, y, x + width, y + 10, 0xFF333333)

    // Für jede der 8 Himmelsrichtungen: Winkelabstand zur aktuellen Blickrichtung berechnen
    for (String cardinal : CARDINALS at their respective yaw degrees):
        float diff = normalizeYaw(cardinalYaw - yaw + 180) - 180  // Abstand in [-180, 180]
        // Skalierung: diff / 180 * (width/2) = Pixel-Offset von der Mitte
        int pixelOffset = (int)(diff / 180f * (width / 2f))
        int labelX = x + width / 2 + pixelOffset

        if (labelX < x || labelX > x + width) continue  // außerhalb sichtbar

        boolean isCurrent = Math.abs(diff) < 22.5f
        int labelColor = isCurrent ? highlightColor.get() : textColor.get()
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(cardinal),
                               labelX - mc.textRenderer.getWidth(cardinal)/2, y + 1, labelColor)

    // Mittellinie / Zeiger
    ctx.fill(x + width/2, y, x + width/2 + 1, y + 10, 0xFFFFFFFF)
```

### Edge Cases

- `mc.player == null`: Guard-Clause.
- Yaw-Wert schwankt nahe ±180° (Nord-Übergang): `normalizeYaw()` behandelt negative Werte korrekt via doppeltem Modulo.
- Kompassstreifen-Labels außerhalb des Streifens: `continue` überspringt sie.
- `compassWidth` sehr klein (<60px): Kaum lesbar, aber kein Crash.
- Pitch außerhalb von [-90°, 90°]: Vanilla begrenzt Pitch; kein Problem.

## Translation Keys

```json
"noctra.direction_hud.name": "Direction HUD",
"noctra.direction_hud.description": "Displays your facing direction and a compass strip on screen.",
"noctra.direction_hud.x_pos": "X Position",
"noctra.direction_hud.y_pos": "Y Position",
"noctra.direction_hud.show_cardinal": "Show Cardinal",
"noctra.direction_hud.show_degrees": "Show Degrees",
"noctra.direction_hud.show_pitch": "Show Pitch",
"noctra.direction_hud.show_compass_bar": "Show Compass Bar",
"noctra.direction_hud.compass_width": "Compass Width",
"noctra.direction_hud.text_color": "Text Color",
"noctra.direction_hud.highlight_color": "Highlight Color",
"noctra.direction_hud.background": "Background"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/direction_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Kompassscheibe mit roter Nord-Nadel; "N" oben hervorgehoben. Pixel-Art-Stil, kreisförmig.
