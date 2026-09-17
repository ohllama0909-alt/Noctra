# Day Counter HUD

**ID:** `day_counter`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/day_counter/DayCounterModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.day_counter`

## Description

Zeigt den aktuellen Minecraft-Welttag sowie optional die Tageszeit auf dem HUD an. Der Welttag wird aus `world.getTimeOfDay()` berechnet (`÷ 24000`). Nützlich für Survival-Challenges, Zeitplanung (Vollmond, Farm-Timings) und Speedruns. Optional wird die Mondphase (0–7) angezeigt da diese an den Tag gebunden ist (Tag % 8).

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `show_time_of_day` | Boolean | `true` | — | "Show Time of Day" | MC-Tageszeit als Uhrzeit anzeigen (z.B. 06:00) |
| `show_moon_phase` | Boolean | `false` | — | "Show Moon Phase" | Mondphase (0–7) anzeigen |
| `show_night_warning` | Boolean | `true` | — | "Show Night Warning" | Warnung wenn es Nacht wird (Mob-Spawn) |
| `compact_mode` | Boolean | `false` | — | "Compact Mode" | Alles in einer Zeile |
| `time_format` | Boolean | `true` | — | "24h Format" | 24h-Format (true) oder 12h mit AM/PM (false) |
| `color_day` | Color | `0xFFFFFF55` | ARGB Hex | "Day Color" | Textfarbe bei Tag |
| `color_night` | Color | `0xFF8899CC` | ARGB Hex | "Night Color" | Textfarbe bei Nacht |
| `moon_phase_names` | Boolean | `true` | — | "Moon Phase Names" | Mondphasennamen statt Nummern (Full Moon etc.) |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Tageszeit und Tag aus Weltzeit berechnen.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
// Minecraft-Zeitsystem:
// world.getTimeOfDay() = Ticks seit Weltbeginn (steigt immer weiter)
// world.getTimeOfDay() % 24000 = Ticks im aktuellen Tag (0 = 06:00 Uhr ingame)
// Tageszeit-Ticks:  0 = 06:00, 6000 = 12:00, 12000 = 18:00, 18000 = 00:00
// Nacht beginnt bei Tick ~13000 (18:32), Tag bei ~23000 (05:32)

onRenderHud(ctx, tickDelta):
    if (mc.world == null || mc.player == null) return

    long totalTicks  = mc.world.getTimeOfDay()
    long dayTicks    = totalTicks % 24000L
    long dayNumber   = totalTicks / 24000L + 1   // Tag 1 = erster Tag

    // Mondphase (zykliert alle 8 Tage)
    int moonPhase = (int) ((dayNumber - 1) % 8)
    String[] phaseNames = {"Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent",
                            "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous"}

    // MC-Tageszeit in echte Uhrzeit umrechnen:
    // Tick 0 = 06:00 → Stunden = (dayTicks / 1000 + 6) % 24
    // Minuten = (int) ((dayTicks % 1000) / 1000.0 * 60)
    int mcHour   = (int) ((dayTicks / 1000 + 6) % 24)
    int mcMinute = (int) ((dayTicks % 1000) / 1000.0 * 60)

    boolean isNight = dayTicks >= 13000 && dayTicks < 23000
    boolean isNightWarn = dayTicks >= 12000 && dayTicks < 13000   // kurz vor Nacht

    // Zeitstring
    String timeStr
    if (timeFormat.get()):   // 24h
        timeStr = String.format("%02d:%02d", mcHour, mcMinute)
    else:                    // 12h AM/PM
        int h12 = mcHour % 12; if (h12 == 0) h12 = 12
        timeStr = String.format("%d:%02d %s", h12, mcMinute, mcHour < 12 ? "AM" : "PM")

    // Farbe
    int timeColor = isNight ? colorNight.get() : colorDay.get()
    if (isNightWarn && showNightWarning.get()) timeColor = 0xFFFF9933

    // Rendering
    if (compactMode.get()):
        StringBuilder sb = new StringBuilder("Day " + dayNumber)
        if (showTimeOfDay.get())  sb.append("  ").append(timeStr)
        if (showMoonPhase.get())  sb.append("  ").append(moonPhaseNames.get() ? phaseNames[moonPhase] : "Phase " + moonPhase)
        drawText(ctx, sb.toString(), x + 4, y + 4, timeColor)
    else:
        int lineY = y + 4
        drawText(ctx, "Day: " + dayNumber, x + 4, lineY, textColor.get());  lineY += 10
        if (showTimeOfDay.get()):
            drawText(ctx, "Time: " + timeStr, x + 4, lineY, timeColor);  lineY += 10
        if (showMoonPhase.get()):
            String phase = moonPhaseNames.get() ? phaseNames[moonPhase] : "Moon: " + moonPhase
            drawText(ctx, phase, x + 4, lineY, 0xFF99AACC)
```

### Edge Cases

- `world.getTimeOfDay()` kann in bestimmten Dimensionen (End, Nether) feststehend oder anders sein — Nether/End hat keine Tageszeit. Guard: Nur anzeigen wenn Overworld (`world.getRegistryKey().equals(World.OVERWORLD)`), andernfalls "Dimension: No Day Cycle".
- Tag-0-Problem: `dayNumber = 1` für den ersten Tag (user-friendly).
- Mondphase-Array: Index 0 = Full Moon. Minecraft-intern ist Phase 0 ebenfalls Full Moon.
- 12h-Format: 00:xx → 12:xx AM (nicht 0:xx).

## Translation Keys

```json
"noctra.day_counter.name": "Day Counter",
"noctra.day_counter.description": "Shows the current Minecraft day and time of day.",
"noctra.day_counter.bg_color": "Background Color",
"noctra.day_counter.border_color": "Border Color",
"noctra.day_counter.text_color": "Text Color",
"noctra.day_counter.text_shadow": "Text Shadow",
"noctra.day_counter.text_scale": "Text Scale",
"noctra.day_counter.show_background": "Show Background",
"noctra.day_counter.show_time_of_day": "Show Time of Day",
"noctra.day_counter.show_moon_phase": "Show Moon Phase",
"noctra.day_counter.show_night_warning": "Show Night Warning",
"noctra.day_counter.compact_mode": "Compact Mode",
"noctra.day_counter.time_format": "24h Format",
"noctra.day_counter.color_day": "Day Color",
"noctra.day_counter.color_night": "Night Color",
"noctra.day_counter.moon_phase_names": "Moon Phase Names"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/day_counter.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Sonne und Mond nebeneinander über einem Kalenderblatt. Pixel-Art, gelb/blau zweigeteilt.
