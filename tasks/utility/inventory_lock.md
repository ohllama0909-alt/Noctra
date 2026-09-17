# Inventory Lock

**ID:** `inventory_lock`  
**Category:** UTILITY  
**Status:** [x] DONE  
**Class:** `modules/impl/InventoryLockModule.java`

## Description

Ermöglicht es, einzelne Inventar-Slots zu sperren, sodass Items in diesen Slots nicht verschoben, gedroppt oder durch Klick-Aktionen verändert werden können. Nützlich zum Schutz von wichtigen Slots (z. B. Totem in Offhand, Nahrung auf fester Hotbar-Position). Slots werden durch Shift+Rechtsklick im Inventar-Screen getoggelt und visuell mit einem Schloss-Symbol markiert.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `locked_slots` | — | `{}` | — | — | Interne persistierte Menge gesperrter Slot-Nummern (kein direktes Setting, wird via ModConfig gespeichert) |
| `show_lock_icon` | Boolean | `true` | — | "Show Lock Icon" | Schloss-Symbol auf gesperrten Slots anzeigen |

*Hinweis: `locked_slots` ist kein `ModuleSetting`, sondern wird separat als `Set<Integer>` in `ModConfig` als `"inventory_lock_slots"` gespeichert (kommagetrennte Integer-Liste im JSON).*

## Implementation

### Event Hooks

- `onEnable()` — Gesperrte Slot-Liste aus `ModConfig` laden.
- `onDisable()` — Gesperrte Slot-Liste in `ModConfig` persistieren; Overlay-Mixin deaktiviert sich automatisch via `isEnabled()`-Check.

### Required Mixins

**Mixin 1: Klick-Blockierung**
- **Class:** `mixin/InventoryLockClickMixin.java`
- **Target:** `net.minecraft.client.network.ClientPlayerInteractionManager`
- **Injection:** `@Inject(at = @At("HEAD"), method = "clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V", cancellable = true)`
- **Zweck:** Wenn das Modul aktiv ist und `slotId` in der gesperrten Menge enthalten ist, `ci.cancel()` aufrufen. Dies verhindert jeden Slot-Klick (PICKUP, QUICK_MOVE, THROW, SWAP, etc.).

**Mixin 2: Drop-Blockierung**
- **Class:** `mixin/InventoryLockDropMixin.java`
- **Target:** `net.minecraft.client.network.ClientPlayerInteractionManager`
- **Injection:** `@Inject(at = @At("HEAD"), method = "dropItem(Z)Z", cancellable = true)`
- **Zweck:** Wenn das Modul aktiv ist und der aktuell aktive Hotbar-Slot (`36 + player.getInventory().getSelectedSlot()`) gesperrt ist, Drop verhindern.

**Mixin 3: Visuelles Overlay**
- **Class:** `mixin/InventoryLockOverlayMixin.java`
- **Target:** `net.minecraft.client.gui.screen.ingame.InventoryScreen` (oder `HandledScreen`)
- **Injection:** `@Inject(at = @At("TAIL"), method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V")`
- **Zweck:** Nach dem Rendern jedes Slots: Wenn `showLockIcon` aktiv, Slot als gesperrt markiert und die Slot-Position im `PlayerScreenHandler` gesperrt ist, ein semi-transparentes rotes Overlay + Schloss-Symbol zeichnen.

**Mixin 4: Toggle per Shift+Rechtsklick**
- **Class:** `mixin/InventoryLockToggleMixin.java`
- **Target:** `net.minecraft.client.gui.screen.ingame.HandledScreen`
- **Injection:** `@Inject(at = @At("HEAD"), method = "mouseClicked(Lnet/minecraft/client/gui/screen/Screen$Click;Z)Z", cancellable = true)`
- **Zweck:** Wenn Modul aktiv, Shift gehalten (`Screen$Click` hat keinen Shift-Flag → `hasShiftDown()` nutzen), Rechtsklick (`click.button() == 1`) und Cursor auf einem Slot: Slot-ID in gesperrte Menge einfügen/entfernen und persistieren. `ci.cancel()` um Vanilla-Shift+Rechtsklick zu verhindern.

### Core Algorithm

```
Felder in der Klasse:
    private Set<Integer> lockedSlots = new HashSet<>();

loadLockedSlots():
    String raw = ModConfig.getInstance().getString("inventory_lock_slots", "")
    for (String s : raw.split(",")) if (!s.isBlank()) lockedSlots.add(Integer.parseInt(s.trim()))

saveLockedSlots():
    String csv = lockedSlots.stream().sorted().map(String::valueOf).collect(Collectors.joining(","))
    ModConfig.getInstance().setString("inventory_lock_slots", csv)
    ModConfig.getInstance().save()

isSlotLocked(int slotId):
    return isEnabled() && lockedSlots.contains(slotId)

toggleSlot(int slotId):
    if (lockedSlots.contains(slotId)) lockedSlots.remove(slotId)
    else lockedSlots.add(slotId)
    saveLockedSlots()

// Im InventoryLockOverlayMixin:
// Slot-Index im PlayerScreenHandler bestimmen:
//   slot.getIndex() gibt die Handler-interne Slot-Nummer
// Overlay zeichnen:
//   ctx.fill(x, y, x+16, y+16, 0x88FF0000)  // semi-transparentes Rot
//   ctx.drawGuiTexture(..., lockSpriteId, x, y, 16, 16)  // Schloss-Sprite
```

### Edge Cases

- `clickSlot` wird auch für Hotbar-Swaps (SlotActionType.SWAP) aufgerufen: Diese werden ebenfalls gecancelt, wenn der Ziel-Slot gesperrt ist.
- Spieler öffnet eine andere Kiste/Anbaublock: `HandledScreen` ist dann kein `InventoryScreen` mehr. Das Overlay-Mixin muss auf `HandledScreen` target sein (nicht nur `InventoryScreen`). Slot-Indices im Chest-Handler unterscheiden sich — Slots 0–N der Kiste vs. Spieler-Slots. Nur Player-Slots (ab Index des ersten Player-Inventory-Slots) sperren. **Guard:** Im Mixin prüfen ob `handler instanceof PlayerScreenHandler || handler instanceof GenericContainerScreenHandler` usw. — oder einfacher: Nur den `PlayerScreenHandler` des Spielers referenzieren.
- Falsche Slot-Indices bei Crafting/Anvil: Unterschiedliche `ScreenHandler`-Typen haben unterschiedliche Slot-Layouts. Sicher ist es, nur Klicks auf Slots zu blockieren, die direkt dem `PlayerScreenHandler` entsprechen (syncId == player.playerScreenHandler.syncId).
- Modul deaktiviert: Alle Slots sofort entsperrt (kein Klick-Cancel); die `lockedSlots`-Daten bleiben im Speicher und werden beim nächsten Enable wiederhergestellt.
- `ModConfig.getString` nicht verfügbar: Falls `ModConfig` nur `Map<String, Boolean>` unterstützt, alternative Speicherung als separate JSON-Datei (`mandatory_locks.json`) via Gson analog zu `DeathCoordinatesModule`.
- Creative Mode: Spieler kann Items frei bewegen, Mixin cancelt trotzdem — erwünschtes Verhalten.

## Translation Keys

```json
"noctra.inventory_lock.name": "Inventory Lock",
"noctra.inventory_lock.description": "Locks inventory slots to prevent accidental item movement or dropping.",
"noctra.inventory_lock.show_lock_icon": "Show Lock Icon"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/inventory_lock.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Geschlossenes Vorhängeschloss, zentriert. Farbe: Gelb/Gold auf dunklem Hintergrund.
