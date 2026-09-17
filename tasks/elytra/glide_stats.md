# Glide Stats

**ID:** `glide_stats`  
**Category:** ELYTRA  
**Status:** [x] DONE  
**Class:** `modules/impl/glide_stats/GlideStatsModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.glide_stats`

## System Notes (Updated)

- Module Ordner: `modules/impl/glide_stats/GlideStatsModule.java`
- Package: `de.snenjih.noctra.modules.impl.glide_stats`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Zeigt ein Echtzeit-HUD mit Flugdaten während des Elytra-Gleitens. Angezeigt werden
horizontale Geschwindigkeit, vertikale Geschwindigkeit (positiv = Aufstieg, negativ = Abstieg),
aktuelle Höhe (Y-Koordinate) und der aktuelle Pitch-Winkel. Alle Werte werden jeden Tick
aus dem Spieler-Zustand gelesen und geglättet angezeigt.

Nutzen: Elytra-Piloten können damit Reiserouten optimieren (Boost-Zeitpunkt ermitteln,
Landepunkte abschätzen, optimalen Gleitwinkel halten) ohne externe Tools.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `always_show` | Boolean | `false` | `true / false` | "Always Show" | HUD auch anzeigen wenn nicht am Gleiten. Nützlich zum Kalibrieren. |
| `hud_x` | Int | `4` | `0 – 640` | "HUD X" | Linke Kante des HUD in Skalenpixeln vom linken Bildschirmrand. |
| `hud_y` | Int | `4` | `0 – 480` | "HUD Y" | Obere Kante des HUD in Skalenpixeln vom oberen Bildschirmrand. |
| `show_background` | Boolean | `true` | `true / false` | "Show Background" | Halbtransparenten Hintergrund hinter dem HUD zeichnen. |

## Implementation

### Event Hooks

- `onClientTick(MinecraftClient client)` — Geschwindigkeitsvektoren jeden Tick aus
  `player.getVelocity()` lesen und in `blocks/s` umrechnen (Faktor × 20).
- `onRenderHud(DrawContext ctx, float tickDelta)` — HUD auf dem Bildschirm zeichnen.

### Required Mixins

Kein Mixin erforderlich. Alle Werte sind direkt über `client.player` lesbar.

### Core Algorithm

```
Felder im Modul:
  private double smoothHSpeed = 0.0;
  private double smoothVSpeed = 0.0;
  private double lastY        = 0.0;
  private boolean wasGliding  = false;

1. In onClientTick(client):
   a. Wenn client.player == null → return
   b. Vec3d vel = player.getVelocity()
   c. double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20.0  // blocks/s horizontal
   d. double vSpeed = vel.y * 20.0  // blocks/s vertikal (positiv = aufsteigend)
   e. Smoothing (exponentieller Glättungsfilter, α = 0.3):
        smoothHSpeed = smoothHSpeed * 0.7 + hSpeed * 0.3
        smoothVSpeed = smoothVSpeed * 0.7 + vSpeed * 0.3
   f. Werte in Instanz-Feldern speichern für Render-Phase

2. In onRenderHud(ctx, tickDelta):
   a. Wenn client.player == null → return
   b. Wenn !always_show.get() UND !player.isGliding() → return
   c. Zeilen-Inhalte zusammenbauen:
        String line1 = String.format("H-Speed: %.1f b/s", smoothHSpeed)
        String line2 = String.format("V-Speed: %+.1f b/s", smoothVSpeed)  // + Zeichen bei positivem Wert
        String line3 = String.format("Y: %.1f", player.getY())
        String line4 = String.format("Pitch: %.1f°", player.getPitch())
   d. int x = hud_x.get(), y = hud_y.get()
   e. Wenn show_background.get():
        int w = 90, h = 4 * 10 + 4  // 4 Zeilen × (Zeilenhöhe 9 + Gap 1) + Padding
        ctx.fill(x - 2, y - 2, x + w, y + h, 0x88000000)  // halbtransparentes Schwarz
   f. Zeilen rendern:
        for (int i = 0; i < lines.length; i++) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(lines[i]), x, y + i * 10, 0xFFFFFF)
        }
```

### Geschwindigkeits-Physik

Minecraft läuft mit 20 Ticks pro Sekunde. `getVelocity()` gibt blocks/tick zurück.
Multiplikation mit 20 ergibt blocks/s.

Typische Werte beim Elytra-Gleiten:
- Normales Gleiten nach Absprung: 10–20 b/s horizontal
- Nach einem Feuerwerk-Boost: 30–40 b/s horizontal
- Optimaler Gleitwinkel (-29° pitch): ~0.5–1 b/s Höhenverlust pro Block horizontal

`v.y` ist negativ beim Fallen. Das Format `%+.1f` zeigt `+0.5` oder `-1.2` an, was die
Richtung intuitiv ablesbar macht.

### HUD-Layout (Referenz)

```
[Hintergrund-Box]
H-Speed: 28.3 b/s
V-Speed: -1.1 b/s
Y:       73.0
Pitch:   -29.4°
```

Breite der Box: ~90 Skalenpixel. Schriftgröße ist immer Vanilla-Standard (Minecraft-Font,
8px Zellhöhe + 1px Zeilenabstand = 9px pro Zeile).

### Edge Cases

- **Spieler noch nicht eingeloggt** (`client.player == null`): Früher Return in beiden Hooks
- **`always_show = false` und nicht am Gleiten**: Werte werden weiter getickt (smoothing
  läuft weiter), aber nicht angezeigt — so ist der erste angezeigte Wert schon plausibel
- **HUD außerhalb Bildschirmgrenzen**: Werte werden nicht geclampt; User ist verantwortlich.
  Zur Sicherheit könnte man `Math.min(hud_x, screenW - 100)` anwenden, aber das macht die
  Einstellung unintuitiv
- **Hohe Velocity-Spitzen bei Absturz/Explosion**: Smoothing dämpft Ausreißer visuell
- **Spectator/Creative**: Gleiter-Logik gilt genauso; `isGliding()` korrekt für
  Creative-Elytra-Gleiter
- **InGameHud deaktiviert** (`F1`-Modus): HUD-Callbacks werden in diesem Modus vom Fabric
  HudElementRegistry trotzdem aufgerufen — aber HudElementRegistry-Events respektieren
  `client.options.hudHidden`? Zur Sicherheit Guard:
  `if (client.options.hudHidden) return;`

## Translation Keys

```json
"noctra.glide_stats.name": "Glide Stats",
"noctra.glide_stats.description": "Shows real-time speed, altitude, and pitch while gliding.",
"noctra.glide_stats.always_show": "Always Show",
"noctra.glide_stats.hud_x": "HUD X",
"noctra.glide_stats.hud_y": "HUD Y",
"noctra.glide_stats.show_background": "Show Background"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/glide_stats.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Kleines Tachometer- oder Diagramm-Symbol (Balkendiagramm mit aufsteigenden Balken),
alternativ ein Elytra-Silhouette mit einem kleinen Blitz daneben. Stil: Pixel-Art, weiße Linien
auf transparentem oder dunklem Hintergrund.
