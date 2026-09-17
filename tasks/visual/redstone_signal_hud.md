# Redstone Signal Strength HUD

**ID:** `redstone_signal_hud`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/redstone_signal_hud/RedstoneSignalHudModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.redstone_signal_hud`

## Description

Zeigt die Redstone-Signalstärke (0–15) des Blocks an, auf den der Spieler gerade zielt. Funktioniert für alle Blöcke die Redstone-Power produzieren oder durchleiten: Hebel, Knöpfe, Redstone-Fackel, Komparator, Repeater, Redstone-Staub, Truhen (Komparator-Ausgang), u.a. Kompakter Ersatz für den F3-Screen beim Bau komplexer Redstone-Schaltungen.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `only_when_targeting` | Boolean | `true` | — | "Only When Targeting" | Nur anzeigen wenn Redstone-Block angevisiert |
| `show_bar` | Boolean | `true` | — | "Show Bar" | Signalstärke-Balken (0–15) anzeigen |
| `show_block_name` | Boolean | `true` | — | "Show Block Name" | Name des anvisierten Blocks anzeigen |
| `show_all_sides` | Boolean | `false` | — | "Show All Sides" | Signalstärke aller 6 Seiten anzeigen |
| `color_zero` | Color | `0xFF666666` | ARGB Hex | "Color Zero" | Farbe bei Signal 0 (kein Signal) |
| `color_low` | Color | `0xFFFF5555` | ARGB Hex | "Color Low (1-7)" | Farbe bei niedriger Signalstärke |
| `color_high` | Color | `0xFFFF2222` | ARGB Hex | "Color High (8-15)" | Farbe bei hoher Signalstärke |
| `always_show_value` | Boolean | `false` | — | "Always Show" | Auch anzeigen wenn Signal 0 (kein Signal) |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Anvisierten Block ermitteln und Signal auslesen.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
// Redstone-Relevante Block-Typen erkennen:
// Nicht alle Blöcke haben sinnvolle Signalstärken.
// Redstone-relevante Blöcke: Block implementiert AbstractRedstoneGateBlock,
// oder ist RedstoneWireBlock, RedstoneTorchBlock, LeverBlock etc.
// Einfachste Methode: world.getEmittedRedstonePower() aufrufen und
// alle Blöcke mit power > 0 oder explizit Redstone-Blöcke anzeigen.

static final Set<Block> REDSTONE_BLOCKS = Set.of(
    Blocks.REDSTONE_WIRE, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH,
    Blocks.REPEATER, Blocks.COMPARATOR, Blocks.LEVER, Blocks.STONE_BUTTON,
    Blocks.OAK_BUTTON, /* alle Button-Varianten... */
    Blocks.DAYLIGHT_DETECTOR, Blocks.OBSERVER, Blocks.TARGET,
    Blocks.REDSTONE_BLOCK, Blocks.SCULK_SENSOR, Blocks.CALIBRATED_SCULK_SENSOR,
    Blocks.TRAPPED_CHEST
)

onRenderHud(ctx, tickDelta):
    if (mc.player == null || mc.world == null) return
    
    // Anvisierten Block ermitteln
    HitResult hitResult = mc.crosshairTarget
    if (!(hitResult instanceof BlockHitResult blockHit)) return
    
    BlockPos pos       = blockHit.getBlockPos()
    BlockState state   = mc.world.getBlockState(pos)
    Block block        = state.getBlock()
    
    // Ist es ein Redstone-relevanter Block?
    boolean isRedstone = REDSTONE_BLOCKS.contains(block)
                      || block instanceof AbstractRedstoneGateBlock
    
    if (onlyWhenTargeting.get() && !isRedstone) return
    
    // Signalstärke abrufen
    // getEmittedRedstonePower gibt die abgegebene Stärke zurück
    int power = mc.world.getEmittedRedstonePower(pos, blockHit.getSide())
    // Zusätzlich: getReceivedRedstonePower für eingehende Signalstärke
    int receivedPower = mc.world.getReceivedRedstonePower(pos)
    
    // Wenn Signal = 0 und always_show_value = false und onlyWhenTargeting = true → return
    if (!alwaysShowValue.get() && power == 0 && !isRedstone) return
    
    // Farbe wählen
    int color
    if (power == 0)       color = colorZero.get()
    else if (power <= 7)  color = colorLow.get()
    else                  color = colorHigh.get()
    
    // Block-Name
    String blockName = block.getName().getString()
    
    // Rendering
    int lineY = y + 4
    if (showBlockName.get()):
        drawText(ctx, blockName, x + 4, lineY, 0xFFAAAAAA);  lineY += 10
    
    drawText(ctx, "Signal: " + power + "/15", x + 4, lineY, color);  lineY += 10
    
    if (showBar.get()):
        int barX = x + 4; int barY = lineY; int barW = getDefaultWidth() - 8; int barH = 4
        int fill = (int) (barW * (power / 15.0f))
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF333333)
        if (fill > 0) ctx.fill(barX, barY, barX + fill, barY + barH, color)
        lineY += 8
    
    // Alle Seiten (optional, nur für Fortgeschrittene)
    if (showAllSides.get()):
        Direction[] dirs = Direction.values()
        for (Direction dir : dirs):
            int sidePower = mc.world.getEmittedRedstonePower(pos, dir)
            if (sidePower > 0):
                drawText(ctx, dir.getName() + ": " + sidePower, x + 4, lineY, color)
                lineY += 9
```

**Hinweis zu `getEmittedRedstonePower`:** Gibt die Signalstärke zurück die der Block nach außen abgibt. Für Komparatoren und Repeater ist das der Output-Wert. Für Redstone-Staub ist es der Staub-Level. Dies ist die praxisnächste Methode — kein komplexes Redstone-System-Verständnis nötig.

### Edge Cases

- `mc.crosshairTarget` kann null sein (sehr seltener Edge-Case nach Reconnect) → null-Guard.
- Nicht-Block-HitResult (Entität angevisiert): `instanceof BlockHitResult` Guard.
- Block außerhalb Render-Distanz: `getBlockState()` kann eine leere Chunk-Section zurückgeben → `state.isAir()` Guard.
- Button-Varianten: Alle Button-Subklassen von `AbstractButtonBlock` müssen einzeln oder via instanceof-Check erfasst werden.
- `REDSTONE_BLOCKS` Set ist static-final — kein Allokierungs-Overhead per Frame.

## Translation Keys

```json
"noctra.redstone_signal_hud.name": "Redstone Signal",
"noctra.redstone_signal_hud.description": "Shows the redstone signal strength of the targeted block.",
"noctra.redstone_signal_hud.bg_color": "Background Color",
"noctra.redstone_signal_hud.border_color": "Border Color",
"noctra.redstone_signal_hud.text_color": "Text Color",
"noctra.redstone_signal_hud.text_shadow": "Text Shadow",
"noctra.redstone_signal_hud.text_scale": "Text Scale",
"noctra.redstone_signal_hud.show_background": "Show Background",
"noctra.redstone_signal_hud.only_when_targeting": "Only When Targeting",
"noctra.redstone_signal_hud.show_bar": "Show Bar",
"noctra.redstone_signal_hud.show_block_name": "Show Block Name",
"noctra.redstone_signal_hud.show_all_sides": "Show All Sides",
"noctra.redstone_signal_hud.color_zero": "Color Zero",
"noctra.redstone_signal_hud.color_low": "Color Low (1-7)",
"noctra.redstone_signal_hud.color_high": "Color High (8-15)",
"noctra.redstone_signal_hud.always_show_value": "Always Show"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/redstone_signal_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Roter Redstone-Staub-Pixel mit einer Zahl "15" in Orange daneben. Klassisch Redstone-rot.
