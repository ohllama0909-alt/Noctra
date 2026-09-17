# Cave Finder

**ID:** `cave_finder`  
**Category:** WORLD  
**Status:** [x] DONE  
**Class:** `modules/impl/cave_finder/CaveFinderModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.cave_finder`

## System Notes (Updated)

- Module Ordner: `modules/impl/cave_finder/CaveFinderModule.java`
- Package: `de.snenjih.noctra.modules.impl.cave_finder`
- Implementiert HudElement: Nein (Welt-Rendering via `onRenderWorld`)
- Verwendet `WorldRenderContext` aus `net.fabricmc.fabric.api.client.rendering.v1.world`

## Description

Locates the nearest cave or air pocket beneath and around the player, then displays its depth and horizontal direction on the HUD. Useful for quickly finding resource-bearing cave systems without digging blindly.

The module uses two complementary strategies:

1. **Vertical Raycast:** Scans straight down from the player's position to find the nearest air gap directly below. Fast and reliable in flat terrain.
2. **Horizontal Search (optional):** Scans a configurable radius for air pockets at lower Y levels to find caves that are not directly below the player.

Results are displayed as a compact HUD element showing depth (blocks down) and a directional arrow toward the best cave candidate. An optional 3D marker (downward arrow or colored sphere via a short WorldRenderer line) can be drawn above the cave entrance.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Description |
|---|---|---|---|---|---|
| `scan_interval` | Int | `20` | `1–60` | "Scan Interval (ticks)" | Ticks between re-scans. Lower = more responsive; higher = better performance. |
| `radius` | Int | `32` | `16–64` | "Search Radius (blocks)" | Horizontal radius to scan for air pockets. |
| `min_cave_size` | Int | `4` | `1–20` | "Min Cave Size (blocks)" | Minimum number of connected air blocks to count as a cave. Filters out single-block holes. |
| `show_3d_marker` | Boolean | `true` | — | "Show 3D Marker" | Draws a colored vertical line above the detected cave entrance in the world. |
| `hud_x` | Int | `8` | `0–480` | "HUD X" | X position of the HUD element. |
| `hud_y` | Int | `8` | `0–240` | "HUD Y" | Y position of the HUD element. |
| `search_mode` | Enum | `BOTH` | `DOWN_ONLY, RADIUS, BOTH` | "Search Mode" | Controls which strategy is used: vertical-only, radius-only, or both combined. |

## Implementation

### Event Hooks

- `onEnable()` — Leert den Cache; setzt `lastResult = null`.
- `onDisable()` — Setzt `lastResult = null`.
- `onClientTick(MinecraftClient client)` — Führt die Cave-Suche durch (alle `scan_interval` Ticks). Schreibt `lastResult`.
- `onRenderHud(DrawContext ctx, float tickDelta)` — Zeigt HUD-Text mit Tiefe und Richtung.
- `onRenderWorld(WorldRenderContext ctx)` — Zeichnet 3D-Marker über der gefundenen Höhle (wenn aktiv).
- `onJoinWorld(ClientWorld world)` — Setzt `lastResult = null`.
- `onLeaveWorld()` — Setzt `lastResult = null`.

### Required Mixins

Kein Mixin erforderlich.

### Data Structure

```java
record CaveResult(
    BlockPos cavePos,      // Position des obersten Luft-Blocks der Höhle
    int      depth,        // Tiefe in Blöcken unterhalb des Spielers (immer >= 0)
    int      caveSize,     // Anzahl verbundener Luft-Blöcke (aus BFS)
    double   angle         // Horizontaler Winkel zur Höhle (Radians, world-east = 0)
) {}

CaveResult lastResult = null;
int tickCounter = 0;
```

### Core Algorithm

```
// onClientTick:
if (tickCounter++ % scan_interval.get() != 0) return
world  = client.world
player = client.player
if world == null || player == null → return

BlockPos playerPos = player.getBlockPos()
SearchMode mode = search_mode.get()

CaveResult best = null

// --- Strategie 1: Vertikaler Raycast nach unten ---
if mode == DOWN_ONLY || mode == BOTH:
  best = scanDown(world, playerPos)

// --- Strategie 2: Radius-Suche ---
if mode == RADIUS || mode == BOTH:
  CaveResult radiusResult = scanRadius(world, playerPos)
  if radiusResult != null:
    if best == null || radiusResult.caveSize > best.caveSize:
      best = radiusResult  // Bevorzuge größere Höhle

lastResult = best

// --- scanDown(world, playerPos): ---
scanDown:
  for dy in 1..200:  // max 200 Blöcke nach unten
    checkPos = playerPos.down(dy)
    if checkPos.getY() < world.getBottomY() → break
    
    state = world.getBlockState(checkPos)
    if state.isAir():
      size = floodFillSize(world, checkPos, min_cave_size.get() * 3)  // BFS mit Cap
      if size >= min_cave_size.get():
        return new CaveResult(checkPos, dy, size, 0.0)  // angle = 0 (direkt unten)
  
  return null

// --- scanRadius(world, playerPos): ---
scanRadius:
  best = null
  int r = radius.get()
  
  for dx in -r..r (Schritt 4):  // Gitter-Sampling: alle 4 Blöcke (Performance-Kompromiss)
    for dz in -r..r (Schritt 4):
      for dy in -4..-100 (Schritt 4):  // Von Spieler-Y abwärts, bis 100 Blöcke tiefer
        checkY = playerPos.getY() + dy
        if checkY < world.getBottomY() → break
        
        pos = new BlockPos(playerPos.getX() + dx, checkY, playerPos.getZ() + dz)
        state = world.getBlockState(pos)
        if !state.isAir() → continue
        
        size = floodFillSize(world, pos, min_cave_size.get() * 3)
        if size < min_cave_size.get() → continue
        
        depth = playerPos.getY() - checkY
        dx_d  = (double)(playerPos.getX() + dx) - player.getX()
        dz_d  = (double)(playerPos.getZ() + dz) - player.getZ()
        angle = atan2(dz_d, dx_d)
        
        candidate = new CaveResult(pos, depth, size, angle)
        if best == null || size > best.caveSize:
          best = candidate
  
  return best

// --- floodFillSize(world, start, cap): ---
// BFS um Luft-Blöcke zu zählen; stoppt bei `cap` um unbegrenzte Ausbreitung zu verhindern
floodFillSize(world, start, cap):
  visited = HashSet
  queue   = ArrayDeque
  queue.add(start)
  visited.add(start)
  count = 0
  
  while !queue.isEmpty() && count < cap:
    current = queue.poll()
    count++
    for each neighbor in 6-way adjacency (±X, ±Y, ±Z):
      if visited.contains(neighbor) → skip
      if neighbor.getY() < world.getBottomY() || neighbor.getY() > world.getTopY() → skip
      if world.getBlockState(neighbor).isAir():
        visited.add(neighbor)
        queue.add(neighbor)
  
  return count

// onRenderHud:
if lastResult == null → return
player = MinecraftClient.getInstance().player
if player == null → return

String depthStr = "Cave: " + lastResult.depth + "m below"
String sizeStr  = " (" + lastResult.caveSize + "+ blocks)"
String arrow    = (lastResult.angle == 0.0 && lastResult.depth > 0)
                    ? "↓"
                    : angleToArrow(lastResult.angle - toRadians(player.getYaw() + 90))

ctx.drawTextWithShadow(textRenderer,
    Text.literal(arrow + " " + depthStr + sizeStr),
    hud_x.get(), hud_y.get(), 0xFFAA66FF)  // lila

// onRenderWorld:
if !show_3d_marker.get() || lastResult == null → return
Camera camera = ctx.camera()
Vec3d camPos  = camera.getPos()
MatrixStack matrices = ctx.matrixStack()
VertexConsumerProvider consumers = ctx.consumers()

BlockPos cave = lastResult.cavePos
double relX = cave.getX() + 0.5 - camPos.x
double relY = cave.getY() - camPos.y
double relZ = cave.getZ() + 0.5 - camPos.z

// Senkrechte Linie 8 Blöcke nach oben über der Höhle (fungiert als Marker-Beacon)
WorldRenderer.drawBox(matrices, consumers.getBuffer(RenderLayer.LINES),
    relX - 0.1, relY,     relZ - 0.1,
    relX + 0.1, relY + 8, relZ + 0.1,
    0.67f, 0.4f, 1.0f, 0.9f)  // lila
```

### Performance Notes

- **scanDown:** O(200) Blocks pro Scan — vernachlässigbar.
- **scanRadius (Gitter-Sampling):** Statt alle Blöcke zu prüfen wird ein 4-Block-Gitter verwendet. Bei Radius 32 und 4er-Schritt: 17×17×26 = 7514 Positionen. Pro Position: 1 `getBlockState()` + bei Treffer 1 BFS mit Cap. Vertretbar bei `scan_interval = 20`.
- **BFS-Cap:** `cap = min_cave_size * 3` begrenzt die BFS. Bei Default `min_cave_size = 4` → cap = 12. Damit kann die BFS maximal 12 Blöcke besuchen. Das verhindert, dass eine riesige Höhle die gesamte Scan-Zeit frisst.
- **Nur ein Ergebnis:** Es wird die beste (größte) Höhle gespeichert, nicht alle. Das hält den Speicherverbrauch minimal.
- **Keine Chunk-Validierung nötig für `isAir()`:** `world.getBlockState()` gibt `Blocks.VOID_AIR` für nicht geladene Chunks zurück; `isAir()` gibt dafür `true` zurück. Das kann Fehlalarme erzeugen. Fix: `world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)` vor jedem `getBlockState()`-Aufruf — erhöht aber den Overhead. Kompromiss: Nur bei `scanRadius` prüfen (da dort viele Positionen außerhalb des geladenen Bereichs liegen könnten).

### Edge Cases

- **Spieler bereits in einer Höhle:** `scanDown` findet sofort bei `dy=0` Luft → Tiefe 0. Das Modul zeigt dann "0m below" — korrekt und nützlich (Spieler ist bereits in einer Höhle).
- **Nether:** Die Bedrock-Decke bei Y=127–255 enthält keine Luft. `scanDown` trifft auf Bedrock und findet nichts bis zur Bedrock-Untergrenze. `scanRadius` kann trotzdem seitliche Lava-Seen oder offene Bereiche finden. Kein spezieller Nether-Modus nötig.
- **Superflat-Welten:** Keine Höhlen vorhanden. `lastResult` bleibt `null`; HUD zeigt nichts.
- **End:** Ähnlich wie Superflat in weiten Bereichen. Die End-Islands haben keine Höhlen. `lastResult = null`.
- **Spieler unter Y=0 (Tiefenschichten):** `scanDown` scannt von Spieler-Y aus abwärts — funktioniert auch bei negativen Y-Koordinaten.
- **Welt noch nicht geladen:** `world == null` → früher Return in `onClientTick`.
- **Concurrency:** Alle Lese- und Schreibzugriffe auf `lastResult` erfolgen im Client-Thread (Tick-Event und Render-Event sind synchron). Kein Locking nötig.

## Translation Keys

```json
"noctra.cave_finder.name": "Cave Finder",
"noctra.cave_finder.description": "Locates nearby caves and shows their depth and direction on the HUD.",
"noctra.cave_finder.scan_interval": "Scan Interval",
"noctra.cave_finder.radius": "Search Radius",
"noctra.cave_finder.min_cave_size": "Min Cave Size",
"noctra.cave_finder.show_3d_marker": "Show 3D Marker",
"noctra.cave_finder.hud_x": "HUD X",
"noctra.cave_finder.hud_y": "HUD Y",
"noctra.cave_finder.search_mode": "Search Mode"
```

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/cave_finder.png`  
**Size:** 32×32 PNG  
**Suggestion:** Ein nach unten zeigender Pfeil der in eine stilisierte Höhlen-Silhouette (dunkle Öffnung im Fels) mündet. Alternativ: Eine Draufsicht mit einem Spieler-Symbol oben und einer gestrichelten Linie die nach unten zu einem Höhlen-Symbol führt — symbolisiert Tiefe und Richtung.
