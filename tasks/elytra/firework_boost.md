# Firework Boost

**ID:** `firework_boost`  
**Category:** ELYTRA  
**Status:** [x] DONE  
**Class:** `modules/impl/firework_boost/FireworkBoostModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.firework_boost`

## System Notes (Updated)

- Module Ordner: `modules/impl/firework_boost/FireworkBoostModule.java`
- Package: `de.snenjih.noctra.modules.impl.firework_boost`
- Implementiert HudElement: Nein
- `KeybindSetting` für Boost-Keybind verwenden

## Description

Verwendet automatisch Feuerwerksraketen aus dem Inventar, wenn die horizontale Fluggeschwindigkeit
unter einen konfigurierbaren Schwellenwert fällt. Das Modul sucht zunächst in der Hotbar,
dann im Hauptinventar nach Raketen. Zwischen zwei Boosts wird ein konfigurierbarer Cooldown
eingehalten, um Raketen nicht in einem einzelnen Tick zu verbrauchen.

Nutzen: Verhindert ungewollte Notlandungen auf langen Flügen — der Pilot muss sich nur
um die Richtung kümmern, nicht ständig manuell boosten.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `min_speed` | Float | `10.0` | `1.0 – 50.0` | "Min Speed (b/s)" | Unterschreitet die horizontale Geschwindigkeit diesen Wert, wird geboosted. |
| `cooldown_ticks` | Int | `20` | `1 – 200` | "Boost Cooldown (ticks)" | Minimale Ticks zwischen zwei automatischen Boosts. 20 Ticks = 1 Sekunde. |
| `prefer_hotbar` | Boolean | `true` | `true / false` | "Prefer Hotbar" | Hotbar-Slots zuerst durchsuchen, bevor das Hauptinventar durchsucht wird. |

## Implementation

### Event Hooks

- `onClientTick(MinecraftClient client)` — Geschwindigkeit prüfen, Cooldown dekrementieren,
  Feuerwerk-Slot suchen und `interactItem` auslösen.

### Required Mixins

Kein Mixin erforderlich. Das Verwenden von Feuerwerk kann über
`mc.interactionManager.interactItem(player, hand)` ausgelöst werden, nachdem die Rakete
in die Haupthand gebracht wurde — oder direkt mit einem Inventory-Click auf den richtigen
Slot und anschließendem `interactItem`. Einfachere Methode: Rakete per `clickSlot` in den
aktiven Hotbar-Slot tauschen, dann `interactItem` aufrufen, danach zurücktauschen. Oder:
direkt `NetworkHandler.sendPacket(PlayerInteractItemC2SPacket)` — aber das ist zu tief.

Die sauberste vanilla-kompatible Methode ist: Rakete in einen freien Hotbar-Slot (oder
den aktiven Slot selbst, falls schon dort) schieben und `interactItem` auf `MAIN_HAND`
oder `OFF_HAND` aufrufen.

### Core Algorithm

```
Felder im Modul:
  private int cooldownRemaining = 0;

1. In onClientTick(client):
   a. Wenn client.player == null || !player.isGliding() → return (Cooldown weiter dekrementieren)
   b. if (cooldownRemaining > 0) { cooldownRemaining--; return; }
   c. Vec3d vel = player.getVelocity()
   d. double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20.0
   e. Wenn hSpeed >= min_speed.get() → return (noch schnell genug)
   f. int rocketSlot = findRocket(player, prefer_hotbar.get())
   g. Wenn rocketSlot == -1:
        // Kein Feuerwerk → Warnung in ActionBar
        player.sendMessage(Text.translatable("noctra.firework_boost.no_rockets"), true)
        cooldownRemaining = 40  // 2s Cooldown für die Warnung
        return
   h. Boost auslösen:
        triggerBoost(mc, player, rocketSlot)
   i. cooldownRemaining = cooldown_ticks.get()

2. findRocket(player, preferHotbar):
   // Hotbar: Slots 0–8 im Inventar (index im PlayerInventory)
   // Hauptinventar: Slots 9–35
   int[] searchOrder = preferHotbar
       ? concat([0..8], [9..35])
       : concat([9..35], [0..8])
   for (int i : searchOrder) {
       ItemStack s = player.getInventory().getStack(i)
       if (s.getItem() == Items.FIREWORK_ROCKET) return i
   }
   return -1

3. triggerBoost(mc, player, inventorySlot):
   // Strategie: aktiven Hotbar-Slot merken, Rakete dorthin tauschen (wenn nötig),
   // interactItem aufrufen, ursprünglichen Slot wiederherstellen.
   //
   // Einfachste Methode: Rakete liegt bereits in Hotbar (inventorySlot 0–8)?
   //   → aktiven Slot auf inventorySlot setzen (setSelectedSlot via Vanilla Packet)
   //     und interactItem aufrufen
   // Rakete liegt im Hauptinventar (inventorySlot 9–35)?
   //   → Slot-Swap via clickSlot: 
   //       int syncId = player.playerScreenHandler.syncId
   //       int hotbarSlotAbsolute = 36 + player.getInventory().getSelectedSlot()
   //       // Temp-Tausch: Rakete → Hotbar, dann interactItem, dann zurück
   //       // ABER: clickSlot + interactItem im selben Tick kann zu Race Conditions führen
   //       // Besser: Rakete in Offhand-Slot (Slot 45) tauschen und OFFHAND verwenden
   //
   // EMPFEHLUNG: Offhand-Strategie
   //   a. Merke was in Offhand ist: offhandStack = player.getOffHandStack()
   //   b. clickSlot(syncId, inventorySlotInScreen, ...) Rakete in Offhand
   //   c. mc.interactionManager.interactItem(player, Hand.OFF_HAND)
   //   d. Im nächsten Tick (wenn Rakete verbraucht): Offhand zurücktauschen
   //
   // ALTERNATIV (einfachste, robust): Rakete per swap in aktiven Hotbar-Slot bringen
   //   Hotbar-Slot-Nummern in PlayerScreenHandler: selectedSlot + 36 (für Main-Inventar-Clicks)
   //   Feuerwerk via PICKUP → selectedSlot → PICKUP auf Rakete-Slot → PICKUP auf selectedSlot

   // Konkrete Implementierung (robust, ein Tick):
   int syncId = player.playerScreenHandler.syncId
   int activeAbsSlot = 36 + player.getInventory().getSelectedSlot()
   
   // Rakete-Slot in PlayerScreenHandler-Koordinaten:
   // Inventar-Slot 0–8 (Hotbar) = PSH-Slot 36–44
   // Inventar-Slot 9–35 (Haupt) = PSH-Slot 9–35
   int pshSlot = (inventorySlot < 9) ? 36 + inventorySlot : inventorySlot
   
   if (pshSlot == activeAbsSlot) {
       // Rakete schon im aktiven Slot → direkt verwenden
       mc.interactionManager.interactItem(player, Hand.MAIN_HAND)
   } else {
       // Rakete via SWAP (Q-Taste äquivalent) in aktiven Hotbar-Slot tauschen:
       // SlotActionType.SWAP mit button = activeHotbarIndex (0–8)
       int hotbarIndex = player.getInventory().getSelectedSlot()  // 0–8
       mc.interactionManager.clickSlot(syncId, pshSlot, hotbarIndex, SlotActionType.SWAP, player)
       mc.interactionManager.interactItem(player, Hand.MAIN_HAND)
       // Zurücktauschen (jetzt liegt was anderes im pshSlot):
       mc.interactionManager.clickSlot(syncId, pshSlot, hotbarIndex, SlotActionType.SWAP, player)
   }
```

### Slot-Koordinaten-Mapping

`PlayerInventory.getStack(i)` gibt den Stack an Index `i` zurück:
- 0–8: Hotbar (0 = ganz links)
- 9–35: Hauptinventar
- 36–39: Rüstung (Helm, Brust, Hose, Stiefel)
- 40: Offhand

`PlayerScreenHandler`-Slots (für `clickSlot`):
- 5–8: Rüstung (Helm=5, Brust=6, Hose=7, Stiefel=8)
- 9–35: Hauptinventar
- 36–44: Hotbar (36 = Slot 0)
- 45: Offhand

`SlotActionType.SWAP` mit `button = hotbarIndex` (0–8) tauscht den angeklickten Slot
mit dem entsprechenden Hotbar-Index-Slot aus, ohne das Cursor-Item zu benutzen.

### Edge Cases

- **Kein Feuerwerk im Inventar**: ActionBar-Meldung, Cooldown von 40 Ticks auf die
  Warnung legen, damit sie nicht jeden Tick erscheint
- **Spieler ist nicht am Gleiten**: Früher Return; kein Boost, kein Cooldown-Abbau
- **Spieler in Creative Mode**: `interactItem` funktioniert, verbraucht aber keine Items.
  Kein Problem, aber ggf. Warnung unterdrücken
- **Rakete im aktiven Slot beim SWAP**: SWAP eines Slots mit sich selbst ist idempotent
  — SWAP-Button muss dem Hotbar-Index entsprechen. Wenn `pshSlot == 36 + hotbarIndex`,
  dann ist es bereits der aktive Slot → direkt `interactItem`
- **Mehrere Raketen werden verbraucht**: Cooldown `cooldown_ticks` verhindert dies;
  Standard 20 Ticks = 1 Rakete/s
- **Gleiter startet gerade** (sehr niedrige Geschwindigkeit am Anfang): `hSpeed` ist kurz
  nach dem Absprung tatsächlich niedrig. Um Boost direkt beim Abfliegen zu verhindern,
  könnte ein kurzer Startup-Cooldown von ~40 Ticks nach `player.isGliding()` === true
  eingebaut werden. Optional als Setting.
- **ScreenHandler-Sync-ID wechselt**: `player.playerScreenHandler.syncId` wird beim
  Öffnen eines Containers geändert. Prüfen ob `client.currentScreen == null` bevor
  Slot-Manipulation stattfindet
- **Slot-Manipulation mit offenem Inventar**: Guard: `if (client.currentScreen != null) return`
- **Offerwork in Offhand bereits vorhanden**: Wenn der Spieler manuell eine Rakete in
  der Offhand hält, wird durch das Modul nichts verändert — `triggerBoost` prüft
  zunächst die Inventar-Slots, nicht die Offhand separat

## Translation Keys

```json
"noctra.firework_boost.name": "Firework Boost",
"noctra.firework_boost.description": "Automatically uses firework rockets to maintain glide speed.",
"noctra.firework_boost.min_speed": "Min Speed (b/s)",
"noctra.firework_boost.cooldown_ticks": "Boost Cooldown (ticks)",
"noctra.firework_boost.prefer_hotbar": "Prefer Hotbar",
"noctra.firework_boost.no_rockets": "No fireworks in inventory!"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/firework_boost.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Feuerwerksrakete die nach links-oben fliegt, mit kleinem Flammen-Trail.
Alternativ eine Elytra-Silhouette mit einer Rakete darunter. Pixel-Art, orange/gelb auf
dunklem Hintergrund.
