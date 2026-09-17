# FPS & Ping Display

**ID:** `fps_ping_display`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/fps_ping_display/FpsPingDisplayModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.fps_ping_display`

## System Notes (Updated)

- Module Ordner: `modules/impl/fps_ping_display/FpsPingDisplayModule.java`
- Package: `de.snenjih.noctra.modules.impl.fps_ping_display`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Zeigt die aktuelle Bildwiederholrate (FPS) und den Server-Ping (Latenz in ms) als kompaktes HUD-Element an. Hilfreich bei der Performance-Diagnose und um zu sehen, ob die Verbindung zum Server stabil ist. Auf Singleplayer wird der Ping-Wert automatisch ausgeblendet oder als "SP" dargestellt.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `2` | `0–1920` | "X Position" | Horizontale Position |
| `y_pos` | Int | `2` | `0–1080` | "Y Position" | Vertikale Position |
| `show_fps` | Boolean | `true` | — | "Show FPS" | FPS-Wert anzeigen |
| `show_ping` | Boolean | `true` | — | "Show Ping" | Ping-Wert anzeigen |
| `text_color` | Int | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Textfarbe |
| `color_fps_good` | Int | `0xFF55FF55` | ARGB Hex | "FPS Color Good" | Farbe bei hoher FPS (>= good_threshold) |
| `color_fps_bad` | Int | `0xFFFF5555` | ARGB Hex | "FPS Color Bad" | Farbe bei niedriger FPS (< bad_threshold) |
| `fps_good_threshold` | Int | `60` | `10–300` | "FPS Good (≥)" | Ab dieser FPS grüne Farbe |
| `fps_bad_threshold` | Int | `30` | `5–120` | "FPS Bad (<)" | Unter dieser FPS rote Farbe |
| `color_ping_good` | Int | `0xFF55FF55` | ARGB Hex | "Ping Color Good" | Farbe bei gutem Ping (<= ping_good) |
| `color_ping_bad` | Int | `0xFFFF5555` | ARGB Hex | "Ping Color Bad" | Farbe bei schlechtem Ping (> ping_bad) |
| `ping_good_threshold` | Int | `80` | `1–500` | "Ping Good (≤ ms)" | Bis zu diesem Ping grüne Farbe |
| `ping_bad_threshold` | Int | `200` | `50–1000` | "Ping Bad (> ms)" | Über diesem Ping rote Farbe |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — FPS und Ping auslesen und rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
onRenderHud(DrawContext ctx, float tickDelta):
    if (mc.player == null) return

    int x = xPos.get()
    int y = yPos.get()
    int currentY = y
    List<String> lines = new ArrayList<>()
    List<Integer> colors = new ArrayList<>()

    // FPS
    if (showFps.get()):
        int fps = mc.getCurrentFps()
        int fpsColor
        if (fps >= fpsGoodThreshold.get())      fpsColor = colorFpsGood.get()
        else if (fps < fpsBadThreshold.get())   fpsColor = colorFpsBad.get()
        else                                     fpsColor = textColor.get()
        lines.add("FPS: " + fps)
        colors.add(fpsColor)

    // Ping
    if (showPing.get()):
        int ping = getPing(mc)
        if (ping < 0):
            lines.add("Ping: SP")
            colors.add(textColor.get())
        else:
            int pingColor
            if (ping <= pingGoodThreshold.get())     pingColor = colorPingGood.get()
            else if (ping > pingBadThreshold.get())  pingColor = colorPingBad.get()
            else                                      pingColor = textColor.get()
            lines.add("Ping: " + ping + "ms")
            colors.add(pingColor)

    // Hintergrund
    if (background.get()):
        int maxW = lines.stream().mapToInt(l -> mc.textRenderer.getWidth(l)).max().orElse(50)
        ctx.fill(x - 2, y - 2, x + maxW + 2, y + lines.size() * 10 + 2, 0x88000000)

    // Rendern
    for (int i = 0; i < lines.size(); i++):
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(lines.get(i)), x, y + i * 10, colors.get(i))

Hilfsmethode getPing(MinecraftClient mc):
    // Singleplayer: mc.getNetworkHandler() vorhanden, aber latency = 0
    // Echter Multiplayer-Ping:
    try:
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid())
        if (entry == null) return -1
        return entry.getLatency()
    catch (Exception e):
        return -1
```

### Edge Cases

- `mc.player == null`: Guard-Clause.
- Singleplayer: `getNetworkHandler()` ist nicht null (integrierter Server), aber `getLatency()` ist `0`. Anzeige: "SP" oder `0ms` — via `showPing` konfigurierbar. Ein Wert von 0 in SP ist kein echter Ping.
- `getNetworkHandler()` == null (sehr kurz nach Spielstart): `try-catch` in `getPing()` fängt NPE ab, gibt `-1` zurück.
- `getPlayerListEntry()` null: Passiert kurz nach dem Verbinden. Gibt `-1` zurück → zeigt "SP".
- FPS-Wert schwankt stark: Kein Smoothing implementiert — Raw-Wert wie F3-Screen.

## Translation Keys

```json
"noctra.fps_ping_display.name": "FPS & Ping",
"noctra.fps_ping_display.description": "Shows current FPS and server ping on screen.",
"noctra.fps_ping_display.x_pos": "X Position",
"noctra.fps_ping_display.y_pos": "Y Position",
"noctra.fps_ping_display.show_fps": "Show FPS",
"noctra.fps_ping_display.show_ping": "Show Ping",
"noctra.fps_ping_display.text_color": "Text Color",
"noctra.fps_ping_display.color_fps_good": "FPS Color Good",
"noctra.fps_ping_display.color_fps_bad": "FPS Color Bad",
"noctra.fps_ping_display.fps_good_threshold": "FPS Good (≥)",
"noctra.fps_ping_display.fps_bad_threshold": "FPS Bad (<)",
"noctra.fps_ping_display.color_ping_good": "Ping Color Good",
"noctra.fps_ping_display.color_ping_bad": "Ping Color Bad",
"noctra.fps_ping_display.ping_good_threshold": "Ping Good (≤ ms)",
"noctra.fps_ping_display.ping_bad_threshold": "Ping Bad (> ms)",
"noctra.fps_ping_display.background": "Background"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/fps_ping_display.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Kleines Diagramm oder Speedometer-Symbol mit "60" als Zahl; daneben ein WLAN-Balken-Symbol. Grüne Signalfarbe.
