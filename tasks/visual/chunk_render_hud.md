# Chunk Render HUD (Performance Stats)

**ID:** `chunk_render_hud`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/chunk_render_hud/ChunkRenderHudModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.chunk_render_hud`

## Description

Zeigt Performance-relevante Render-Statistiken auf dem HUD an: gerenderte Chunks, geladene Chunks, Entityanzahl im Sichtfeld, Block-Entitäten und optional Partikel-Anzahl. Entspricht einem kompakten, immer sichtbaren Auszug aus dem F3-Debug-Screen — ohne diesen öffnen zu müssen. Ideal für Performance-Analyse beim Erkunden oder beim Bauen aufwändiger Builds.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `show_chunks` | Boolean | `true` | — | "Show Chunks" | Gerenderte / geladene Chunks anzeigen |
| `show_entities` | Boolean | `true` | — | "Show Entities" | Anzahl sichtbarer Entitäten anzeigen |
| `show_block_entities` | Boolean | `true` | — | "Show Block Entities" | Anzahl sichtbarer Block-Entitäten anzeigen |
| `show_particles` | Boolean | `false` | — | "Show Particles" | Partikelanzahl anzeigen |
| `show_pending_chunks` | Boolean | `false` | — | "Show Pending Chunks" | Chunks in der Render-Queue anzeigen |
| `compact_mode` | Boolean | `false` | — | "Compact Mode" | Alle Werte in einer Zeile |
| `color_warn_entities` | Int | `200` | `50–2000` | "Entity Warn Count" | Ab dieser Entitätszahl Warnfarbe |
| `color_entity_warn` | Color | `0xFFFFFF55` | ARGB Hex | "Entity Warn Color" | Warnfarbe für hohe Entitätszahl |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Statistiken aus WorldRenderer auslesen und rendern.

### Required Mixins

Kein Mixin erforderlich — alle Daten sind über `MinecraftClient` zugänglich.

### Core Algorithm

```
// Zugriff auf WorldRenderer-Statistiken in 1.21.x:
// mc.worldRenderer enthält die relevanten Zähler als interne Felder.
// Diese sind teilweise package-private oder über @Accessor Mixins zugänglich.
// Alternativ: DebugHud-String parsen (robust aber unelegant).
// Empfohlener Ansatz: Accessor Mixin für WorldRenderer.

// Benötigte Accessor Mixin Felder aus WorldRenderer:
//   int renderedChunks    — sichtbare, gerenderte Chunk-Sections
//   int loadedChunks      — alle geladenen Chunk-Sections im Speicher
//   int regularEntityCount — Entitäten im Sichtfeld
//   int blockEntityCount   — Block-Entitäten im Sichtfeld
// In 1.21.x heißen die internen Felder möglicherweise anders.
// Alternativ: mc.worldRenderer.getDebugString() parsen.

onRenderHud(ctx, tickDelta):
    if (mc.player == null || mc.world == null) return

    // Chunks
    String chunksLine = null
    if (showChunks.get()):
        // Aus DebugHud-String extrahieren (fallback-sicher):
        String debug = mc.worldRenderer.getDebugString()  // z.B. "C[256] s[0] , D: 12, pC: 0"
        // Parsen: Zahlen aus String extrahieren
        // Oder via Accessor:
        int rendered = getRenderedChunks()   // via Accessor Mixin
        int loaded   = mc.world.getChunkManager().getLoadedChunkCount()
        chunksLine   = "Chunks: " + rendered + " / " + loaded

    // Entities
    String entitiesLine = null
    if (showEntities.get()):
        int entCount = getRenderedEntityCount()  // via Accessor Mixin
        int entColor = entCount >= colorWarnEntities.get() ? colorEntityWarn.get() : textColor.get()
        entitiesLine = "Entities: " + entCount

    // Block Entities
    String beLine = null
    if (showBlockEntities.get()):
        int beCount  = getRenderedBlockEntityCount()  // via Accessor Mixin
        beLine       = "BE: " + beCount

    // Rendering
    if (compactMode.get()):
        // Alle Werte in einer Zeile: "C: 256/1024  E: 47  BE: 12"
        StringBuilder sb = new StringBuilder()
        if (chunksLine != null)  sb.append("C: ").append(rendered).append("/").append(loaded).append("  ")
        if (entitiesLine != null) sb.append("E: ").append(entCount).append("  ")
        if (beLine != null)       sb.append("BE: ").append(beCount)
        // Rendern...
    else:
        // Jede Zeile einzeln
        int lineY = y + 4
        for jede aktivierte Zeile:
            drawText(ctx, line, x + 4, lineY, color)
            lineY += 10
```

### Accessor Mixin für WorldRenderer

Da die internen Felder von `WorldRenderer` package-private sind, wird ein Accessor-Mixin benötigt:

```java
// mixin/WorldRendererAccessor.java
@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor("regularEntityCount")
    int getRegularEntityCount();
    
    @Accessor("blockEntityCount")  
    int getBlockEntityCount();
}
```

**Registrierung in noctra.mixins.json** unter `"client"`.

**Fallback-Strategie ohne Mixin:** `mc.worldRenderer.getDebugString()` gibt einen String wie `"C[256] s[0] , D: 12, pC: 0"` zurück. Daraus lassen sich Chunk-Zahlen via Regex extrahieren. Für Entities: `mc.world.getEntities()` zählen (weniger präzise als "rendered"-Count).

### Edge Cases

- `mc.worldRenderer == null`: Guard-Clause.
- Accessor-Felder umbenennen sich zwischen MC-Versionen: Yarn-Mappings für 1.21.11 prüfen.
- `getDebugString()` Format kann sich ändern: Als Fallback nur Gesamtzahl aus `mc.world` nutzen.
- Kompakt-Modus: Text kann breiter als HUD-Widget sein → `getDefaultWidth()` dynamisch an Inhalt anpassen.

## Translation Keys

```json
"noctra.chunk_render_hud.name": "Chunk Render Stats",
"noctra.chunk_render_hud.description": "Shows chunk and entity render stats on the HUD.",
"noctra.chunk_render_hud.bg_color": "Background Color",
"noctra.chunk_render_hud.border_color": "Border Color",
"noctra.chunk_render_hud.text_color": "Text Color",
"noctra.chunk_render_hud.text_shadow": "Text Shadow",
"noctra.chunk_render_hud.text_scale": "Text Scale",
"noctra.chunk_render_hud.show_background": "Show Background",
"noctra.chunk_render_hud.show_chunks": "Show Chunks",
"noctra.chunk_render_hud.show_entities": "Show Entities",
"noctra.chunk_render_hud.show_block_entities": "Show Block Entities",
"noctra.chunk_render_hud.show_particles": "Show Particles",
"noctra.chunk_render_hud.show_pending_chunks": "Show Pending Chunks",
"noctra.chunk_render_hud.compact_mode": "Compact Mode",
"noctra.chunk_render_hud.color_warn_entities": "Entity Warn Count",
"noctra.chunk_render_hud.color_entity_warn": "Entity Warn Color"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/chunk_render_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Isometrische Chunk-Würfel mit einem kleinen Balkendiagramm-Symbol. Technisch/analytisch wirkende Pixel-Art.
