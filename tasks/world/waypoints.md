# Waypoints

**ID:** `waypoints`  
**Category:** WORLD  
**Status:** [x] DONE  
**Class:** `modules/impl/waypoints/WaypointsModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.waypoints`

## System Notes (Updated)

- Module Ordner: `modules/impl/waypoints/WaypointsModule.java`
- Package: `de.snenjih.noctra.modules.impl.waypoints`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`
- `WaypointConfig`-Klasse im selben Unterordner (`modules/impl/waypoints/WaypointConfig.java`)

## Description

Allows the player to save named locations (waypoints) and display them persistently as HUD markers while in the world. Each waypoint shows its name, distance, and a directional arrow on the HUD. An optional 3D beacon column is drawn in the world at each waypoint location for quick visual identification at distance.

Waypoints are dimension-aware: only waypoints belonging to the current dimension are shown. A maximum of 20 waypoints can be stored. Color is configurable per waypoint at creation time.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Description |
|---|---|---|---|---|---|
| `show_beacons` | Boolean | `true` | — | "Show 3D Beacons" | Draws a vertical colored line/column in the world at each waypoint position. |
| `max_distance` | Int | `2000` | `100–10000` | "Max Render Distance (m)" | Waypoints further than this distance (in blocks) are hidden from the HUD. |
| `hud_x` | Int | `8` | `0–480` | "HUD X" | Left-edge X position of the waypoint list on the HUD. |
| `hud_y` | Int | `60` | `0–240` | "HUD Y" | Top-edge Y position of the waypoint list on the HUD. |

## External Config

Waypoints are stored in a separate file so they survive module toggles and mod updates without polluting `mandatory.json`.

- **File:** `config/mandatory_waypoints.json`
- **Format:**
  ```json
  [
    {
      "name": "Home",
      "x": 128,
      "y": 64,
      "z": -512,
      "color": "#FF5555",
      "dimension": "minecraft:overworld"
    }
  ]
  ```
- **Fields:**
  - `name` — display label (max 32 chars, trimmed on save)
  - `x`, `y`, `z` — block coordinates (integers)
  - `color` — hex string, rendered as ARGB with full opacity (`#RRGGBB` → `0xFF` + RGB)
  - `dimension` — `world.getRegistryKey().getValue().toString()`, e.g. `"minecraft:overworld"`, `"minecraft:the_nether"`, `"minecraft:the_end"`

- **Zugriff:** Dedizierte Klasse `WaypointConfig` (im Modul-Package oder in `config/`) mit Gson-basiertem Laden/Speichern. `WaypointConfig.load()` beim `onEnable()` und `onJoinWorld()` aufrufen; `WaypointConfig.save()` nach jeder Mutation.

## Implementation

### Event Hooks

- `onEnable()` — Lade `WaypointConfig`; registriere `.wp`-Sub-Command-Handler über `ChatCommandDispatcher` (via `onSendChat`).
- `onDisable()` — Entferne nichts aus dem Registry; Commands werden nur im `onSendChat`-Hook verarbeitet (der Hook ist automatisch inaktiv wenn das Modul deaktiviert ist).
- `onJoinWorld(ClientWorld world)` — Setze `currentDimension` und filtere die aktive Waypoint-Liste.
- `onLeaveWorld()` — Setze `currentDimension = null`; leere die aktive Waypoint-Liste.
- `onClientTick(MinecraftClient client)` — Berechne Abstände und Richtungen neu (gecacht, alle 10 Ticks).
- `onRenderHud(DrawContext ctx, float tickDelta)` — Zeichne Waypoint-Liste mit Name, Distanz, Pfeil.
- `onRenderWorld(WorldRenderContext ctx)` — Zeichne 3D-Beacon-Linien (wenn `show_beacons` aktiv).
- `onSendChat(String message)` — Fange `.wp`-Befehle ab; return `ActionResult.SUCCESS` wenn verarbeitet.

### Required Mixins

Kein Mixin erforderlich. Chat-Commands werden über den bestehenden `ChatCommandDispatcher`-Mechanismus via `onSendChat` abgefangen.

### Core Algorithm

```
// onClientTick (alle 10 Ticks):
if (tickCounter++ % 10 != 0) return
player = client.player
if player == null || world == null → return
Vec3d playerPos = player.getPos()

activeWaypoints = waypointList.filter(w → w.dimension == currentDimension)
for each waypoint w in activeWaypoints:
  dx = w.x - playerPos.x
  dz = w.z - playerPos.z
  w.cachedDistance = sqrt(dx*dx + (w.y - playerPos.y)^2 + dz*dz)
  w.cachedAngle = atan2(dz, dx)  // in radians, world-east = 0

// onRenderHud:
if activeWaypoints.isEmpty() || player == null → return
playerYaw = player.getYaw()  // degrees, -180 to +180

y = hud_y.get()
for each w in activeWaypoints (sorted by distance, ascending):
  if w.cachedDistance > max_distance.get() → skip

  // Directional arrow: angle relative to player facing
  relativeAngle = w.cachedAngle - toRadians(playerYaw + 90)
  arrowChar = angleToArrow(relativeAngle)  // see below
  label = arrowChar + " " + w.name + " §7" + formatDistance(w.cachedDistance)
  ctx.drawTextWithShadow(textRenderer, Text.literal(label), hud_x.get(), y, w.colorArgb)
  y += 10

angleToArrow(radians) → char:
  // Normalize to 0–2π, split into 8 sectors of 45°
  // N=↑  NE=↗  E=→  SE=↘  S=↓  SW=↙  W=←  NW=↖

formatDistance(blocks):
  if blocks >= 1000 → return String.format("%.1fkm", blocks / 1000.0)
  return (int)blocks + "m"

// onRenderWorld (Beacon-Linien):
if !show_beacons.get() → return
Camera camera = ctx.camera()
Vec3d camPos = camera.getPos()
MatrixStack matrices = ctx.matrixStack()
VertexConsumerProvider consumers = ctx.consumers()

for each w in activeWaypoints:
  if w.cachedDistance > max_distance.get() → skip
  relX = w.x + 0.5 - camPos.x
  relZ = w.z + 0.5 - camPos.z
  bottomY = w.y - camPos.y
  topY    = bottomY + 64  // column height: 64 blocks tall upward

  // Linie von bottom bis top (schlanke Box: 0.1 breit)
  WorldRenderer.drawBox(matrices,
      consumers.getBuffer(RenderLayer.LINES),
      relX - 0.05, bottomY, relZ - 0.05,
      relX + 0.05, topY,    relZ + 0.05,
      r, g, b, 0.8f)
```

### Chat Commands (via `onSendChat`)

Das Modul überschreibt `onSendChat` und fängt `.wp`-Befehle ab:

| Befehl | Beschreibung |
|---|---|
| `.wp add <name>` | Fügt Waypoint an aktueller Spielerposition hinzu (Dimension + Farbe automatisch). Farbe wird via Zufalls-Rotation aus einer Palette von 8 Farben vergeben. |
| `.wp add <name> <#color>` | Fügt Waypoint mit expliziter Hex-Farbe hinzu. |
| `.wp remove <name>` | Entfernt den ersten Waypoint mit diesem Namen in der aktuellen Dimension. |
| `.wp list` | Zeigt alle Waypoints der aktuellen Dimension im Chat an (Name, Koordinaten, Distanz). |
| `.wp clear` | Entfernt alle Waypoints der aktuellen Dimension (mit Bestätigungs-Feedback). |

Rückgabe `ActionResult.SUCCESS` verhindert, dass die Nachricht an den Server gesendet wird.

### Performance Notes

- **Tick-Rate:** Distanz/Winkel-Berechnung nur alle 10 Ticks (0.5 s). Für maximal 20 Waypoints ist das vernachlässigbar.
- **Kein Block-Scan:** Waypoints sind manuelle Koordinaten — keine Chunk-Iteration nötig.
- **3D-Rendering:** Nur `consumers.getBuffer(RenderLayer.LINES)` verwenden; keine eigene Matrix-Transformation nötig solange Koordinaten kamera-relativ übergeben werden.
- **Distanz-Cull:** `max_distance`-Setting verhindert, dass entfernte Waypoints HUD und World-Render befüllen.

### Edge Cases

- **Welt noch nicht geladen:** `world == null` → alle Hooks returnen sofort; `onLeaveWorld()` leert die aktive Liste.
- **Nether-Koordinaten:** Waypoints speichern Raw-Koordinaten. Im Nether sind X/Z-Abstände 8× kleiner. Keine automatische Skalierung — der Nutzer gibt Nether-Koordinaten ein.
- **Name mit Leerzeichen:** `.wp add My Base` — Name wird als alles nach dem ersten Wort bis zum optionalen Farb-Argument zusammengesetzt. Implementierung: `split(" ", 3)` und `parts[2]` als Name falls vorhanden.
- **Duplikate:** Gleicher Name + gleiche Dimension ist erlaubt (Liste, kein Map). Beim Remove wird der erste Treffer entfernt.
- **Limit 20:** Bei `.wp add` wenn bereits 20 Waypoints in der Dimension: Notification-Fehler, kein Hinzufügen.
- **Ungültige Farbe:** Wenn `#color`-Parsing fehlschlägt → Standard-Farbe `#55FF55` verwenden und Warnung ausgeben.
- **Spieler unter Waypoint:** Distance = 0 → Pfeil zeigt nach oben (`↑`) oder zeigt Y-Differenz an.

## Translation Keys

```json
"noctra.waypoints.name": "Waypoints",
"noctra.waypoints.description": "Save locations and display them as HUD markers.",
"noctra.waypoints.show_beacons": "Show 3D Beacons",
"noctra.waypoints.max_distance": "Max Render Distance",
"noctra.waypoints.hud_x": "HUD X",
"noctra.waypoints.hud_y": "HUD Y",
"noctra.waypoints.cmd.added": "Waypoint added: %s",
"noctra.waypoints.cmd.removed": "Waypoint removed: %s",
"noctra.waypoints.cmd.not_found": "Waypoint not found: %s",
"noctra.waypoints.cmd.limit": "Waypoint limit reached (max 20).",
"noctra.waypoints.cmd.cleared": "All waypoints cleared for this dimension."
```

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/waypoints.png`  
**Size:** 32×32 PNG  
**Suggestion:** Ein stilisierter Standort-Pin (Tropfenform, unten spitz) mit einem weißen Kreuz oder Stern im Inneren auf dunklem Hintergrund. Alternativ: Kompassnadel + kleiner Stern in der Ecke.
