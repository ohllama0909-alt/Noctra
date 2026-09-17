# Death Coordinates

**ID:** `death_coordinates`  
**Category:** UTILITY  
**Status:** [x] DONE  
**Class:** `modules/impl/DeathCoordinatesModule.java`

## Description

Speichert bei jedem Spielertod automatisch die genauen Koordinaten und die Dimension. Die letzten N Todesörter werden persistent in `config/mandatory_deaths.json` gespeichert und können jederzeit über ein HUD oder einen Chat-Befehl abgerufen werden. Hilft dabei, nach dem Tod schnell zum Drop-Ort zurückzufinden.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `max_entries` | Int | `5` | `1–20` | "Max Entries" | Maximale Anzahl gespeicherter Tode |
| `show_hud` | Boolean | `true` | — | "Show HUD" | Letzten Todesort dauerhaft als HUD anzeigen |
| `hud_x` | Int | `4` | `0–1920` | "HUD X" | HUD-X-Position (Pixel vom linken Rand) |
| `hud_y` | Int | `4` | `0–1080` | "HUD Y" | HUD-Y-Position (Pixel vom oberen Rand) |

## Implementation

### Event Hooks

- `onClientTick(MinecraftClient client)` — Prüft jeden Tick ob der Spieler gerade gestorben ist (`player.isDead() && !wasDeadLastTick`). Falls ja: Koordinaten, Dimension und Timestamp erfassen und in die Liste eintragen, dann persistieren.
- `onRenderHud(DrawContext ctx, RenderTickCounter counter)` — Wenn `show_hud` aktiv und mindestens ein Eintrag vorhanden: letzten Todesort als Text auf dem HUD zeichnen.
- `onJoinWorld(ClientWorld world)` — Death-Liste aus Datei nachladen, `wasDeadLastTick` zurücksetzen.

### Required Mixins

Kein Mixin erforderlich. Die Tod-Erkennung erfolgt polling-basiert via `onClientTick`.

### Core Algorithm

```
Felder in der Klasse:
    private boolean wasDeadLastTick = false;
    private final List<DeathEntry> deaths = new ArrayList<>();
    // DeathEntry: record { BlockPos pos, String dimension, long timestamp }

onClientTick(MinecraftClient client):
    if (client.player == null || client.world == null) return
    boolean isDead = client.player.isDead()
    if (isDead && !wasDeadLastTick):
        BlockPos pos       = client.player.getBlockPos()
        String dimension   = client.world.getRegistryKey().getValue().toString()
        long timestamp     = System.currentTimeMillis()
        deaths.add(0, new DeathEntry(pos, dimension, timestamp))    // neueste zuerst
        while (deaths.size() > maxEntries.get()) deaths.remove(deaths.size() - 1)
        saveToFile()
        sendChatMessage(client, pos, dimension)
    wasDeadLastTick = isDead

onRenderHud(DrawContext ctx, RenderTickCounter counter):
    if (!showHud.get() || deaths.isEmpty()) return
    DeathEntry last = deaths.get(0)
    String text = "Last death: " + formatPos(last.pos()) + " [" + shortDim(last.dimension()) + "]"
    ctx.drawTextWithShadow(textRenderer, Text.literal(text), hudX.get(), hudY.get(), 0xFF5555)

saveToFile():
    Gson gson = new Gson()
    Path path = FabricLoader.getInstance().getConfigDir().resolve("mandatory_deaths.json")
    Files.writeString(path, gson.toJson(deaths))

loadFromFile():
    Path path = FabricLoader.getInstance().getConfigDir().resolve("mandatory_deaths.json")
    if (Files.exists(path)):
        // Gson deserialization into List<DeathEntry>

shortDim(String dim):
    // "minecraft:overworld" → "OW", "minecraft:the_nether" → "NT", "minecraft:the_end" → "END"
    // alles andere: letztes Segment nach ":"

sendChatMessage(MinecraftClient client, BlockPos pos, String dimension):
    Text msg = Text.literal("[NoctraMod] Died at " + formatPos(pos) + " in " + shortDim(dimension))
        .styled(s -> s.withColor(0xFF5555))
    client.inGameHud.getChatHud().addMessage(msg)
```

### Edge Cases

- `client.player == null` oder `client.world == null`: Guard-Clause zu Beginn von `onClientTick`.
- Spieler stirbt mehrfach ohne Respawn (z. B. Instant-Kill in derselben Tick-Gruppe): `wasDeadLastTick` verhindert doppelte Einträge.
- Respawn-Screen: Spieler ist `isDead()` während der Respawn-Screen offen ist. Eintrag wird korrekt beim ersten Tod-Tick erstellt, nicht erneut beim Klick auf "Respawn".
- `mandatory_deaths.json` beschädigt oder nicht lesbar: `loadFromFile()` muss `try/catch` haben; bei Fehler mit leerer Liste starten.
- Singleplayer vs. Multiplayer: Koordinaten funktionieren in beiden Fällen. Auf Multiplayer kennt der Client die genaue Position vor dem Tod (letzte bekannte Serverposition), also kein Problem.
- Creative Mode: Spieler kann in Creative nicht sterben (`isDead()` wird nicht true). Kein Eintrag wird erstellt — erwünschtes Verhalten.
- Modul deaktiviert während Spieler tot ist: `wasDeadLastTick` bleibt true, nächster Tick nach Re-Enable ignoriert den bereits-toten Zustand korrekt.
- HUD-Position außerhalb des Bildschirms: Keine Auto-Clamp, aber IntSetting Range begrenzt sinnvoll.

## Translation Keys

```json
"noctra.death_coordinates.name": "Death Coordinates",
"noctra.death_coordinates.description": "Saves your last death positions and shows them on the HUD.",
"noctra.death_coordinates.max_entries": "Max Entries",
"noctra.death_coordinates.show_hud": "Show HUD",
"noctra.death_coordinates.hud_x": "HUD X",
"noctra.death_coordinates.hud_y": "HUD Y"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/death_coordinates.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Roter Totenkopf oder ein Grabstein mit einem kleinen Koordinaten-Pin. Farbe: Rot/Dunkelrot auf dunklem Hintergrund.
