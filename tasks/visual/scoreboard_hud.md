# Scoreboard HUD

**ID:** `scoreboard_hud`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/scoreboard_hud/ScoreboardHudModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.scoreboard_hud`

## System Notes (Updated)

- Module Ordner: `modules/impl/scoreboard_hud/ScoreboardHudModule.java`
- Package: `de.snenjih.noctra.modules.impl.scoreboard_hud`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Ersetzt das Vanilla-Scoreboard-Sidebar-Widget durch ein eigenes, frei positionierbares HUD-Element mit konfigurierbarer Größe, Transparenz und Position. Ermöglicht, das Scoreboard kleiner, an eine beliebige Ecke zu verschieben oder komplett auszublenden, ohne den Informationsgehalt zu verlieren.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `hud_x` | Int | `2` | `0–1920` | "HUD X" | X-Position des Scoreboards (Pixel vom linken Rand) |
| `hud_y` | Int | `10` | `0–1080` | "HUD Y" | Y-Position des Scoreboards (Pixel vom oberen Rand) |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Skalierung des Scoreboard-Textes |
| `background_alpha` | Int | `128` | `0–255` | "Background Alpha" | Transparenz des Hintergrunds (0=unsichtbar, 255=opak) |
| `max_entries` | Int | `15` | `1–20` | "Max Entries" | Maximale Anzahl angezeigter Einträge |
| `hide_vanilla` | Boolean | `true` | — | "Hide Vanilla" | Vanilla-Scoreboard-Sidebar ausblenden |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, RenderTickCounter counter)` — Liest das aktive Sidebar-Objective aus dem Scoreboard, sortiert Einträge und rendert das eigene HUD.

### Required Mixins

**Mixin: Vanilla-Scoreboard ausblenden**
- **Class:** `mixin/ScoreboardHudVanillaMixin.java`
- **Target:** `net.minecraft.client.gui.hud.InGameHud`
- **Methode:** `renderScoreboard(DrawContext context, ScoreboardObjective objective)`
- **Injection:** `@Inject(at = @At("HEAD"), method = "renderScoreboard(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", cancellable = true)`
- **Zweck:** Wenn Modul aktiv und `hideVanilla` true: `ci.cancel()` → unterdrückt das Vanilla-Scoreboard-Rendering.

### Core Algorithm

```
onRenderHud(DrawContext ctx, RenderTickCounter counter):
    MinecraftClient mc = MinecraftClient.getInstance()
    if (mc.world == null || mc.player == null) return

    Scoreboard scoreboard = mc.world.getScoreboard()
    ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR)
    if (objective == null) return

    // Alle Einträge sammeln und sortieren
    Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective)
    if (entries == null || entries.isEmpty()) return

    List<ScoreboardEntry> sorted = entries.stream()
        .sorted(Comparator.comparingInt(ScoreboardEntry::value).reversed())
        .limit(maxEntries.get())
        .collect(Collectors.toList())

    TextRenderer tr = mc.textRenderer
    int lineHeight = (int)(9 * textScale.get()) + 1
    int x = hudX.get()
    int y = hudY.get()

    // Titel
    String title = objective.getDisplayName().getString()
    int titleWidth = (int)(tr.getWidth(title) * textScale.get())

    // Breite des breitesten Eintrags berechnen
    int maxWidth = titleWidth
    for (ScoreboardEntry entry : sorted):
        String line = entry.owner() + ": " + entry.value()
        maxWidth = Math.max(maxWidth, (int)(tr.getWidth(line) * textScale.get()))

    int bgAlpha = backgroundAlpha.get() << 24
    int bgColor = bgAlpha | 0x000000

    // Hintergrund
    ctx.fill(x - 2, y - 2, x + maxWidth + 4, y + (sorted.size() + 1) * lineHeight + 2, bgColor)

    // Titel rendern
    ctx.getMatrices().push()
    ctx.getMatrices().scale(textScale.get(), textScale.get(), 1f)
    ctx.drawTextWithShadow(tr, Text.literal(title), (int)(x / textScale.get()), (int)(y / textScale.get()), 0xFFFF55)
    ctx.getMatrices().pop()

    // Einträge rendern
    int i = 1
    for (ScoreboardEntry entry : sorted):
        int entryY = y + i * lineHeight
        ctx.getMatrices().push()
        ctx.getMatrices().scale(textScale.get(), textScale.get(), 1f)
        String name = entry.owner()
        String score = String.valueOf(entry.value())
        ctx.drawTextWithShadow(tr, Text.literal(name), (int)(x / textScale.get()), (int)(entryY / textScale.get()), 0xFFFFFF)
        ctx.drawTextWithShadow(tr, Text.literal(score), (int)((x + maxWidth - tr.getWidth(score)) / textScale.get()), (int)(entryY / textScale.get()), 0xFF5555)
        ctx.getMatrices().pop()
        i++
```

### Edge Cases

- `objective == null`: Kein Sidebar-Objective aktiv → nichts rendern. Guard-Clause.
- Leere Eintrags-Liste: Guard-Clause nach `entries` Check.
- `entry.owner()` kann Team-Präfixe enthalten (z. B. `§aPlayerName`): Formatting-Codes werden von `textRenderer.getWidth()` korrekt behandelt.
- Sidebar ändert sich während Rendering: `getScoreboardEntries` liefert immer den aktuellen Stand. Kein State gecacht.
- `maxEntries` > tatsächliche Eintragsanzahl: `limit()` ist sicher wenn Limit > Listengröße.
- `text_scale` < 1.0: `ctx.getMatrices().scale()` funktioniert korrekt für Verkleinerung. Aber Koordinaten müssen durch `textScale` geteilt werden.
- Scoreboard nicht sichtbar (F1-Mode): Vanilla blendet HUD komplett aus. Das eigene HUD wird über `onRenderHud` gerendert, welches ebenfalls nicht aufgerufen wird wenn F1 aktiv ist — korrekt.
- Team-Farben im Scoreboard: `entry.owner()` kann bereits Team-Farb-Codes enthalten. Wird korrekt gerendert.

## Translation Keys

```json
"noctra.scoreboard_hud.name": "Scoreboard HUD",
"noctra.scoreboard_hud.description": "Replaces the vanilla scoreboard sidebar with a customizable HUD element.",
"noctra.scoreboard_hud.hud_x": "HUD X",
"noctra.scoreboard_hud.hud_y": "HUD Y",
"noctra.scoreboard_hud.text_scale": "Text Scale",
"noctra.scoreboard_hud.background_alpha": "Background Alpha",
"noctra.scoreboard_hud.max_entries": "Max Entries",
"noctra.scoreboard_hud.hide_vanilla": "Hide Vanilla"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/scoreboard_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Stilisierte Ranglisten-Tabelle (drei horizontale Striche mit Nummern 1/2/3 links). Farbe: Weiß/Gelb auf dunklem Hintergrund.
