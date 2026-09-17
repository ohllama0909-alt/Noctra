# Biome Display

**ID:** `biome_display`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/biome_display/BiomeDisplayModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.biome_display`

## System Notes (Updated)

- Module Ordner: `modules/impl/biome_display/BiomeDisplayModule.java`
- Package: `de.snenjih.noctra.modules.impl.biome_display`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Zeigt den Namen des aktuellen Bioms als HUD-Overlay an. Hilfreich beim Erkunden, bei der Suche nach bestimmten Biomen und beim Sammeln biomspezifischer Ressourcen. Der Biom-Name wird automatisch lokalisiert und entspricht dem in-game F3-Anzeige-Wert.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `2` | `0–1920` | "X Position" | Horizontale Position |
| `y_pos` | Int | `16` | `0–1080` | "Y Position" | Vertikale Position |
| `text_color` | Int | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Textfarbe |
| `show_label` | Boolean | `true` | — | "Show Label" | Präfix "Biome: " anzeigen |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund |
| `update_interval` | Int | `20` | `1–100` | "Update Interval (ticks)" | Wie oft der Biom-Wert aktualisiert wird |

## Implementation

### Event Hooks

- `onClientTick(MinecraftClient client)` — Biom periodisch (alle `updateInterval` Ticks) auslesen und cachen.
- `onRenderHud(DrawContext ctx, float tickDelta)` — Gecachten Biom-String rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
Felder:
    private String cachedBiomeName = ""
    private int tickCounter = 0

onClientTick(MinecraftClient client):
    if (client.player == null || client.world == null) return
    tickCounter++
    if (tickCounter < updateInterval.get()) return
    tickCounter = 0

    BlockPos pos = client.player.getBlockPos()
    RegistryEntry<Biome> biomeEntry = client.world.getBiome(pos)

    // Biom-Name aus Registry-Key extrahieren
    Optional<RegistryKey<Biome>> keyOpt = biomeEntry.getKey()
    if (keyOpt.isEmpty()):
        cachedBiomeName = "Unknown"
        return

    // Übersetzungsschlüssel: "biome.namespace.path"
    // z. B. "biome.minecraft.forest"
    RegistryKey<Biome> key = keyOpt.get()
    String translationKey = "biome." + key.getValue().getNamespace() + "." + key.getValue().getPath()
    cachedBiomeName = Text.translatable(translationKey).getString()

onRenderHud(DrawContext ctx, float tickDelta):
    if (cachedBiomeName.isEmpty()) return

    String display = showLabel.get() ? "Biome: " + cachedBiomeName : cachedBiomeName
    int x = xPos.get()
    int y = yPos.get()

    if (background.get()):
        int w = mc.textRenderer.getWidth(display)
        ctx.fill(x - 2, y - 2, x + w + 2, y + 10, 0x88000000)

    ctx.drawTextWithShadow(mc.textRenderer, Text.literal(display), x, y, textColor.get())
```

**Hinweis zu Biom-Übersetzung:** Die Vanilla-Sprach-Keys für Biome folgen dem Muster `biome.<namespace>.<path>`. Modded Biome (z. B. aus Biomes O' Plenty) haben eigene Namespace-Keys, die korrekt aufgelöst werden, sofern das Sprachpaket des Mods geladen ist. Fehlende Keys zeigen den rohen Key-String.

### Edge Cases

- `mc.player == null` oder `mc.world == null`: Guard in `onClientTick`; `cachedBiomeName` bleibt leer → `onRenderHud` zeigt nichts.
- Spieler wechselt Dimension: Biom wird beim nächsten Tick-Interval aktualisiert; kurze Verzögerung akzeptabel.
- `biomeEntry.getKey()` liefert `Optional.empty()`: Kann bei dynamisch generierten oder korrupten Biomen passieren → "Unknown" anzeigen.
- `updateInterval == 1`: Biom wird jeden Tick abgefragt — kostenintensiver, aber kein Problem für Client-Performance bei einem String-Lookup.
- Biom-Name nicht in aktiver Sprache übersetzt: `Text.translatable(...).getString()` gibt den rohen Key zurück — akzeptables Fallback-Verhalten.

## Translation Keys

```json
"noctra.biome_display.name": "Biome Display",
"noctra.biome_display.description": "Shows the name of your current biome on screen.",
"noctra.biome_display.x_pos": "X Position",
"noctra.biome_display.y_pos": "Y Position",
"noctra.biome_display.text_color": "Text Color",
"noctra.biome_display.show_label": "Show Label",
"noctra.biome_display.background": "Background",
"noctra.biome_display.update_interval": "Update Interval (ticks)"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/biome_display.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Miniatur-Landschaft: grüner Hügel, Baum und Sonne im Pixel-Art-Stil. Alternativ ein stilisiertes Blatt oder Grasblock.
