# Auto Shield

**ID:** `auto_shield`  
**Category:** COMBAT  
**Status:** [x] DONE  
**Class:** `modules/impl/auto_shield/AutoShieldModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.auto_shield`

## System Notes (Updated)

- Module Ordner: `modules/impl/auto_shield/AutoShieldModule.java`
- Package: `de.snenjih.noctra.modules.impl.auto_shield`
- Implementiert HudElement: Nein (reagiert auf Schadensereignisse)
- Verwendet `onClientTick` und ggf. Mixin auf Schadens-Events

## Description

Nimmt automatisch das Schild in der Offhand hoch, wenn der Spieler einen Angriff oder ein Projektil bekommt. Ersetzt das manuelle Halten der rechten Maustaste für den Schildblock. Auf dem Client-seitigen Shield-Activation-Mechanismus basierend: Das Modul sendet das `PlayerInteractItemC2SPacket` für die Offhand, sobald eine Bedrohung erkannt wird, und setzt den Blockier-Zustand zurück, wenn die Bedrohung vorbei ist.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `react_to_arrows` | Boolean | `true` | — | "React to Arrows" | Shield hochnehmen wenn ein Pfeil in der Nähe erkannt wird |
| `react_to_melee` | Boolean | `true` | — | "React to Melee" | Shield hochnehmen wenn `hurtTime` aktiv ist (nach einem Treffer) |
| `hold_duration` | Int | `10` | `1–40` | "Hold Duration (ticks)" | Wie lange das Schild gehalten wird nach der letzten Bedrohung (in Ticks) |
| `arrow_detection_radius` | Float | `8.0` | `2.0–20.0` | "Arrow Radius" | Radius in Blöcken, in dem Pfeile erkannt werden |

## Implementation

### Event Hooks

- `onClientTick(MinecraftClient client)` — Bedrohungen erkennen, Shield-Paket senden oder zurückziehen.

### Required Mixins

Das Shield-Hochnehmen erfordert das Senden eines Netzwerk-Pakets an den Server, da `ClientPlayerInteractionManager.interactItem()` auf dem Client ausgeführt werden muss. Zwei Ansätze:

**Ansatz A: Direkter Methodenaufruf (bevorzugt)**
```java
// ClientPlayerInteractionManager.interactItem() ist die korrekte API:
mc.interactionManager.interactItem(player, Hand.OFF_HAND)
// Diese Methode sendet intern das PlayerInteractItemC2SPacket und gibt ActionResult zurück.
// Kein Mixin nötig — interactItem() ist public.
```

**Ansatz B: Mixin auf interactItem (nur falls Ansatz A geblockt wird)**
- **Class:** `mixin/ShieldInteractMixin.java`
- **Target:** `net.minecraft.client.network.ClientPlayerInteractionManager`
- **Injection:** `@Inject(at = @At("HEAD"), method = "interactItem(...)")`

**Empfehlung: Ansatz A.** `ClientPlayerInteractionManager.interactItem(PlayerEntity, Hand)` ist public und direkt aufrufbar. Kein Mixin erforderlich.

**Shield deaktivieren:** Das Schild wird deaktiviert, indem die rechte Maustaste losgelassen wird. Client-seitig entspricht das dem Senden von `PlayerActionC2SPacket` mit `Action.RELEASE_USE_ITEM`. Alternativ: `player.stopUsingItem()` auf dem Client aufrufen (löst die Paket-Übertragung intern aus).

### Core Algorithm

```
// Felder in der Modul-Klasse:
private int shieldHoldTicksRemaining = 0
private boolean isShieldActive = false

onEnable():
  shieldHoldTicksRemaining = 0
  isShieldActive = false

onDisable():
  // Shield zurückziehen falls aktiv
  if isShieldActive:
    deactivateShield()

onClientTick(client):
  player = client.player
  if player == null → return
  if client.currentScreen != null → return
  if player.isCreative() || player.isSpectator() → return

  // Prüfe ob Shield in Offhand verfügbar
  if !hasShieldInOffhand(player) → return

  // Bedrohungs-Erkennung
  boolean threatened = false

  // 1. Melee: hurtTime ist > 0 wenn der Spieler kürzlich getroffen wurde
  if reactToMelee.get() && player.hurtTime > 0:
    threatened = true

  // 2. Projektil: Pfeile in der Nähe suchen
  if reactToArrows.get() && !threatened:
    float radius = arrowDetectionRadius.get()
    Box searchBox = player.getBoundingBox().expand(radius)
    List<Entity> nearbyEntities = client.world.getEntitiesByClass(
        PersistentProjectileEntity.class,  // Pfeile, Tridents
        searchBox,
        e -> e.isAlive()
    )
    // Prüfen ob ein Pfeil sich auf den Spieler zubewegt
    for Entity proj : nearbyEntities:
      if isProjectileApproaching(proj, player):
        threatened = true
        break

  // Shield-Aktivierung / Deaktivierung
  if threatened:
    shieldHoldTicksRemaining = holdDuration.get()
    if !isShieldActive:
      activateShield(client, player)

  if shieldHoldTicksRemaining > 0:
    shieldHoldTicksRemaining--
  else if isShieldActive:
    deactivateShield(client, player)

// Hilfsmethoden:
private boolean hasShieldInOffhand(ClientPlayerEntity player):
  ItemStack offhand = player.getOffHandStack()
  return offhand.isOf(Items.SHIELD)

private void activateShield(MinecraftClient mc, ClientPlayerEntity player):
  ActionResult result = mc.interactionManager.interactItem(player, Hand.OFF_HAND)
  if result != ActionResult.FAIL:
    isShieldActive = true

private void deactivateShield(MinecraftClient mc, ClientPlayerEntity player):
  // Schild loslassen: RELEASE_USE_ITEM Paket senden
  player.stopUsingItem()
  // Alternativ: Netzwerk-Paket direkt senden:
  // mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
  //     PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
  //     BlockPos.ORIGIN, Direction.DOWN, 0))
  isShieldActive = false

private boolean isProjectileApproaching(Entity proj, ClientPlayerEntity player):
  // Prüfe ob die Geschwindigkeit des Projektils grob auf den Spieler zeigt
  Vec3d toPlayer = player.getPos().subtract(proj.getPos()).normalize()
  Vec3d velocity = proj.getVelocity().normalize()
  // Dot-Produkt > 0.5 bedeutet Winkel < 60° zur Spieler-Richtung
  return toPlayer.dotProduct(velocity) > 0.5
```

**Shield-Aktivierung — Mechanismus:**
`ClientPlayerInteractionManager.interactItem(player, Hand.OFF_HAND)` löst auf dem Client aus:
1. `player.startUsingItem(Hand.OFF_HAND)` (setzt `usingItem` State)
2. Sendet `PlayerInteractItemC2SPacket` an den Server
3. Server validiert und aktiviert den Schildblock

Der Server lehnt die Anfrage ab, wenn:
- Das Item in der Offhand kein Schild ist
- Der Spieler gerade angreift (kurzer Cooldown nach Angriff)
- Der Spieler im Creative-Modus ist

**`hurtTime` für Melee-Detection:**
`player.hurtTime` wird auf dem Client via `EntityTrackerUpdate` synchronisiert. Es ist `10` direkt nach einem Treffer und zählt jede Tick um 1 runter. Ein Wert `> 0` signalisiert, dass der Spieler kürzlich getroffen wurde. Caveat: Das Schild wird erst NACH dem Treffer hochgenommen — das ist reaktiv, nicht präventiv. Für echtes "Pre-Hit"-Shield wäre komplexes Projektil-Tracking nötig (→ `react_to_arrows`).

### Edge Cases

- **Kein Shield in Offhand:** `hasShieldInOffhand()` gibt `false` zurück — kein Aktivierungsversuch. `isShieldActive` bleibt `false`.
- **Shield in Offhand, aber schon blockierend (`player.isBlocking()`):** Nicht nochmals `interactItem()` aufrufen. Guard: `if (isShieldActive || player.isBlocking()) return` vor `activateShield()`.
- **Spieler greift selbst an:** Nach einem Angriff hat das Schild einen kurzen Cooldown serverseitig. Der Server lehnt `interactItem` ab, aber der Client-State `isShieldActive` wird trotzdem `true`. Besser: `player.getItemUseTime() == 0 && !player.isAttacking()` als zusätzlicher Guard.
- **Shield-Cooldown nach Treffer:** Wenn ein Schild geblockt wurde und zerbricht, hat es einen 5-Sekunden-Cooldown. `player.getItemCooldownManager().isCoolingDown(Items.SHIELD)` prüfen → wenn true, nicht aktivieren.
- **Multishot-Pfeile:** Mehrere Pfeile werden als mehrere Entitäten erkannt — das ist korrekt, `threatened` wird trotzdem nur einmal `true`.
- **Tridents und andere Projektile:** `PersistentProjectileEntity` deckt Pfeile und Tridents ab. `FireballEntity` und `WitherSkullEntity` sind davon ausgeschlossen — diese können separat über `ThrownEntity` erkannt werden.
- **Spieler in Wasser / blockiert:** Shield funktioniert in Wasser normal. In Minecraft kann man unter Wasser blocken.
- **Modul deaktiviert während Shield aktiv:** `onDisable()` ruft `deactivateShield()` auf um den State aufzuräumen.

## Translation Keys

```json
"noctra.auto_shield.name": "Auto Shield",
"noctra.auto_shield.description": "Automatically raises your shield when attacked or an arrow approaches.",
"noctra.auto_shield.react_to_arrows": "React to Arrows",
"noctra.auto_shield.react_to_melee": "React to Melee",
"noctra.auto_shield.hold_duration": "Hold Duration",
"noctra.auto_shield.arrow_detection_radius": "Arrow Radius"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/auto_shield.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Das vanilla Schild-Item aus der Vorderansicht, mit einem kleinen Blitz- oder Pfeil-Symbol in der Ecke, das "automatische Reaktion" symbolisiert. Blaue oder silberne Farbgebung.
