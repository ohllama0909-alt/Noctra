# Chest Highlight

**ID:** `chest_highlight`  
**Category:** WORLD  
**Status:** [x] DONE  
**Class:** `modules/impl/chest_highlight/ChestHighlightModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.chest_highlight`

## System Notes (Updated)

- Module Ordner: `modules/impl/chest_highlight/ChestHighlightModule.java`
- Package: `de.snenjih.noctra.modules.impl.chest_highlight`
- Implementiert HudElement: Nein (Welt-Rendering via `onRenderWorld`)
- Verwendet `WorldRenderContext` aus `net.fabricmc.fabric.api.client.rendering.v1.world`

## Description

Draws colored wireframe outlines around storage containers within a configurable radius, making it easy to spot chests, barrels, and shulker boxes through terrain. Each container type has its own configurable color. An "X-ray" option controls whether outlines are drawn even when the block is occluded by geometry (always visible) or only when there is a direct line of sight. Supports Chest, Trapped Chest, Barrel, and all Shulker Box color variants.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Description |
|---|---|---|---|---|---|
| `radius` | Int | `32` | `8–64` | "Search Radius (blocks)" | Maximum distance in blocks to scan for containers. Applied as a cube (±radius in X, Y, Z). |
| `scan_interval` | Int | `20` | `5–60` | "Scan Interval (ticks)" | Ticks between full re-scans. Lower = more responsive but more expensive. |
| `xray` | Boolean | `true` | — | "Show Through Walls" | When true, outlines are always rendered (no depth test). When false, only containers with direct line of sight are shown. |
| `color_chest` | Int | `0xFFAA6600` | — | "Chest Color (ARGB)" | Wireframe color for Chest and Trapped Chest. Stored as ARGB integer. Default: orange-brown. |
| `color_barrel` | Int | `0xFF886644` | — | "Barrel Color (ARGB)" | Wireframe color for Barrel. Default: muted brown. |
| `color_shulker` | Int | `0xFFCC55FF` | — | "Shulker Box Color (ARGB)" | Wireframe color for all Shulker Box variants. Default: purple. |
| `line_width` | Float | `2.0f` | `0.5–5.0` | "Line Width" | Width of the wireframe lines. |

**Implementation note on color settings:** IntSetting stores the ARGB value as a raw integer. The carousel UI does not yet support a color picker, so values are changed via `.set chest_highlight.color_chest <argb-int>` in chat. Consider documenting this in the in-game description.

## Implementation

### Event Hooks

- `onEnable()` — Leert den Container-Cache.
- `onDisable()` — Leert den Container-Cache.
- `onClientTick(MinecraftClient client)` — Scannt Blocks in Radius (gecacht, alle `scan_interval` Ticks). Aktualisiert `containerCache`.
- `onRenderWorld(WorldRenderContext ctx)` — Rendert Wireframe-Box um jeden gecachten Container.
- `onJoinWorld(ClientWorld world)` — Leert den Container-Cache.
- `onLeaveWorld()` — Leert den Container-Cache.

### Required Mixins

Kein Mixin erforderlich.

### Data Structure

```java
enum ContainerType { CHEST, BARREL, SHULKER }

record ContainerEntry(BlockPos pos, ContainerType type) {}
List<ContainerEntry> containerCache = new ArrayList<>();
```

### Core Algorithm

```
// onClientTick:
if (tickCounter++ % scan_interval.get() != 0) return
world  = client.world
player = client.player
if world == null || player == null → return

containerCache.clear()
center = player.getBlockPos()
int r  = radius.get()

for dx in -r..r:
  for dy in -r..r:
    for dz in -r..r:
      pos   = center.add(dx, dy, dz)
      state = world.getBlockState(pos)
      block = state.getBlock()
      
      ContainerType type = null
      if block instanceof ChestBlock || block instanceof TrappedChestBlock:
        type = ContainerType.CHEST
      else if block instanceof BarrelBlock:
        type = ContainerType.BARREL
      else if block instanceof ShulkerBoxBlock:
        type = ContainerType.SHULKER
      
      if type != null:
        containerCache.add(new ContainerEntry(pos, type))

// onRenderWorld:
if containerCache.isEmpty() → return
MatrixStack matrices      = ctx.matrixStack()
Camera camera             = ctx.camera()
Vec3d  camPos             = camera.getPos()

// RenderLayer: LINES für Wireframe
// Für X-ray (keine Tiefenprüfung): eigenen RenderLayer mit depthTest=false verwenden
// Fabric/Minecraft bietet keinen eingebauten "no-depth" RenderLayer für drawBox.
// Lösung: GL-State manuell setzen VOR dem Draw-Call:
//   RenderSystem.disableDepthTest() wenn xray=true
//   RenderSystem.enableDepthTest()  danach wieder

VertexConsumer lineBuffer = ctx.consumers().getBuffer(RenderLayer.LINES)

if xray.get():
  RenderSystem.disableDepthTest()

for each entry in containerCache:
  int color = switch(entry.type):
    CHEST   → color_chest.get()
    BARREL  → color_barrel.get()
    SHULKER → color_shulker.get()
  
  float r = ((color >> 16) & 0xFF) / 255.0f
  float g = ((color >>  8) & 0xFF) / 255.0f
  float b = ( color        & 0xFF) / 255.0f
  float a = ((color >> 24) & 0xFF) / 255.0f
  
  // Kamera-relative Koordinaten (WorldRenderer.drawBox erwartet relative Koordinaten)
  double relX = entry.pos.getX() - camPos.x
  double relY = entry.pos.getY() - camPos.y
  double relZ = entry.pos.getZ() - camPos.z
  
  // Box leicht vergrößern (inflate um 0.01) damit sie nicht mit Block-Flächen flimmert
  WorldRenderer.drawBox(matrices, lineBuffer,
      relX - 0.01, relY - 0.01, relZ - 0.01,
      relX + 1.01, relY + 1.01, relZ + 1.01,
      r, g, b, a)

if xray.get():
  RenderSystem.enableDepthTest()
```

**Double-Chest Handling:** Ein Double-Chest besteht aus zwei Blöcken (`ChestType.LEFT` / `ChestType.RIGHT`). Beide Hälften werden einzeln gescannt und bekommen je eine eigene Box. Das ist ausreichend; alternativ kann man mit `ChestBlock.getBlockEntitySource()` die Doppelkisten erkennen und nur eine gemeinsame große Box zeichnen — dieser Mehraufwand ist optional.

### Performance Notes

- **Block-Iteration Kosten:** Bei Radius 32 ist das 65×65×65 = 274.625 Blöcke. Das ist sehr teuer wenn synchron im Tick durchgeführt. **Empfehlung:** Bei großen Radien den Scan auf mehrere Ticks verteilen (Chunked Scan) oder den Standard-Radius auf 16 reduzieren und Radius 32 nur für Fortgeschrittene empfehlen. Alternativ: nur Chunks scannen die der Client geladen hat (`world.getChunk(pos)` gibt null zurück für nicht geladene Chunks → überspringen).
- **Geladene Chunks prüfen:** `world.isChunkLoaded(cx, cz)` (wobei cx/cz Chunk-Koordinaten = blockPos >> 4) vor dem Zugriff auf `getBlockState()` aufrufen, um NPE und unnötige Chunk-Loads zu vermeiden.
- **Cache-Größe:** Im schlimmsten Fall (Radius 32, dichter Keller mit Kisten) können hunderte Einträge gecacht werden. Rendering-seitig ist das gut vertretbar (drawBox ist günstig). Ein Cap bei z.B. 500 Einträgen verhindert Pathologien.
- **Scan-Interval:** Default 20 Ticks (1 s) ist für diese Operation angemessen. Kisten verschwinden und erscheinen selten innerhalb von Sekunden.

### Edge Cases

- **Nicht geladene Chunks:** `world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)` prüfen; nicht geladene Chunks überspringen.
- **Welt null:** Alle Hooks prüfen `world == null` und returnen früh.
- **Shulker Box Farbe:** `ShulkerBoxBlock` hat 17 Varianten (16 Farben + ungefärbt). Alle teilen `ContainerType.SHULKER` und `color_shulker`. Falls pro-Farbe-Tinting gewünscht ist, kann der Shulker-Block via `ShulkerBoxBlock.getColor()` nach DyeColor befragt werden — das ist eine optionale Verbesserung.
- **Ender Chest:** Bewusst ausgelassen — Ender Chests sind persönlich und nicht "Loot". Kann als optionales BooleanSetting `show_ender_chest` nachträglich hinzugefügt werden.
- **Barrel-Rotation:** Barrels sind 1×1×1 Blöcke, die Box passt unabhängig von Rotation.
- **RenderSystem-Zustand:** `disableDepthTest()` muss zuverlässig mit `enableDepthTest()` paired werden — auch bei Exceptions. Try-finally verwenden.

## Translation Keys

```json
"noctra.chest_highlight.name": "Chest Highlight",
"noctra.chest_highlight.description": "Outlines nearby chests, barrels, and shulker boxes.",
"noctra.chest_highlight.radius": "Search Radius",
"noctra.chest_highlight.scan_interval": "Scan Interval",
"noctra.chest_highlight.xray": "Show Through Walls",
"noctra.chest_highlight.color_chest": "Chest Color",
"noctra.chest_highlight.color_barrel": "Barrel Color",
"noctra.chest_highlight.color_shulker": "Shulker Box Color",
"noctra.chest_highlight.line_width": "Line Width"
```

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/chest_highlight.png`  
**Size:** 32×32 PNG  
**Suggestion:** Ein vereinfachtes Kisten-Icon (Trapez-Silhouette mit Schloss-Streifen) umgeben von einem leuchtenden farbigen Rahmen/Glow — die Outline-Box symbolisiert das Highlight-Konzept. Farbe des Rahmens in Orange (Kisten-Farbe).
