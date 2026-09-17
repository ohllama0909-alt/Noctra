# Speed Display

**ID:** `speed_display`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/speed_display/SpeedDisplayModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.speed_display`

## System Notes (Updated)

- Module Ordner: `modules/impl/speed_display/SpeedDisplayModule.java`
- Package: `de.snenjih.noctra.modules.impl.speed_display`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Zeigt die aktuelle Bewegungsgeschwindigkeit des Spielers in Blöcken pro Sekunde (blocks/s) als HUD-Overlay an. Die Geschwindigkeit wird aus der horizontalen Positionsänderung zwischen Ticks berechnet. Nützlich für Speedrunner, beim Vergleichen von Bewegungsmethoden (Laufen, Pferd, Elytra) oder beim Optimieren von Parcours.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `2` | `0–1920` | "X Position" | Horizontale Position |
| `y_pos` | Int | `36` | `0–1080` | "Y Position" | Vertikale Position |
| `show_label` | Boolean | `true` | — | "Show Label" | Präfix "Speed: " anzeigen |
| `decimal_places` | Int | `2` | `0–3` | "Decimal Places" | Nachkommastellen |
| `show_vertical` | Boolean | `false` | — | "Show Vertical" | Vertikale Geschwindigkeit (Y-Achse) separat anzeigen |
| `text_color` | Int | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Textfarbe |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund |

## Implementation

### Event Hooks

- `onClientTick(MinecraftClient client)` — Vorherige Position speichern und Geschwindigkeit pro Tick berechnen.
- `onRenderHud(DrawContext ctx, float tickDelta)` — Gecachten Speed-Wert rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
Felder:
    private double prevX = 0, prevY = 0, prevZ = 0
    private double cachedSpeedH = 0   // horizontale Geschwindigkeit (blocks/s)
    private double cachedSpeedV = 0   // vertikale Geschwindigkeit (blocks/s)
    private boolean hasPrev = false

onClientTick(MinecraftClient client):
    if (client.player == null) return
    ClientPlayerEntity player = client.player

    double x = player.getX()
    double y = player.getY()
    double z = player.getZ()

    if (hasPrev):
        double dx = x - prevX
        double dy = y - prevY
        double dz = z - prevZ

        // Bewegung pro Tick → mal 20 = blocks/s
        double speedHPerTick = Math.sqrt(dx * dx + dz * dz)
        cachedSpeedH = speedHPerTick * 20.0

        double speedVPerTick = Math.abs(dy)
        cachedSpeedV = speedVPerTick * 20.0

    prevX = x
    prevY = y
    prevZ = z
    hasPrev = true

onLeaveWorld():
    hasPrev = false
    cachedSpeedH = 0
    cachedSpeedV = 0

onRenderHud(DrawContext ctx, float tickDelta):
    if (mc.player == null) return

    String fmt = "%." + decimalPlaces.get() + "f"
    String speedStr = String.format(fmt, cachedSpeedH)
    String line = showLabel.get() ? "Speed: " + speedStr + " b/s" : speedStr + " b/s"

    int x = xPos.get()
    int y = yPos.get()
    int currentY = y

    if (background.get()):
        int bgW = mc.textRenderer.getWidth(line) + 4
        int bgH = showVertical.get() ? 22 : 12
        ctx.fill(x - 2, y - 2, x + bgW, y + bgH, 0x88000000)

    ctx.drawTextWithShadow(mc.textRenderer, Text.literal(line), x, currentY, textColor.get())
    currentY += 10

    if (showVertical.get()):
        String vStr = String.format(fmt, cachedSpeedV)
        String vLine = showLabel.get() ? "V-Speed: " + vStr + " b/s" : vStr + " b/s"
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(vLine), x, currentY, textColor.get())
```

### Edge Cases

- `mc.player == null`: Guard in beiden Hooks.
- Erster Tick nach Enable (`hasPrev == false`): `cachedSpeedH` bleibt 0 → zeigt "0.00 b/s", korrekt.
- Teleportation (z. B. via `/tp`): Riesiger Sprung in einer Tick → sehr hoher Speed-Wert für einen Frame. Kein Smoothing implementiert — bei Bedarf kann ein gleitender Mittelwert über 5 Ticks ergänzt werden.
- `onLeaveWorld()` reset: Verhindert falsche Speed-Messung beim erneuten Betreten einer Welt.
- Elytra-Flug: Korrekt gemessen, da horizontale XZ-Distanz erfasst wird.
- Negative vertikale Geschwindigkeit (Fallen): `Math.abs(dy)` zeigt Fallgeschwindigkeit als positiven Wert.

## Translation Keys

```json
"noctra.speed_display.name": "Speed Display",
"noctra.speed_display.description": "Shows your movement speed in blocks per second.",
"noctra.speed_display.x_pos": "X Position",
"noctra.speed_display.y_pos": "Y Position",
"noctra.speed_display.show_label": "Show Label",
"noctra.speed_display.decimal_places": "Decimal Places",
"noctra.speed_display.show_vertical": "Show Vertical Speed",
"noctra.speed_display.text_color": "Text Color",
"noctra.speed_display.background": "Background"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/speed_display.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Stilisierter Tachometer oder Speedometer-Zeiger; Pfeile die nach rechts zeigen für Bewegung. Blau-weiße Farben.
