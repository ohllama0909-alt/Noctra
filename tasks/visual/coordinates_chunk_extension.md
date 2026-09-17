# Coordinates HUD — Chunk Extension

**ID:** `coordinates_hud` (kein neues Modul — Erweiterung des bestehenden)  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/coordinates_hud/CoordinatesHudModule.java` (bestehend erweitern)  
**Package:** `de.snenjih.noctra.modules.impl.coordinates_hud`

## Description

Erweitert das bestehende Coordinates HUD um detaillierte Chunk-Informationen. Das aktuelle `show_chunk`-Setting zeigt nur Chunk X/Y/Z — diese Extension fügt die **Position innerhalb des Chunks** (0–15 je Achse) sowie optional die **äquivalenten Koordinaten in der anderen Dimension** (Overworld ↔ Nether, ÷8 bzw. ×8) hinzu. Kein neues Modul — alle Settings werden im bestehenden CoordinatesHudModule ergänzt.

**Achtung:** Das bestehende `show_chunk` Boolean bleibt erhalten. Die neuen Settings sind eigenständig schaltbar.

## Neue Settings (werden zu CoordinatesHudModule hinzugefügt)

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `show_chunk_pos` | Boolean | `false` | — | "Show Chunk Position" | Chunk X/Z und Within-Chunk-Offset (0–15) anzeigen |
| `show_within_chunk` | Boolean | `true` | — | "Show Within-Chunk Offset" | Offset innerhalb des Chunks (nur wenn show_chunk_pos aktiv) |
| `show_nether_coords` | Boolean | `false` | — | "Show Nether Coords" | Äquivalente Koordinaten in anderer Dimension anzeigen |
| `show_region_file` | Boolean | `false` | — | "Show Region File" | Region-Dateiname anzeigen (r.X.Z.mca) |

## Implementation

### Zu ergänzende Logik in CoordinatesHudModule.renderHud()

```
// Neue Berechnungen (nach bestehenden X/Y/Z-Berechnungen):

// Chunk-Position (Chunk-Koordinaten = Block >> 4)
int chunkX = bx >> 4   // oder (int) Math.floor(player.getX()) >> 4
int chunkZ = bz >> 4

// Offset innerhalb des Chunks (0–15)
int offsetX = ((bx % 16) + 16) % 16   // Modulo mit Handling negativer Zahlen
int offsetZ = ((bz % 16) + 16) % 16

// Nether-Konvertierung
double netherX, netherZ, overworldX, overworldZ
boolean inNether = mc.world.getDimensionKey() == DimensionTypes.THE_NETHER
                   // oder world.getRegistryKey() == World.NETHER
if (inNether):
    overworldX = player.getX() * 8.0
    overworldZ = player.getZ() * 8.0
    netherX    = player.getX()
    netherZ    = player.getZ()
else:
    netherX    = player.getX() / 8.0
    netherZ    = player.getZ() / 8.0
    overworldX = player.getX()
    overworldZ = player.getZ()

// Region-Datei: r.(chunkX >> 5).(chunkZ >> 5).mca
int regionX = chunkX >> 5
int regionZ = chunkZ >> 5
String regionFile = "r." + regionX + "." + regionZ + ".mca"

// --- Rendering ---

// show_chunk_pos: "Chunk: -3 / 7" + optional "[5, 12]"
if (showChunkPos.get()):
    String chunkLine = "Chunk: " + chunkX + " / " + chunkZ
    if (showWithinChunk.get()):
        chunkLine += "  [" + offsetX + ", " + offsetZ + "]"
    ctx.drawTextWithShadow(tr, chunkLine, x + 4, currentY, 0xFF8899AA)
    currentY += 10

// show_nether_coords: "Nether: -12 / 7" oder "Overworld: 97 / 56"
if (showNetherCoords.get()):
    String label  = inNether ? "Overworld:" : "Nether:"
    double eqX    = inNether ? overworldX   : netherX
    double eqZ    = inNether ? overworldZ   : netherZ
    String coordLine = label + " " + (int)eqX + " / " + (int)eqZ
    ctx.drawTextWithShadow(tr, coordLine, x + 4, currentY, 0xFF88BBFF)
    currentY += 10

// show_region_file: "Region: r.-1.0.mca"
if (showRegionFile.get()):
    ctx.drawTextWithShadow(tr, "Region: " + regionFile, x + 4, currentY, 0xFF667788)
    currentY += 10
```

### Dimensionscheck

```java
// Empfohlene Methode für Dimensionscheck in 1.21.x:
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

boolean inNether = mc.world.getRegistryKey().equals(World.NETHER);
boolean inOverworld = mc.world.getRegistryKey().equals(World.OVERWORLD);
// Wenn weder Nether noch Overworld (End etc.): Nether-Coords ausblenden
if (!inNether && !inOverworld && showNetherCoords.get()) → nicht rendern
```

### Edge Cases

- Negative Block-Koordinaten beim Modulo-Offset: `((bx % 16) + 16) % 16` gibt immer 0–15.
- End-Dimension: Nether-Koordinaten sinnlos → `showNetherCoords` nur rendern wenn Overworld oder Nether.
- `show_chunk` (altes Setting) und `show_chunk_pos` (neues) überlappen sich thematisch — das alte `show_chunk` zeigt nur Chunk X/Y/Z ohne Offset, das neue zeigt X/Z mit Offset. Beide können gleichzeitig aktiv sein.
- Region-Koordinaten für sehr weit entfernte Positionen (>32 Mio. Blöcke): Kein Problem, int-Arithmetik reicht.

## Translation Keys (neue Keys)

```json
"noctra.coordinates_hud.show_chunk_pos": "Show Chunk Position",
"noctra.coordinates_hud.show_within_chunk": "Show Within-Chunk Offset",
"noctra.coordinates_hud.show_nether_coords": "Show Nether Coords",
"noctra.coordinates_hud.show_region_file": "Show Region File"
```

## Kein neues Icon erforderlich

Das bestehende Coordinates-HUD-Icon wird verwendet.
