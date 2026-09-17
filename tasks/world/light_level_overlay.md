# Light Level Overlay

**ID:** `light_level_overlay`  
**Category:** WORLD  
**Status:** [x] DONE  
**Class:** `modules/impl/light_level_overlay/LightLevelOverlayModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.light_level_overlay`

## System Notes (Updated)

- Module Ordner: `modules/impl/light_level_overlay/LightLevelOverlayModule.java`
- Package: `de.snenjih.noctra.modules.impl.light_level_overlay`
- Implementiert HudElement: Nein (Welt-Rendering via `onRenderWorld`)
- Verwendet `WorldRenderContext` aus `net.fabricmc.fabric.api.client.rendering.v1.world`

## Description

Renders the raw light level (0–15) directly on the top surface of blocks within a configurable radius. Primarily a mob-spawn warning tool: blocks with total light level 0–7 allow monster spawning (shown in red), 8–11 are borderline (shown in yellow), and 12–15 are safe (shown in green). Only blocks where mobs can actually spawn are considered — the block surface must be solid and the block directly above must be air. An optional toggle limits display to only dangerous/borderline blocks so the overlay is not cluttered in well-lit areas.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Description |
|---|---|---|---|---|---|
| `radius` | Int | `12` | `1–20` | "Radius (blocks)" | How many blocks in each horizontal direction to scan around the player. Vertical range is always ±4 blocks. |
| `only_dangerous` | Boolean | `false` | — | "Only Dangerous Blocks" | When true, only shows blocks with light level 0–11 (red + yellow). Green safe blocks are hidden. |
| `use_block_light` | Boolean | `false` | — | "Use Block Light Only" | When true, shows block-light level only (ignores sky light). Useful underground where sky light is always 0. When false, uses the combined `getLightLevel()` which returns max(sky, block). |
| `scan_interval` | Int | `10` | `1–40` | "Scan Interval (ticks)" | How many ticks between full block scans. Lower = more responsive; higher = better performance. |

## Implementation

### Event Hooks

- `onEnable()` — Leert den Cache; startet den Scan-Timer.
- `onDisable()` — Leert den Block-Cache, setzt `tickCounter = 0`.
- `onClientTick(MinecraftClient client)` — Führt den Block-Scan durch (gecacht, alle `scan_interval` Ticks). Aktualisiert `lightCache`.
- `onRenderWorld(WorldRenderContext ctx)` — Rendert für jeden gecachten Block die Licht-Zahl (oder Farbquadrat) auf dessen Oberfläche als 3D-Text im Weltkoordinatensystem.
- `onJoinWorld(ClientWorld world)` — Leert den Cache.
- `onLeaveWorld()` — Leert den Cache.

### Required Mixins

Kein Mixin erforderlich.

### Data Structure

```java
record LightEntry(BlockPos pos, int level) {}
List<LightEntry> lightCache = new ArrayList<>();  // Ergebnis des letzten Scans
```

### Core Algorithm

```
// onClientTick:
if (tickCounter++ % scan_interval.get() != 0) return
client = MinecraftClient.getInstance()
world = client.world
player = client.player
if world == null || player == null → return

lightCache.clear()
center = player.getBlockPos()
radius = radius.get()

for dx in -radius..radius:
  for dz in -radius..radius:
    for dy in -4..4:
      pos = center.add(dx, dy, dz)
      
      // Spawn-Bedingung prüfen:
      // 1. Block darunter (pos selbst) muss solid sein (Mob steht auf pos.up())
      //    Eigentlich: Mob spawnt auf pos, steht auf pos (top surface), braucht Luft bei pos und pos.up()
      //    Genauer: pos ist Boden-Block → prüfe pos.up() auf Luft
      
      floorState = world.getBlockState(pos)
      aboveState = world.getBlockState(pos.up())
      
      // Block muss begehbar sein: Ober-Fläche solid + Block oben drüber ist Luft
      if !floorState.isSolidBlock(world, pos) → continue
      if !aboveState.isAir() → continue
      
      // Lichtwert des Luft-Blocks direkt über dem Boden-Block
      checkPos = pos.up()
      int light
      if use_block_light.get():
        light = world.getLightLevel(LightType.BLOCK, checkPos)
      else:
        light = world.getLightLevel(checkPos)  // max(sky, block)
      
      if only_dangerous.get() && light >= 12 → continue
      
      lightCache.add(new LightEntry(pos, light))

// onRenderWorld:
if lightCache.isEmpty() → return
MatrixStack matrices = ctx.matrixStack()
VertexConsumerProvider consumers = ctx.consumers()
Camera camera = ctx.camera()
Vec3d camPos = camera.getPos()
TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer

for each entry in lightCache:
  int color = colorForLevel(entry.level)
  // Text auf Oberseite des Blocks rendern
  // Position: Mitte des Blocks (x+0.5, y+1.01, z+0.5), leicht über der Oberfläche
  double relX = entry.pos.getX() + 0.5 - camPos.x
  double relY = entry.pos.getY() + 1.01 - camPos.y
  double relZ = entry.pos.getZ() + 0.5 - camPos.z
  
  // Distanz-Cull: Text ab 20 Blöcken Entfernung nicht rendern (zu klein)
  double dist = sqrt(relX*relX + relY*relY + relZ*relZ)
  if dist > 20.0 → continue
  
  matrices.push()
  matrices.translate(relX, relY, relZ)
  
  // Billboard: zur Kamera drehen (Euler-Rotation aus Kamera-Orientierung)
  Quaternionf rotation = camera.getRotation()
  matrices.multiply(rotation)
  
  // Text ist Y-up, aber Welt-Rendering ist unterschiedlich → spiegeln
  matrices.scale(-0.025f, -0.025f, 0.025f)
  
  String label = String.valueOf(entry.level)
  float textX = -textRenderer.getWidth(label) / 2.0f
  
  // Verwende VertexConsumerProvider für In-World-Text
  textRenderer.draw(label, textX, 0, color, false,
      matrices.peek().getPositionMatrix(),
      consumers, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0)
  
  matrices.pop()

colorForLevel(int level) → int ARGB:
  if level <= 7  → return 0xFFFF4444  // rot
  if level <= 11 → return 0xFFFFAA00  // gelb/orange
  else           → return 0xFF44FF44  // grün
```

### Performance Notes

- **Block-Iteration:** Bei Radius 12 und ±4 vertikal: 25×25×9 = 5625 Positionen pro Scan. Mit `scan_interval = 10` sind das 562 Checks/Tick im Durchschnitt — akzeptabel.
- **Radius-Limit 20:** Bei Radius 20 sind es 41×41×9 = 15129 Positionen. Das ist die Obergrenze; daher `scan_interval` entsprechend höher empfehlen.
- **Cache-Strategie:** Der vollständige Scan ersetzt `lightCache` atomar. Kein inkrementelles Update. Der List-Aufbau ist im Client-Tick-Thread; das Rendering liest die Liste im Render-Thread. Da Minecraft single-threaded ist (Client-Tick und Rendering laufen nicht gleichzeitig), ist kein Lock nötig.
- **Text-Rendering-Kosten:** Pro sichtbarem Block ein `textRenderer.draw()`-Call. Bei Radius 12 und vielen offenen Höhlen können das mehrere hundert Calls sein. Das Distanz-Cull (>20 Blöcke überspringen) begrenzt die tatsächliche Anzahl gerendeter Labels erheblich.
- **`isSolidBlock()` vs. `isOpaque()`:** `isSolidBlock(world, pos)` ist korrekt für Spawn-Prüfung; Slab-Oberseiten usw. werden korrekt erkannt.

### Edge Cases

- **Welt null:** `world == null` → Scan überspringen; Cache bleibt aus letztem Scan erhalten (unschädlich da keine Welt). `onJoinWorld`/`onLeaveWorld` leeren den Cache.
- **Spieler im Nether:** Der Nether-Decken-Bereich (y=127–255) enthält massive Bedrock-Decken. Dort findet der Scan viele solid Blocks mit 0 Light → roter Alarm überall. Vertikales Clipping auf `pos.getY() >= world.getBottomY()` und `pos.getY() < world.getTopY() - 1` schützt vor Out-of-Bounds.
- **Flüssigkeits-Oberflächen:** Wasser/Lava als "solid" — `isSolidBlock()` gibt für Flüssigkeiten in der Regel `false` zurück, daher automatisch gefiltert.
- **Spawn-geschützte Blöcke:** Das Modul zeigt nur Lichtwerte, keine tatsächlichen Spawn-Regeln (kein Biom-Check, kein Chunk-Owner-Check). Das ist die Standard-Erwartung für diesen Overlay-Typ.
- **Spieler bewegt sich:** Der Cache ist bis zum nächsten Scan-Intervall statisch. Bei schneller Bewegung kann der Overlay kurz hinter dem Spieler zurückbleiben. Das ist bei `scan_interval = 10` (0.5 s) kaum spürbar.

## Translation Keys

```json
"noctra.light_level_overlay.name": "Light Level Overlay",
"noctra.light_level_overlay.description": "Shows light levels on blocks to warn about mob spawn locations.",
"noctra.light_level_overlay.radius": "Radius",
"noctra.light_level_overlay.only_dangerous": "Only Dangerous Blocks",
"noctra.light_level_overlay.use_block_light": "Use Block Light Only",
"noctra.light_level_overlay.scan_interval": "Scan Interval"
```

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/light_level_overlay.png`  
**Size:** 32×32 PNG  
**Suggestion:** Ein Glühsymbol (Glühbirne oder Sonne) mit einer kleinen roten Warnung (Ausrufezeichen oder Totenkopf) in der unteren rechten Ecke. Alternativ: Ein Block mit der Zahl "0" in Rot darauf, und rechts daneben ein Block mit "15" in Grün — symbolisiert das Lichtwert-Spektrum.
