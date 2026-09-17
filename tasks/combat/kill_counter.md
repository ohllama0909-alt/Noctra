# Kill Counter

**ID:** `kill_counter`  
**Category:** COMBAT  
**Status:** [x] DONE  
**Class:** `modules/impl/kill_counter/KillCounterModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.kill_counter`

## System Notes (Updated)

- Module Ordner: `modules/impl/kill_counter/KillCounterModule.java`
- Package: `de.snenjih.noctra.modules.impl.kill_counter`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Zählt Kills des Spielers in der aktuellen Session und zeigt die Anzahl als HUD-Element an. Ein "Kill" wird gezählt, wenn ein von diesem Spieler angegriffenes Entity in derselben Session stirbt. Der Zähler wird beim Verlassen der Welt oder beim Deaktivieren des Moduls zurückgesetzt. Nützlich beim PvP, beim Monster-Farming oder zum Tracken persönlicher Meilensteine.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `5` | `0–1920` | "X Position" | Horizontale Position des HUD-Elements |
| `y_pos` | Int | `60` | `0–1080` | "Y Position" | Vertikale Position des HUD-Elements |
| `show_session_time` | Boolean | `false` | — | "Show Session Time" | Zeigt die Zeit seit Modul-Aktivierung neben dem Kill-Count an |
| `reset_on_death` | Boolean | `false` | — | "Reset on Death" | Kill-Counter zurücksetzen wenn der Spieler stirbt |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund zeichnen |
| `text_color` | Int | `0xFFFFFFFF` | ARGB | "Text Color" | Farbe des Textes |

## Implementation

### Event Hooks

- `onAttackEntity(ClientPlayerEntity player, Entity target)` — Merkt sich, welche Entities der Spieler angegriffen hat (als Set von Entity-IDs).
- `onClientTick(MinecraftClient client)` — Prüft bei jedem Tick, ob eines der gemerkten Entities gestorben ist. Bei Tod: Kill-Counter erhöhen, Entity aus der Tracking-Liste entfernen.
- `onRenderHud(DrawContext ctx, RenderTickCounter counter)` — Kill-Count und optional Session-Zeit rendern.

### Required Mixins

Kein Mixin erforderlich. Kill-Detection erfolgt über Entity-Gesundheits-Polling im `onClientTick`.

Für den `reset_on_death`-Fall:
- Option A: `ClientPlayNetworkHandler` Packet-Listener für `DeathScreenS2CPacket` via Mixin.
- Option B: Im `onClientTick` `player.isDead()` oder `player.getHealth() <= 0f` prüfen.

Option B wird empfohlen (kein Mixin nötig):
```java
// In onClientTick, nach dem Entity-Check:
if (resetOnDeath.get() && client.player != null && client.player.isDead()) {
    if (!wasDeadLastTick) {
        killCount = 0;
        sessionKills.clear();
    }
    wasDeadLastTick = true;
} else {
    wasDeadLastTick = false;
}
```

### Core Algorithm

```
// Felder in der Modul-Klasse:
private int killCount = 0
private final Set<Integer> attackedEntityIds = new HashSet<>()
// Integer = Entity.getId() (client-seitige Entity-ID, eindeutig pro Welt-Session)
private long sessionStartTime = 0L    // System.currentTimeMillis() beim onEnable
private boolean wasDeadLastTick = false

onEnable():
  killCount = 0
  attackedEntityIds.clear()
  sessionStartTime = System.currentTimeMillis()

onDisable():
  killCount = 0
  attackedEntityIds.clear()

onAttackEntity(player, target):
  // Entity zur Tracking-Liste hinzufügen
  // Nur LivingEntity zählt (Blöcke, Items etc. scheiden aus)
  if (target instanceof LivingEntity):
    attackedEntityIds.add(target.getId())

onClientTick(client):
  if client.player == null || client.world == null → return

  // Über alle getracken Entity-IDs iterieren und prüfen ob gestorben
  Iterator<Integer> iter = attackedEntityIds.iterator()
  while iter.hasNext():
    int entityId = iter.next()
    Entity entity = client.world.getEntityById(entityId)

    if entity == null:
      // Entity nicht mehr in der Welt — entweder despawnt oder gestorben
      // Wenn es ein LivingEntity war und es nicht mehr existiert, ist es wahrscheinlich tot
      // Konservative Herangehensweise: Entity aus Liste entfernen, KEIN Kill zählen
      // (könnte auch Chunk-Unload sein)
      iter.remove()
      continue

    if entity instanceof LivingEntity living && living.getHealth() <= 0f:
      // Entity ist tot und war von uns angegriffen worden → Kill
      killCount++
      iter.remove()
      continue

    if entity.isRemoved():
      // Entity wurde aus der Welt entfernt (despawn, Chunk unload)
      iter.remove()

  // Reset on death
  if resetOnDeath.get():
    boolean isDead = client.player.isDead()
    if isDead && !wasDeadLastTick:
      killCount = 0
      attackedEntityIds.clear()
    wasDeadLastTick = isDead

onRenderHud(ctx, counter):
  mc = MinecraftClient.getInstance()
  if mc.player == null → return
  if mc.options.hudHidden → return

  int x = xPos.get()
  int y = yPos.get()

  String killText = "Kills: " + killCount

  if showSessionTime.get():
    long elapsedMs = System.currentTimeMillis() - sessionStartTime
    long seconds = elapsedMs / 1000
    long minutes = seconds / 60
    seconds = seconds % 60
    killText += String.format(" (%02d:%02d)", minutes, seconds)

  if background.get():
    int textWidth = mc.textRenderer.getWidth(killText)
    ctx.fill(x - 2, y - 2, x + textWidth + 2, y + 10, 0x88000000)

  ctx.drawTextWithShadow(mc.textRenderer, Text.literal(killText), x, y, textColor.get())
```

**Kill-Detection — Warum Polling statt Event:**
Vanilla Fabric bietet kein `EntityDeathCallback` auf dem Client. Der Server sendet zwar `EntityStatusS2CPacket` mit Status `3` (EntityStatus.PLAY_DEATH_SOUND), aber dessen Abfangen würde ein Mixin auf `ClientPlayNetworkHandler.onEntityStatus()` erfordern. Die Polling-Lösung (Gesundheit prüfen) ist einfacher und zuverlässig, da der Client die HP via `EntityTrackerUpdate` synchronisiert bekommt.

**Problem: Chunk-Unload vs. Tod**
Wenn ein Entity den Render-Radius verlässt, wird es aus `client.world` entfernt — `getEntityById()` gibt `null` zurück. Das ist ununterscheidbar vom Tod ohne zusätzliche State-Speicherung. Lösung: Die letzte bekannte Gesundheit des Entities mittracking:

```java
// Erweitertes Tracking mit letzter bekannter HP:
private final Map<Integer, Float> trackedEntities = new HashMap<>()
// Key: entity.getId(), Value: letzte bekannte getHealth()

onAttackEntity: trackedEntities.put(target.getId(), ((LivingEntity)target).getHealth())
onClientTick:
  für jede ID in trackedEntities:
    entity = world.getEntityById(id)
    if entity == null:
      // Chunk-Unload-Heuristik: Wenn letzte bekannte HP <= 3.0f, Kill annehmen
      // (Entity in Todes-Nähe war, dann Chunk unloaded). Sonst: kein Kill.
      if trackedEntities.get(id) <= 3.0f: killCount++
      trackedEntities.remove(id)
    else if ((LivingEntity)entity).getHealth() <= 0:
      killCount++
      trackedEntities.remove(id)
    else:
      trackedEntities.put(id, ((LivingEntity)entity).getHealth())  // HP aktualisieren
```

Empfehlung für Erstimplementierung: Einfacheres Set ohne HP-Tracking verwenden; Chunk-Unload-Fehler tolerieren.

### Edge Cases

- **Entity stirbt ohne Spieler-Beteiligung danach:** Das Entity bleibt in `attackedEntityIds`, wird aber beim nächsten Tick mit `getHealth() <= 0` als Kill gewertet. Das ist beabsichtigt — der Spieler hat Schaden beigetragen.
- **Spieler verlässt Welt:** `onDisable()` wird beim Mod-Toggle aufgerufen; beim Weltaustritt muss der Counter zusätzlich via `ClientPlayConnectionEvents.DISCONNECT` (Fabric API) oder in `onClientTick` mit `client.world == null`-Check zurückgesetzt werden.
- **Mob stirbt durch Lava nach Angriff:** Wird korrekt als Kill gezählt (HP sinkt auf 0, Entity noch in der Welt).
- **Mob despawnt ohne zu sterben (z.B. Bat bei Tag):** `isRemoved()` true ohne `getHealth() <= 0` — kein Kill. Korrekt.
- **Liste wächst unbegrenzt:** Bei langen Sessions viele Mobs angreifen ohne dass sie sterben. Begrenzen: `if attackedEntityIds.size() > 200: attackedEntityIds.clear()` als Sicherheitsnetz.
- **Mehrfacher Angriff auf dasselbe Entity:** `Set.add()` ist idempotent — kein Problem.

## Translation Keys

```json
"noctra.kill_counter.name": "Kill Counter",
"noctra.kill_counter.description": "Counts your kills this session and displays them on the HUD.",
"noctra.kill_counter.x_pos": "X Position",
"noctra.kill_counter.y_pos": "Y Position",
"noctra.kill_counter.show_session_time": "Show Session Time",
"noctra.kill_counter.reset_on_death": "Reset on Death",
"noctra.kill_counter.background": "Background",
"noctra.kill_counter.text_color": "Text Color"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/kill_counter.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Ein Schwert-Symbol mit einer kleinen Zahl "0" oder "III" in der Ecke, ähnlich wie ein Tally-Mark-Muster. Blutroter Akzent oder Totenkopf-Silhouette im Hintergrund.
