# Server Address HUD

**ID:** `server_address_hud`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/server_address_hud/ServerAddressHudModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.server_address_hud`

## Description

Zeigt die Adresse oder den Namen des aktuell verbundenen Servers dauerhaft auf dem HUD an. Nützlich wenn man mehrere Server nutzt und Screenshots macht — man sieht immer sofort auf welchem Server man war. In Singleplayer wird der Welt-Name angezeigt. Optional: Nur auf Multiplayer sichtbar, mit konfigurierbarem Alias (eigener Name statt der rohen IP).

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFF8899AA` | ARGB Hex | "Text Color" | Textfarbe der Serveradresse |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `show_ip` | Boolean | `true` | — | "Show IP" | Rohe IP/Domain anzeigen (true) oder nur Server-Name (false) |
| `custom_alias` | Text | `""` | max 32 Zeichen | "Custom Alias" | Eigener Anzeigename statt IP (leer = IP verwenden) |
| `hide_port` | Boolean | `true` | — | "Hide Port" | Standard-Port (:25565) ausblenden |
| `only_on_multiplayer` | Boolean | `true` | — | "Only Multiplayer" | In Singleplayer ausblenden |
| `show_on_singleplayer` | Boolean | `false` | — | "Show in Singleplayer" | In SP den Welt-Namen anzeigen |
| `truncate_length` | Int | `30` | `5–64` | "Max Length" | Maximale Zeichenanzahl (längere IPs werden abgeschnitten) |
| `show_ping` | Boolean | `false` | — | "Show Ping" | Ping neben der Adresse anzeigen |
| `prefix` | Text | `""` | max 16 Zeichen | "Prefix" | Optionales Präfix (z.B. "Server: ") |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Server-Info aus NetworkHandler lesen.
- `onJoinWorld(ClientWorld world)` — Serveradresse beim Betreten cachen.
- `onLeaveWorld()` — Cache leeren.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
// Felder:
private String cachedAddress  = null
private String cachedName     = null
private boolean isSingleplayer = false

onJoinWorld(world):
    MinecraftClient mc = MinecraftClient.getInstance()
    isSingleplayer = mc.isInSingleplayer()
    
    if (isSingleplayer):
        // Welt-Name aus IntegratedServer
        if (mc.getServer() != null):
            cachedName    = mc.getServer().getSaveProperties().getLevelName()
            cachedAddress = "Singleplayer"
    else:
        // Multiplayer: ServerInfo aus mc.getCurrentServerEntry()
        ServerInfo info = mc.getCurrentServerEntry()
        if (info != null):
            cachedAddress = info.address     // z.B. "play.hypixel.net" oder "192.168.1.1:25565"
            cachedName    = info.name        // Nutzer-definierter Name aus der Server-Liste
        else:
            // Direct Connect ohne gespeicherte Server-Info
            // Adresse aus NetworkHandler
            try:
                var nh = mc.getNetworkHandler()
                if (nh != null):
                    var conn = nh.getConnection()
                    var addr = conn.getAddress()
                    cachedAddress = addr.toString()   // InetSocketAddress.toString()
                    cachedName    = null
            catch:
                cachedAddress = "Unknown"

onLeaveWorld():
    cachedAddress = null
    cachedName    = null

onRenderHud(ctx, tickDelta):
    if (mc.player == null) return
    if (isSingleplayer && onlyOnMultiplayer.get()) return
    if (isSingleplayer && !showOnSingleplayer.get()) return
    
    // Anzeigetext bestimmen
    String display
    if (!customAlias.get().isEmpty()):
        display = customAlias.get()
    else if (!showIp.get() && cachedName != null && !cachedName.isEmpty()):
        display = cachedName
    else if (cachedAddress != null):
        display = cachedAddress
        // Port ausblenden
        if (hidePort.get()):
            display = display.replace(":25565", "")
        // Truncaten
        if (display.length() > truncateLength.get()):
            display = display.substring(0, truncateLength.get()) + "…"
    else:
        return   // Keine Info verfügbar → nichts anzeigen
    
    // Prefix
    String fullText = prefix.get() + display
    
    // Ping (optional)
    String pingStr = null
    if (showPing.get() && !isSingleplayer && mc.getNetworkHandler() != null):
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid())
        if (entry != null) pingStr = " [" + entry.getLatency() + "ms]"
    
    if (pingStr != null) fullText += pingStr
    
    int w = Math.max(80, mc.textRenderer.getWidth(fullText) + 8)
    int h = 18
    
    if (showBackground.get()):
        ctx.fill(x, y, x + w, y + h, bgColor.get())
        ctx.drawStrokedRectangle(x, y, w, h, borderColor.get())
    
    drawText(ctx, fullText, x + 4, y + 4, textColor.get())
```

**Hinweis zu `mc.getCurrentServerEntry()`:** Gibt `null` zurück wenn der Spieler per "Direct Connect" verbunden ist ohne den Server in der Liste gespeichert zu haben. Der Fallback auf `NetworkHandler.getConnection().getAddress()` ist dann nötig, gibt aber eine `InetSocketAddress` zurück deren `.toString()` Format `"/192.168.1.1:25565"` ist (mit führendem Slash) — diesen Slash trimmen.

### Edge Cases

- `getCurrentServerEntry() == null` (Direct Connect): Fallback auf Connection-Adresse mit Trimming.
- Singleplayer: `mc.getServer()` → Welt-Name aus `getSaveProperties().getLevelName()`. Kann null sein direkt nach dem Laden.
- `cachedAddress` nach Reconnect: `onJoinWorld()` wird erneut aufgerufen → Cache wird neu befüllt.
- `InetSocketAddress.toString()`: Format ist `"hostname/IP:Port"` oder `"/IP:Port"` — beides richtig trimmen: Alles nach dem letzten `/` oder den Text vor dem `/` verwenden.
- Sehr lange Sub-Domain-Adressen: `truncate_length` Setting schützt vor HUD-Overflow.
- `custom_alias`: Wenn gesetzt, wird immer der Alias angezeigt — egal ob SP oder MP.

## Translation Keys

```json
"noctra.server_address_hud.name": "Server Address",
"noctra.server_address_hud.description": "Displays the current server address or name on the HUD.",
"noctra.server_address_hud.bg_color": "Background Color",
"noctra.server_address_hud.border_color": "Border Color",
"noctra.server_address_hud.text_color": "Text Color",
"noctra.server_address_hud.text_shadow": "Text Shadow",
"noctra.server_address_hud.text_scale": "Text Scale",
"noctra.server_address_hud.show_background": "Show Background",
"noctra.server_address_hud.show_ip": "Show IP",
"noctra.server_address_hud.custom_alias": "Custom Alias",
"noctra.server_address_hud.hide_port": "Hide Port",
"noctra.server_address_hud.only_on_multiplayer": "Only Multiplayer",
"noctra.server_address_hud.show_on_singleplayer": "Show in Singleplayer",
"noctra.server_address_hud.truncate_length": "Max Length",
"noctra.server_address_hud.show_ping": "Show Ping",
"noctra.server_address_hud.prefix": "Prefix"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/server_address_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** WLAN/Server-Symbol (drei gebogene Balken) über einem kleinen Haus-Pixel. Blaugrau Farbton.
