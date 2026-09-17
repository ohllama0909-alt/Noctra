# MC Time Display

**ID:** `mc_time_display`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/mc_time_display/McTimeDisplayModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.mc_time_display`

## System Notes (Updated)

- Module Ordner: `modules/impl/mc_time_display/McTimeDisplayModule.java`
- Package: `de.snenjih.noctra.modules.impl.mc_time_display`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Zeigt die aktuelle Minecraft-Weltzeit als Uhrzeit (HH:MM) und optional die Tagesphase (Tag, Dämmerung, Nacht, Morgengrauen) sowie die absolute Tagnummer als HUD-Element an. Nützlich beim Planen von Aktivitäten (z. B. Warten auf Nacht für Mob-Spawning oder auf Tag für sicheres Reisen).

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `show_day_number` | Boolean | `true` | — | "Show Day Number" | Aktuelle Tagnummer anzeigen |
| `show_phase` | Boolean | `true` | — | "Show Phase" | Tagesphase (Day/Dusk/Night/Dawn) anzeigen |
| `hud_x` | Int | `4` | `0–1920` | "HUD X" | X-Position des HUD-Elements |
| `hud_y` | Int | `4` | `0–1080` | "HUD Y" | Y-Position des HUD-Elements |
| `time_format` | Enum | `HOURS_24` | `HOURS_24`, `HOURS_12` | "Time Format" | 24h oder 12h Zeitformat |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, RenderTickCounter counter)` — Liest Weltzeit aus, berechnet Uhrzeit und Tagesphase, rendert HUD.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
Enum TimeFormat { HOURS_24, HOURS_12 }

// Tagesphase-Konstanten (in Ticks, 0 = 06:00 Uhr Minecraft-Zeit):
// 0–12000:   TAG (Day)
// 12000–13800: DÄMMERUNG (Dusk)
// 13800–22200: NACHT (Night)
// 22200–24000: MORGENGRAUEN (Dawn)

onRenderHud(DrawContext ctx, RenderTickCounter counter):
    MinecraftClient mc = MinecraftClient.getInstance()
    if (mc.world == null) return

    long timeOfDay = mc.world.getTimeOfDay()
    long dayTime = timeOfDay % 24000L          // Tageszeit (0–23999)
    long dayNumber = timeOfDay / 24000L + 1    // Tagnummer (1-basiert)

    // Minecraft-Zeit → Real-Zeit Konversion
    // Tick 0 = 06:00, Tick 6000 = 12:00, Tick 12000 = 18:00, Tick 18000 = 00:00
    int hour = (int)((dayTime / 1000L + 6L) % 24L)
    int minute = (int)((dayTime % 1000L) * 60L / 1000L)

    String timeStr
    if (timeFormat.get() == TimeFormat.HOURS_24):
        timeStr = String.format("%02d:%02d", hour, minute)
    else:
        int h12 = hour % 12
        if (h12 == 0) h12 = 12
        String ampm = hour < 12 ? "AM" : "PM"
        timeStr = String.format("%d:%02d %s", h12, minute, ampm)

    // Tagesphase bestimmen
    String phase
    int phaseColor
    if (dayTime < 12000):
        phase = "Day";      phaseColor = 0xFFFF55   // Gelb
    else if (dayTime < 13800):
        phase = "Dusk";     phaseColor = 0xFF8800   // Orange
    else if (dayTime < 22200):
        phase = "Night";    phaseColor = 0x5555FF   // Blau
    else:
        phase = "Dawn";     phaseColor = 0xFF88AA   // Rosa

    TextRenderer tr = mc.textRenderer
    int x = hudX.get()
    int y = hudY.get()

    // Uhrzeit rendern
    ctx.drawTextWithShadow(tr, Text.literal(timeStr), x, y, phaseColor)

    // Tagnummer (nächste Zeile)
    if (showDayNumber.get()):
        ctx.drawTextWithShadow(tr, Text.literal("Day " + dayNumber), x, y + 10, 0xAAAAAA)

    // Tagesphase (optional, selbe Zeile wie Uhrzeit oder darunter)
    if (showPhase.get()):
        String phaseLabel = "[" + phase + "]"
        int phaseX = x + tr.getWidth(timeStr) + 4
        ctx.drawTextWithShadow(tr, Text.literal(phaseLabel), phaseX, y, phaseColor)
```

### Edge Cases

- `mc.world == null`: Guard-Clause. Passiert im Hauptmenü oder beim Laden.
- `timeOfDay` ist negativ: Kann in bestimmten Modded-Welten vorkommen. `Math.floorMod(timeOfDay, 24000L)` statt `%` nutzen für korrekte Modulo-Arithmetik mit negativen Zahlen.
- Ultraweit geschrittener Tag (Spielzeit > Long.MAX_VALUE / 24000): Theoretisch unrealistisch, aber `long` reicht für Millionen von Tagen.
- Gamerule `doDaylightCycle false`: Zeit ändert sich nicht. Der angezeigte Wert ist trotzdem korrekt (fest bei dem Tick wo es pausiert wurde).
- `getTimeOfDay()` in 1.21.11: Liefert absolute Welt-Ticks. Die Formel oben ist vanilla-kompatibel.
- HUD-Überlappung mit anderen Elementen: Keine Auto-Positionierung, User konfiguriert X/Y.
- 12h-Format Mitternacht: `hour = 0 → h12 = 12, AM` — korrekt.
- Multiplayer vs. Singleplayer: `mc.world.getTimeOfDay()` funktioniert auf beiden, da der Client die Zeit vom Server bekommt.

## Translation Keys

```json
"noctra.mc_time_display.name": "MC Time Display",
"noctra.mc_time_display.description": "Shows Minecraft world time and day phase on the HUD.",
"noctra.mc_time_display.show_day_number": "Show Day Number",
"noctra.mc_time_display.show_phase": "Show Phase",
"noctra.mc_time_display.hud_x": "HUD X",
"noctra.mc_time_display.hud_y": "HUD Y",
"noctra.mc_time_display.time_format": "Time Format"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/mc_time_display.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Analoguhr oder Sonnensymbol mit einer digitalen Zeitanzeige. Farbe: Gold/Gelb für die Sonne, Dunkelblau für den Hintergrund.
