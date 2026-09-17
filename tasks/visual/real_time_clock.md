# Real Time Clock

**ID:** `real_time_clock`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/real_time_clock/RealTimeClockModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.real_time_clock`

## System Notes (Updated)

- Module Ordner: `modules/impl/real_time_clock/RealTimeClockModule.java`
- Package: `de.snenjih.noctra.modules.impl.real_time_clock`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Zeigt die echte Systemzeit (Uhrzeit des PCs) als HUD-Overlay auf dem Bildschirm an. Verhindert, dass Spieler die Zeit vergessen, während sie im Spiel versunken sind. Optional kann das Datum und eine 12/24-Stunden-Darstellung gewählt werden.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `2` | `0–1920` | "X Position" | Horizontale Position |
| `y_pos` | Int | `46` | `0–1080` | "Y Position" | Vertikale Position |
| `format_24h` | Boolean | `true` | — | "24h Format" | 24-Stunden-Format (false = 12h AM/PM) |
| `show_seconds` | Boolean | `true` | — | "Show Seconds" | Sekunden anzeigen |
| `show_date` | Boolean | `false` | — | "Show Date" | Datum (DD.MM.YYYY) anzeigen |
| `text_color` | Int | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Textfarbe |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Systemzeit aus Java `LocalTime` lesen und rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
Imports benötigt:
    import java.time.LocalDateTime
    import java.time.format.DateTimeFormatter

Felder (gecachte Formatter, nur einmalig instanziieren):
    private DateTimeFormatter time24    = DateTimeFormatter.ofPattern("HH:mm:ss")
    private DateTimeFormatter time24NoS = DateTimeFormatter.ofPattern("HH:mm")
    private DateTimeFormatter time12    = DateTimeFormatter.ofPattern("hh:mm:ss a")
    private DateTimeFormatter time12NoS = DateTimeFormatter.ofPattern("hh:mm a")
    private DateTimeFormatter dateFmt   = DateTimeFormatter.ofPattern("dd.MM.yyyy")

onRenderHud(DrawContext ctx, float tickDelta):
    LocalDateTime now = LocalDateTime.now()

    // Formatter auswählen
    DateTimeFormatter timeFmt
    if (format24h.get()):
        timeFmt = showSeconds.get() ? time24 : time24NoS
    else:
        timeFmt = showSeconds.get() ? time12 : time12NoS

    String timeStr = now.format(timeFmt)
    String dateStr = showDate.get() ? now.format(dateFmt) : null

    int x = xPos.get()
    int y = yPos.get()
    int currentY = y

    // Hintergrund
    if (background.get()):
        int maxW = Math.max(
            mc.textRenderer.getWidth(timeStr),
            dateStr != null ? mc.textRenderer.getWidth(dateStr) : 0
        ) + 4
        int bgH = (dateStr != null ? 20 : 10) + 4
        ctx.fill(x - 2, y - 2, x + maxW, y + bgH, 0x88000000)

    // Zeit rendern
    ctx.drawTextWithShadow(mc.textRenderer, Text.literal(timeStr), x, currentY, textColor.get())
    currentY += 10

    // Datum rendern (optional)
    if (dateStr != null):
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(dateStr), x, currentY, textColor.get())
```

### Edge Cases

- `mc.player == null`: Da keine Spielerdaten verwendet werden, kann das Modul auch ohne eingeloggten Spieler rendern. Trotzdem Guard empfehlenswert, um sicherzustellen dass ein aktives Spiel läuft (`mc.world != null`).
- `LocalDateTime.now()` wirft nie Exceptions unter normalen Umständen — kein Try-Catch nötig.
- Zeitzonen: `LocalDateTime.now()` verwendet die System-Zeitzone des PCs — gewünschtes Verhalten.
- 12h-Format zeigt "12:00:00 PM" oder "12:00:00 AM" — Standard Java-Verhalten, kein Extra-Handling nötig.
- Formatter als Felder cachen (nicht in jedem Frame neu erstellen) um GC-Druck zu minimieren; alternativ `static final`.
- Unterschiedliche Locales: `DateTimeFormatter.ofPattern()` ohne explizite Locale ist plattformunabhängig für Zahlenmuster — akzeptabel.

## Translation Keys

```json
"noctra.real_time_clock.name": "Real Time Clock",
"noctra.real_time_clock.description": "Shows your real system time as an on-screen overlay.",
"noctra.real_time_clock.x_pos": "X Position",
"noctra.real_time_clock.y_pos": "Y Position",
"noctra.real_time_clock.format_24h": "24h Format",
"noctra.real_time_clock.show_seconds": "Show Seconds",
"noctra.real_time_clock.show_date": "Show Date",
"noctra.real_time_clock.text_color": "Text Color",
"noctra.real_time_clock.background": "Background"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/real_time_clock.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Analoge Uhr mit Zeigern (Pixel-Art) oder digitale Anzeige "12:00" im Retro-7-Segment-Stil; helle Ziffern auf dunklem Hintergrund.
