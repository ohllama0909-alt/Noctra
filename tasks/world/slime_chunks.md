# Slime Chunks

**ID:** `slime_chunks`  
**Category:** WORLD  
**Status:** [x] DONE  
**Class:** `modules/impl/slime_chunks/SlimeChunksModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.slime_chunks`

## System Notes (Updated)

- Module Ordner: `modules/impl/slime_chunks/SlimeChunksModule.java`
- Package: `de.snenjih.noctra.modules.impl.slime_chunks`
- Implementiert HudElement: Nein (Welt-Rendering via `onRenderWorld`)
- Verwendet `WorldRenderContext` aus `net.fabricmc.fabric.api.client.rendering.v1.world`

## Description

Visualisiert Slime-Chunks im konfigurierbaren Radius um den Spieler. Slime-Chunks sind feste, seed-abhängige Chunks in denen Slimes auch unterhalb Y=40 spawnen können. Die Anzeige zeichnet farbige Boxen über Chunks (grün = Slime-Chunk, optional auch nicht-Slime-Chunks in Rot). Nur im Overworld verfügbar. Auf Multiplayer-Servern kann der Seed manuell über einen Chat-Befehl eingegeben werden.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `radius` | Int | `4` | `1–8` | "Chunk Radius" | Anzahl der Chunks um den Spieler die angezeigt werden |
| `show_non_slime` | Boolean | `false` | — | "Show Non-Slime" | Nicht-Slime-Chunks ebenfalls (in Rot) anzeigen |
| `opacity` | Int | `80` | `10–255` | "Overlay Opacity" | Transparenz der Chunk-Boxen (Alpha-Wert) |
| `height_offset` | Int | `0` | `-64–320` | "Y Offset" | Y-Position der Overlay-Boxen relativ zum Spieler |

## Implementation

### Event Hooks

- `onRenderWorld(WorldRenderContext ctx)` — Zeichnet farbige Chunk-Boxen für alle Chunks im Radius.
- `onJoinWorld(ClientWorld world)` — Versucht Seed automatisch zu laden (Singleplayer). Seed-Cache leeren.

### Required Mixins

Kein Mixin erforderlich. Der Seed wird via `ClientWorld` gelesen (nur Singleplayer) oder ist manuell konfiguriert.

### Core Algorithm

```
Felder in der Klasse:
    private long worldSeed = 0L;
    private boolean hasSeed = false;

onJoinWorld(ClientWorld world):
    hasSeed = false
    worldSeed = 0L
    // Singleplayer: Seed direkt lesbar
    MinecraftClient mc = MinecraftClient.getInstance()
    if (mc.isInSingleplayer()):
        // In 1.21.11: world.getSeed() — aber ClientWorld hat keinen getSeed().
        // Stattdessen: MinecraftClient.getServer().getOverworld().getSeed()
        IntegratedServer server = mc.getServer()
        if (server != null):
            worldSeed = server.getOverworld().getSeed()
            hasSeed = true

// Manueller Seed-Befehl (via ChatCommandDispatcher):
// /mandatory slimeseed <seed>  → worldSeed = Long.parseLong(arg), hasSeed = true

public static boolean isSlimeChunk(long seed, int chunkX, int chunkZ):
    Random rng = new Random(
        seed
        + (long)(chunkX * chunkX * 0x4C1906)
        + (long)(chunkX * 0x5AC0DB)
        + (long)(chunkZ * chunkZ) * 0x4307A7L
        + (long)(chunkZ * 0x5F24F)
        ^ 0x3AD8025F
    )
    return rng.nextInt(10) == 0

onRenderWorld(WorldRenderContext ctx):
    MinecraftClient mc = MinecraftClient.getInstance()
    if (mc.player == null || mc.world == null) return
    if (!hasSeed):
        // Warnung auf HUD anzeigen statt Boxen
        return

    // Nur im Overworld anzeigen
    if (!mc.world.getRegistryKey().equals(World.OVERWORLD)) return

    int playerChunkX = mc.player.getBlockPos().getX() >> 4
    int playerChunkZ = mc.player.getBlockPos().getZ() >> 4
    int r = radius.get()

    Camera camera = ctx.camera()
    Vec3d camPos = camera.getPos()
    MatrixStack matrices = ctx.matrixStack()

    // Y-Bereich der Box: Spieler-Y ± 1 (nur dünne Schicht als Overlay)
    int playerY = mc.player.getBlockPos().getY() + heightOffset.get()
    double boxY1 = playerY - 0.05
    double boxY2 = playerY + 0.05

    VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers()
    VertexConsumer lines = immediate.getBuffer(RenderLayer.LINES)

    for (int cx = playerChunkX - r; cx <= playerChunkX + r; cx++):
        for (int cz = playerChunkZ - r; cz <= playerChunkZ + r; cz++):
            boolean isSlime = isSlimeChunk(worldSeed, cx, cz)

            if (!isSlime && !showNonSlime.get()) continue

            // Chunk-Block-Koordinaten
            double x1 = (cx * 16.0) - camPos.x
            double z1 = (cz * 16.0) - camPos.z
            double x2 = x1 + 16.0
            double z2 = z1 + 16.0
            double y1 = boxY1 - camPos.y
            double y2 = boxY2 - camPos.y

            int alpha = opacity.get()
            int fillColor
            int lineColor
            if (isSlime):
                fillColor = (alpha << 24) | 0x00FF00   // Grün
                lineColor = 0xFF00FF00
            else:
                fillColor = (alpha << 24) | 0xFF0000   // Rot
                lineColor = 0xFFFF0000

            // Gefülltes Rechteck (Boden)
            // In 1.21.11 WorldRender: WorldRenderContext bietet keinen direkten "filled box" helper
            // → Nutze RenderSystem + Tessellator für Quads, oder nutze DebugRenderer-Style
            drawFilledBox(matrices, immediate, x1, y1, z1, x2, y2, z2, fillColor)
            drawOutlineBox(matrices, lines, x1, y1, z1, x2, y2, z2, lineColor)

    immediate.draw()

// Helper-Methoden für Box-Rendering:
// drawFilledBox: Tessellator mit QUADS (6 Seiten), ARGB-Farbe
// drawOutlineBox: Tessellator mit LINES (12 Kanten), RGB-Farbe
// Analog zu vanilla DebugRenderer.drawBox() als Referenz
```

### Seed-Kommando-Integration

```
// In ChatCommandDispatcher.java registrieren:
// Befehl: /mandatory slimeseed <long>
// Falls Seed ungültig (kein Long): Chat-Fehlermeldung
// Erfolgreich: module.setSeed(seed), hasSeed = true
// Chat-Bestätigung: "[NoctraMod] Slime chunk seed set to: <seed>"
```

### Edge Cases

- `mc.getServer()` ist null auf Multiplayer: `hasSeed = false`. HUD zeigt Warnung "Slime Chunks: No seed! Use /mandatory slimeseed <seed>".
- Seed auf Multiplayer manuell eingeben: Seed muss vom Spieler oder Server-Admin erfragt werden. Keine automatische Detection möglich.
- Nicht im Overworld: Slimes spawnen zwar in Sümpfen auf jeder Y-Höhe, aber die Chunk-basierte Spawn-Logik (unter Y=40) gilt nur im Overworld. Anzeige deaktiviert in Nether/End.
- Sumpf-Biom: In Sumpf-Chunks spawnen Slimes auch ohne Slime-Chunk-Status (Vanilla-Mechanik). Diese Spec deckt nur Slime-Chunks ab, nicht Sumpf-Spawning.
- `java.util.Random` vs. `net.minecraft.util.math.random.Random`: Die Slime-Chunk-Berechnung nutzt `java.util.Random` mit dem spezifischen Seed-Algorithmus. NICHT die Minecraft-eigene Random-Klasse verwenden.
- Falsche Seed-Eingabe: Seed 0 ist valide (wird als gesetzt behandelt wenn manuell eingegeben). `hasSeed`-Flag trennt "gesetzt" von "nicht gesetzt".
- Chunk-Grenzberechnung: `chunkX * 16` bis `chunkX * 16 + 15` ist der korrekte Block-Bereich. Im Code oben: `x1 = cx * 16`, `x2 = x1 + 16` (exklusive Grenze, aber für Rendering korrekt).
- Performance: `radius = 8` → (17×17) = 289 Chunks pro Frame. `isSlimeChunk()` ist O(1), Rendering der Boxen ist der Flaschenhals. Bei vielen Chunks kann FPS sinken. Max-Radius auf 8 begrenzen.
- Y-Offset außerhalb der Welt-Grenzen: `playerY + heightOffset` kann < -64 oder > 320 sein. Kein Crash, nur unsichtbare Box (unterhalb Bedrock oder oberhalb Build-Limit).
- Negative Chunk-Koordinaten: Die Slime-Algorithmus-Berechnung funktioniert korrekt für negative `chunkX`/`chunkZ` (Java `long`-Arithmetik).
- `ctx.matrixStack()` in `WorldRenderContext`: In 1.21.11 verfügbar über `WorldRenderContext.matrixStack()`. Camera-Position via `ctx.camera().getPos()`.

## Translation Keys

```json
"noctra.slime_chunks.name": "Slime Chunks",
"noctra.slime_chunks.description": "Highlights slime chunks in the world based on the world seed.",
"noctra.slime_chunks.radius": "Chunk Radius",
"noctra.slime_chunks.show_non_slime": "Show Non-Slime",
"noctra.slime_chunks.opacity": "Overlay Opacity",
"noctra.slime_chunks.height_offset": "Y Offset"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/slime_chunks.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Stilisierter grüner Slime-Würfel auf einem Schachbrett-Gitter (Chunk-Grid). Farbe: Leuchtendes Grün auf dunklem Hintergrund.
