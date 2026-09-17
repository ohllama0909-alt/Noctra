# Elytra Landing Swap

**ID:** `elytra_landing_swap`  
**Category:** ELYTRA  
**Status:** [x] DONE  
**Class:** `modules/impl/elytra_landing_swap/ElytraLandingSwapModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.elytra_landing_swap`

## System Notes (Updated)

- Module Ordner: `modules/impl/elytra_landing_swap/ElytraLandingSwapModule.java`
- Package: `de.snenjih.noctra.modules.impl.elytra_landing_swap`
- Implementiert HudElement: Nein
- Logik: `onClientTick` — erkennt Landung und führt Inventory-Swap durch

## Description

Tauscht die Elytra automatisch gegen eine Chestplate aus, kurz bevor der Spieler aufkommt.
Das Modul überwacht jede Tick die vertikale Fallgeschwindigkeit und die Distanz zum Boden
(via Raycast nach unten). Sobald beide Triggerbedingungen erfüllt sind — Fallgeschwindigkeit
überschreitet den Schwellenwert UND der Boden ist in konfigurierter Reichweite — wird der
Swap ausgelöst. Der Swap erfolgt genau einmal pro Flug (nicht wiederholt, bis der Spieler
wieder in die Luft geht).

Nutzen: Verhindert Fallschaden durch fehlende Rüstung nach dem Gleiten, ohne dass der
Spieler den Swap manuell timen muss.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `trigger_speed` | Float | `8.0` | `1.0 – 30.0` | "Trigger Speed (b/s)" | Vertikale Fallgeschwindigkeit (nach unten, positiv angegeben), ab der der Swap ausgelöst wird. 8 b/s entspricht ~4 Blöcken pro Sekunde Fallgeschwindigkeit — noch kein Schaden, aber kurz davor. |
| `lookahead_distance` | Float | `6.0` | `1.0 – 20.0` | "Look-ahead Distance (m)" | Maximale Entfernung zum Boden in Blöcken, bei der der Swap ausgelöst wird. Raycast nach unten über diese Distanz. |
| `swap_back` | Boolean | `false` | `true / false` | "Auto Swap Back" | Nach der Landung (isOnGround == true) Chestplate automatisch zurück gegen Elytra tauschen. Nur wenn Chestplate nach dem Landing-Swap im Chest-Slot sitzt. |

## Implementation

### Event Hooks

- `onClientTick(MinecraftClient client)` — Fallgeschwindigkeit überwachen, Raycast
  ausführen, Swap auslösen.

### Required Mixins

Kein Mixin erforderlich. Raycast ist über `client.world.raycastBlock()` / `World.raycast()`
verfügbar. Slot-Manipulation läuft via `interactionManager.clickSlot()`.

### Core Algorithm

```
Felder im Modul:
  private boolean swapTriggered = false;   // verhindert Doppel-Swap pro Flug
  private boolean wasGliding    = false;   // Flug-Phasenerkennung
  private int     chestSlotUsed = -1;      // Chestplate-Slot merken für swap_back

1. In onClientTick(client):
   a. Wenn client.player == null || client.world == null → return
   b. boolean gliding = player.isGliding()

   // Reset: Spieler ist gelandet und war vorher am Gleiten
   c. Wenn !gliding && wasGliding:
        wasGliding    = false
        swapTriggered = false
        // Wenn swap_back.get() == true UND wir getauscht hatten:
        if (swap_back.get() && chestSlotUsed != -1 && player.isOnGround()):
            scheduleSwapBack(mc, player)
            chestSlotUsed = -1

   // Neue Glide-Session starten
   d. Wenn gliding && !wasGliding:
        wasGliding    = true
        swapTriggered = false
        chestSlotUsed = -1

   // Swap-Bedingung prüfen (nur einmal pro Flug)
   e. Wenn !gliding || swapTriggered → return

   f. Vec3d vel = player.getVelocity()
   g. double vSpeed = -vel.y * 20.0  // positiv wenn fallend (vel.y ist negativ beim Fallen)
   h. Wenn vSpeed < trigger_speed.get() → return  // noch nicht schnell genug

   i. // Boden in Reichweite?
      boolean groundNear = isGroundWithin(client.world, player, lookahead_distance.get())
      Wenn !groundNear → return

   j. // Chestplate im Inventar suchen
      int chestplateSlot = findChestplate(player)
      Wenn chestplateSlot == -1 → return  // kein Chestplate verfügbar

   k. // Swap auslösen
      Wenn client.currentScreen != null → return  // kein Inventar-Menü offen
      performLandingSwap(mc, player, chestplateSlot)
      swapTriggered = true
      chestSlotUsed = chestplateSlot  // für swap_back merken

2. isGroundWithin(world, player, maxDist):
   // Raycast von Spieler-Feet-Position gerade nach unten
   Vec3d start = player.getPos()  // Füße-Position
   Vec3d end   = start.add(0, -maxDist, 0)
   BlockHitResult hit = world.raycastBlock(start, end, player, ShapeContext.of(player))
   return hit.getType() == HitResult.Type.BLOCK

3. findChestplate(player):
   // Sucht im Hauptinventar (Slots 9–35) und Hotbar (0–8) nach einer Chestplate.
   // Elytra explizit ausschließen (die Elytra sitzt schon in Slot 6, aber Kopie
   // im Inventar wäre ungewöhnlich — trotzdem ausschließen).
   for (int i = 0; i < 36; i++) {
       ItemStack s = player.getInventory().getStack(i)
       if (isWearableChestplate(s) && !hasBindingCurse(s)) return i
   }
   return -1

   isWearableChestplate(stack):
       if (stack.isEmpty()) return false
       EquippableComponent eq = stack.get(DataComponentTypes.EQUIPPABLE)
       return eq != null
              && eq.slot() == EquipmentSlot.CHEST
              && stack.getItem() != Items.ELYTRA  // Elytra nicht als "Chestplate" werten

   hasBindingCurse(stack):
       // Identisch zu ElytraSwapModule:
       stack.getEnchantments().getEnchantments().stream()
           .anyMatch(e -> e.matchesKey(Enchantments.BINDING_CURSE))

4. performLandingSwap(mc, player, inventorySlot):
   // Ziel: Chestplate aus inventorySlot in Slot 6 (CHEST_SLOT) bringen,
   // Elytra aus Slot 6 in inventorySlot legen.
   //
   // PlayerScreenHandler-Slot-Koordinaten:
   // Inventar-Slot 0–8 (Hotbar) → PSH-Slot 36–44
   // Inventar-Slot 9–35 (Haupt) → PSH-Slot 9–35
   // Chest-Slot (Rüstung) → PSH-Slot 6
   //
   // Identischer Drei-Klick-Swap wie in ElytraSwapModule:
   int syncId = player.playerScreenHandler.syncId
   int pshSlot = (inventorySlot < 9) ? 36 + inventorySlot : inventorySlot
   boolean chestWasEmpty = player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()

   mc.interactionManager.clickSlot(syncId, pshSlot,    0, SlotActionType.PICKUP, player)
   mc.interactionManager.clickSlot(syncId, CHEST_SLOT, 0, SlotActionType.PICKUP, player)
   if (!chestWasEmpty) {
       mc.interactionManager.clickSlot(syncId, pshSlot, 0, SlotActionType.PICKUP, player)
   }

5. scheduleSwapBack(mc, player):
   // Elytra liegt jetzt in chestSlotUsed (im Inventar, da wir sie dorthin getauscht hatten).
   // Swap zurück: Elytra aus chestSlotUsed → Slot 6.
   // Wiederverwendung von performLandingSwap mit Quell-Slot:
   // Aber jetzt ist in chestSlotUsed die Elytra und in Slot 6 die Chestplate.
   // Gleiche Drei-Klick-Logik funktioniert symmetrisch.
   performLandingSwap(mc, player, chestSlotUsed)
   // Hinweis: "chestSlotUsed" enthält jetzt die Elytra — performLandingSwap
   // tauscht Slot 6 (Chestplate) ↔ chestSlotUsed (Elytra). Korrekt.
```

### Elytra-Physik (Fallgeschwindigkeit)

Fallschaden in Minecraft tritt auf wenn die Fallgeschwindigkeit beim Aufprall etwa
≥ 23 Blöcke/s (in Ticks: ≥ 1.15 blocks/tick) beträgt. Das entspricht einem Fall
aus etwa 23 Blöcken ohne Elytra. Mit Elytra-Gleiten fällt man sanfter, aber nach dem
Deaktivieren des Gleitens (z.B. Boden unter 3 Blöcke) beschleunigt man normal.

Triggergeschwindigkeit von 8 b/s (0.4 blocks/tick) gibt ~3 Ticks Vorlaufzeit bis Aufprall
bei lookahead_distance = 6m. Das reicht für die Slot-Manipulation (passiert im gleichen Tick).

Raycast-Tiefe von 6m deckt ab:
- Flacher Gleitanflug mit niedrigem Pitch (Spieler nähert sich langsam)
- Direkter Sturzflug (Spieler mit hohem negativem Pitch)

Bei hohem Gleitwinkel und hoher Geschwindigkeit kann die Vertikalkomponente plötzlich
zunehmen. Daher trigger_speed als primäre Bedingung — Boden-Nähe als Sekundärbedingung.

### Raycast-Details

`world.raycastBlock(Vec3d start, Vec3d end, BlockView, ShapeContext)` — blockiert auf
Solid-Blocks. Wasser, Licht, Glas blockieren nicht (nicht solid). Alternativ:
`world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, ...))`.

Empfohlen: `RaycastContext.ShapeType.COLLIDER` — erfasst auch Blöcke mit reduzierter
Kollisionsbox (Treppen, Zäune, etc.).

```java
RaycastContext ctx = new RaycastContext(
    start, end,
    RaycastContext.ShapeType.COLLIDER,
    RaycastContext.FluidHandling.NONE,  // Wasser ignorieren
    player
);
BlockHitResult hit = world.raycast(ctx);
return hit.getType() == HitResult.Type.BLOCK;
```

### Slot-Mapping-Referenz

```
PlayerInventory.getStack(i):
  0–8:  Hotbar (Slot 0 = Slot 36 in PSH)
  9–35: Hauptinventar
  40:   Offhand

PlayerScreenHandler clickSlot-Index:
  5:    Helm
  6:    Chestplate / Elytra  ← CHEST_SLOT = 6
  7:    Hosen
  8:    Stiefel
  9–35: Hauptinventar
  36–44: Hotbar
  45:   Offhand
```

### Edge Cases

- **Kein Chestplate im Inventar**: Kein Swap, kein Feedback (stiller Skip). Optional könnte
  eine ActionBar-Warnung erscheinen: "No chestplate to swap to!" — aber nur einmal pro
  Flug, nicht jeden Tick
- **Chestplate hat Curse of Binding**: Via `hasBindingCurse()` ausschließen — eine verfluchte
  Rüstung kann man nicht wieder ausziehen, was den Swap unbrauchbar macht
- **Elytra im Chest-Slot hat Binding Curse**: Sollte niemals vorkommen (Elytra hat kein
  Binding in Vanilla), aber zur Sicherheit prüfen bevor dem Swap ausgeführt wird
- **Spieler im Creative Mode**: `performLandingSwap` funktioniert, aber Fallschaden existiert
  nicht. Guard: `if (player.isCreative()) return` vor dem Swap
- **Spieler landet auf Wasser/Lava**: `RaycastContext.FluidHandling.NONE` ignoriert Fluide.
  Landing auf Wasser ist kein Fallschaden-Risiko — kein Swap nötig. Für Lava jedoch
  wäre ein Swap irrelevant (Schaden kommt trotzdem). Fluid-Ignorierung ist korrekt.
- **Inventar-Screen ist offen während Flug**: Guard `client.currentScreen != null` verhindert
  Slot-Manipulation bei offenem Inventar
- **Swap-Back deaktiviert per Setting**: `chestSlotUsed` bleibt auf -1; kein Re-Swap
- **Doppelter Swap** (Modul triggert zweimal weil Raycast kurz True gibt, dann False,
  dann wieder True): `swapTriggered = true` nach erstem Swap verhindert weiteres Triggern
  in derselben Glide-Session
- **Elytra bricht während Flug** (0 Durability): `ElytraSwapModule` hat Guard für Durability.
  Hier reicht es, dass `EquipmentSlot.CHEST` irgendwas enthält — Durability-Check nicht nötig
  für den Landing-Swap (wir tauschen die Chestplate rein, nicht raus)
- **Spieler stirbt im Flug**: `client.player` wird null oder `world` ungültig. Guards
  am Tick-Anfang fangen das ab
- **`swap_back` + `wasGliding`-Reset**: Falls der Spieler kurz landet, dann sofort wieder
  abhebt, wird `wasGliding` resettet und ein neuer Swap-Zyklus beginnt. `scheduleSwapBack`
  prüft `player.isOnGround()` — falls der Spieler bereits wieder springt, kein Re-Swap

## Translation Keys

```json
"noctra.elytra_landing_swap.name": "Landing Swap",
"noctra.elytra_landing_swap.description": "Automatically swaps elytra for chestplate before landing.",
"noctra.elytra_landing_swap.trigger_speed": "Trigger Speed (b/s)",
"noctra.elytra_landing_swap.lookahead_distance": "Look-ahead Distance (m)",
"noctra.elytra_landing_swap.swap_back": "Auto Swap Back"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/elytra_landing_swap.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Elytra-Silhouette oben mit Pfeil nach unten, der in eine Chestplate übergeht.
Alternativ zwei überlagerte Rüstungs-Symbole mit einem Austausch-Pfeil. Pixel-Art,
blau/weiß auf dunklem Hintergrund (ähnlich wie Elytra-Swap-Icon, aber mit Landungs-Motiv).
