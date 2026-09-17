# XP & Level HUD

**ID:** `xp_level_hud`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/xp_level_hud/XpLevelHudModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.xp_level_hud`

## Description

Zeigt das aktuelle Erfahrungslevel, den genauen XP-Fortschritt im aktuellen Level sowie die Gesamtanzahl der angesammelten XP-Punkte auf dem HUD an. Vanilla zeigt nur den Level und einen simplen Balken — dieses Modul macht die genauen Zahlen sichtbar. Optional: Ziel-Level einstellen und anzeigen wie viele XP noch fehlen. Besonders nützlich für XP-Farming und Enchanting-Planung.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `show_bar` | Boolean | `true` | — | "Show Bar" | XP-Balken anzeigen |
| `show_raw_xp` | Boolean | `false` | — | "Show Raw XP" | Gesamte XP-Punkte anzeigen |
| `show_xp_to_next` | Boolean | `true` | — | "Show XP to Next" | XP bis nächstes Level anzeigen |
| `target_level` | Int | `30` | `0–100` | "Target Level" | Ziel-Level für "XP needed" Berechnung |
| `show_target_xp` | Boolean | `false` | — | "Show Target XP" | Fehlende XP bis Ziel-Level anzeigen |
| `compact_mode` | Boolean | `false` | — | "Compact Mode" | Alles in einer Zeile |
| `xp_bar_color` | Color | `0xFF7FFF00` | ARGB Hex | "XP Bar Color" | Farbe des XP-Balkens |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — XP-Werte aus Player lesen.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
// XP-Formeln in Minecraft (vanilla):
// Für Level 0–16:  XP_to_next = 2*level + 7
// Für Level 17–31: XP_to_next = 5*level - 38
// Für Level 32+:   XP_to_next = 9*level - 158

// Gesamte XP für ein Level (kumuliert):
// Level 0–16:  totalXp = level^2 + 6*level
// Level 17–31: totalXp = 2.5*level^2 - 40.5*level + 360
// Level 32+:   totalXp = 4.5*level^2 - 162.5*level + 2220

static int xpToNextLevel(int level):
    if (level <= 15)  return 2 * level + 7
    if (level <= 30)  return 5 * level - 38
    return 9 * level - 158

static int totalXpForLevel(int level):
    if (level <= 16)  return level * level + 6 * level
    if (level <= 31)  return (int)(2.5 * level * level - 40.5 * level + 360)
    return (int)(4.5 * level * level - 162.5 * level + 2220)

onRenderHud(ctx, tickDelta):
    if (mc.player == null) return

    int level       = mc.player.experienceLevel
    float progress  = mc.player.experienceProgress   // 0.0–1.0 innerhalb des aktuellen Levels
    int toNext      = xpToNextLevel(level)
    int xpInLevel   = Math.round(progress * toNext)  // XP im aktuellen Level
    int totalXp     = totalXpForLevel(level) + xpInLevel

    // XP bis Ziel-Level
    int xpNeeded    = 0
    if (showTargetXp.get() && targetLevel.get() > level):
        int targetTotal = totalXpForLevel(targetLevel.get())
        xpNeeded = Math.max(0, targetTotal - totalXp)

    // Rendering
    int lineY = y + 4
    if (compactMode.get()):
        String line = "Lv " + level + "  " + xpInLevel + "/" + toNext
        if (showRawXp.get()) line += "  [" + totalXp + " XP]"
        drawText(ctx, line, x + 4, lineY, 0xFF7FFF00)
    else:
        drawText(ctx, "Level: " + level, x + 4, lineY, 0xFF7FFF00);  lineY += 10
        if (showXpToNext.get()):
            drawText(ctx, "XP: " + xpInLevel + " / " + toNext, x + 4, lineY, textColor.get());  lineY += 10
        if (showRawXp.get()):
            drawText(ctx, "Total: " + totalXp, x + 4, lineY, 0xFF99CC55);  lineY += 10
        if (showTargetXp.get() && targetLevel.get() > level):
            drawText(ctx, "To Lv " + targetLevel.get() + ": " + xpNeeded, x + 4, lineY, 0xFFAA8800)

    if (showBar.get()):
        int barX = x + 4; int barY = lineY + 2; int barW = getDefaultWidth() - 8; int barH = 4
        int fill = (int) (barW * progress)
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF333333)
        if (fill > 0) ctx.fill(barX, barY, barX + fill, barY + barH, xpBarColor.get())
```

### Edge Cases

- `experienceProgress` ist ein float 0.0–1.0, kann durch Floating-Point kurz über 1.0 oder unter 0.0 gehen → `Math.max(0f, Math.min(1f, progress))` clampen.
- Level > 100: `xpToNextLevel` gibt sehr große Zahlen zurück — kein Problem, int reicht bis Level ~300.
- `targetLevel <= level`: `xpNeeded = 0`, Anzeige: "Target reached" oder `show_target_xp` ausblenden.
- Keine Vanilla-XP-Bar überschreiben — das HUD rendert eigenständig, beeinflusst die Vanilla-Bar nicht.

## Translation Keys

```json
"noctra.xp_level_hud.name": "XP & Level",
"noctra.xp_level_hud.description": "Shows your experience level and XP progress in detail.",
"noctra.xp_level_hud.bg_color": "Background Color",
"noctra.xp_level_hud.border_color": "Border Color",
"noctra.xp_level_hud.text_color": "Text Color",
"noctra.xp_level_hud.text_shadow": "Text Shadow",
"noctra.xp_level_hud.text_scale": "Text Scale",
"noctra.xp_level_hud.show_background": "Show Background",
"noctra.xp_level_hud.show_bar": "Show Bar",
"noctra.xp_level_hud.show_raw_xp": "Show Raw XP",
"noctra.xp_level_hud.show_xp_to_next": "Show XP to Next",
"noctra.xp_level_hud.target_level": "Target Level",
"noctra.xp_level_hud.show_target_xp": "Show Target XP",
"noctra.xp_level_hud.compact_mode": "Compact Mode",
"noctra.xp_level_hud.xp_bar_color": "XP Bar Color"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/xp_level_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Grüner XP-Orb (Pixel-Art) mit einer Zahl daneben oder einem Balken darunter. Hellgrüne Akzentfarbe.
