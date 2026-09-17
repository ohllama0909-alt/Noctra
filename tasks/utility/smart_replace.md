# Smart Replace

**ID:** `smart_replace`  
**Category:** UTILITY  
**Status:** [x] DONE  
**Class:** `modules/impl/SmartReplaceModule.java`

## Description

Automatically replaces a broken tool or weapon in the active hotbar slot with the next available tool of the same type from the hotbar or main inventory. Triggered the tick after the held item stack becomes empty (indicating the item broke). Searches for the closest matching tool: same item type first, then same tool family (e.g. any pickaxe), with the best durability remaining.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Description |
|---|---|---|---|---|---|
| `search_inventory` | Boolean | `true` | — | "Search Inventory" | Also search main inventory (slots 9–35) if no replacement found on the hotbar. |
| `match_mode` | Enum | `SAME_ITEM` | `SAME_ITEM`, `SAME_FAMILY` | "Match Mode" | `SAME_ITEM` = only replace with the exact same item type. `SAME_FAMILY` = replace with any tool of the same family (any pickaxe for a pickaxe, any axe for an axe, etc.). |
| `prefer_best_durability` | Boolean | `true` | — | "Prefer Best Durability" | When multiple replacements exist, pick the one with the most durability remaining. When false, pick the first found (lowest slot index). |
| `show_notification` | Boolean | `true` | — | "Show Notification" | Show action-bar message when a replacement is made or when no replacement is found. |

## Implementation

### Event Hooks

- `onClientTick` — Each tick: track the held item. When it transitions from a damageable item at max damage (or an item that just became empty), trigger the replacement search.

### Required Mixins

Kein Mixin erforderlich.

**Note:** The item-break event is observable by watching the held stack go empty. In Minecraft, when a tool breaks its durability reaches `maxDamage`, the server destroys it and the client receives an update — the slot becomes an empty `ItemStack`. We detect this by watching `lastItem` being damageable and the new stack being empty.

### Core Algorithm

```
// State fields:
Item lastItem       = Items.AIR
int  lastDamage     = 0
int  lastMaxDamage  = 0

onClientTick(client):
  player = client.player
  if player == null || client.currentScreen != null:
    updateSnapshot(player)
    return
  if player.isCreative() || player.isSpectator():
    updateSnapshot(player)
    return

  held = player.getMainHandStack()

  // Detect tool break: previous item was damageable AND held is now empty
  toolBroke = lastItem != Items.AIR
           && lastMaxDamage > 0          // was a damageable item
           && held.isEmpty()             // slot is now empty

  if toolBroke:
    handleBreak(client, player, lastItem)

  updateSnapshot(player)

handleBreak(client, player, brokenItem):
  replacement = findReplacement(player, brokenItem)
  if replacement == -1:
    if showNotification.get():
      player.sendMessage(Text.translatable("noctra.smart_replace.no_replacement",
          Text.translatable(brokenItem.getTranslationKey())), true)
    return

  currentHotbar = player.getInventory().getSelectedSlot()
  currentSlot   = 36 + currentHotbar   // screen-handler slot

  if replacement is a hotbar slot (0–8):
    // The replacement is on the hotbar — switch selection to it
    switchHotbarTo(client, player, replacement)
  else:
    // The replacement is in main inventory — move it to the broken tool's hotbar slot
    screenInvSlot = replacement   // slots 9–35 are same in screen handler
    syncId = player.playerScreenHandler.syncId
    mc.interactionManager.clickSlot(syncId, screenInvSlot, 0, SlotActionType.PICKUP, player)
    mc.interactionManager.clickSlot(syncId, currentSlot,   0, SlotActionType.PICKUP, player)
    // If the hotbar slot had something (it shouldn't — item broke), put leftover back:
    // After break, the hotbar slot is empty, so cursor should also be empty after step 2.

  if showNotification.get():
    // Find new item name after swap — look at what's in hand now
    player.sendMessage(Text.translatable("noctra.smart_replace.replaced",
        Text.translatable(brokenItem.getTranslationKey())), true)

findReplacement(player, brokenItem):
  inventory = player.getInventory()
  
  // Build search order
  hotbarRange    = 0..8          // inventory indices (getStack(i))
  mainRange      = 9..35

  // Combined search list in priority order
  searchSlots = hotbarRange (excluding currentHotbar) + (searchInventory ? mainRange : [])
  
  bestSlot        = -1
  bestDurability  = -1

  for i in searchSlots:
    stack = inventory.getStack(i)
    if stack.isEmpty() → continue
    if stack.isDamageable() == false → continue   // skip non-damageable

    if !isMatch(stack, brokenItem) → continue

    // Calculate remaining durability
    remaining = stack.getMaxDamage() - stack.getDamage()
    
    if preferBestDurability.get():
      if remaining > bestDurability:
        bestDurability = remaining
        bestSlot       = i
    else:
      // First match wins
      return i (converted to screen-handler slot if needed)

  // Convert inventory index to screen-handler slot for return:
  // Hotbar 0–8 → screen-handler 36+i  (but we return inventory index 0–8 for hotbar,
  //   and the caller checks range to decide switch vs move)
  return bestSlot   // caller checks: if 0–8 → hotbar switch; if 9–35 → inventory move

isMatch(stack, brokenItem):
  if matchMode == SAME_ITEM:
    return stack.isOf(brokenItem)
  else: // SAME_FAMILY
    return isSameFamily(stack.getItem(), brokenItem)

isSameFamily(candidate, original):
  // Tool family detection based on instanceof / item class
  // Families: PickaxeItem, AxeItem, ShovelItem, HoeItem, SwordItem, MaceItem
  // Use Item class hierarchy:
  return (candidate instanceof PickaxeItem && original instanceof PickaxeItem)
      || (candidate instanceof AxeItem     && original instanceof AxeItem)
      || (candidate instanceof ShovelItem  && original instanceof ShovelItem)
      || (candidate instanceof HoeItem     && original instanceof HoeItem)
      || (candidate instanceof SwordItem   && original instanceof SwordItem)
      // Bows, crossbows, tridents: match exact item only in SAME_FAMILY too (they have no sub-classes)
      || candidate.getClass() == original.getClass()

updateSnapshot(player):
  if player == null || player.getMainHandStack().isEmpty():
    lastItem      = Items.AIR
    lastDamage    = 0
    lastMaxDamage = 0
  else:
    stack         = player.getMainHandStack()
    lastItem      = stack.getItem()
    lastDamage    = stack.getDamage()
    lastMaxDamage = stack.getMaxDamage()
```

### Helper Methods

```java
// Returns inventory index of replacement (0–8 = hotbar, 9–35 = main inventory), or -1.
private int findReplacement(ClientPlayerEntity player, Item brokenItem)

// True if the candidate stack matches the broken item under the current match_mode.
private boolean isMatch(ItemStack candidate, Item brokenItem)

// True if the two items are in the same tool family (SAME_FAMILY mode).
private boolean isSameFamily(Item candidate, Item original)

// Switches hotbar selection to slot 0–8.
private void switchHotbarTo(MinecraftClient client, ClientPlayerEntity player, int slot)

// Updates lastItem / lastDamage / lastMaxDamage from the current hand.
private void updateSnapshot(ClientPlayerEntity player)
```

### Edge Cases

- **Tool breaks while screen is open:** The snapshot update still runs (needed for next tick), but the replacement action is skipped. Next tick with no screen will re-check — but by then the snapshot shows an empty hand and `lastItem` has already been reset. Therefore: snapshot must be updated even when the screen is open, but the break-detection should only trigger the replacement if no screen is open. Consider a one-tick grace: set a `pendingReplacement` flag when break is detected with screen open, and execute it the next tick when the screen closes.
- **Two tools break in the same tick:** Extremely unlikely but handled implicitly — each tick only checks one transition.
- **No replacement found:** Notify via action bar if `show_notification` is on. Do nothing else.
- **Replacement tool also has low durability:** No special handling — any durability > 0 is valid. `prefer_best_durability` ensures we pick the healthiest available.
- **Creative mode:** Skip entirely. Items don't break in creative.
- **Spectator mode:** Skip.
- **Non-tool item that "breaks" (e.g. a flint and steel at 0 durability):** Correctly handled — `flintAndSteel instanceof PickaxeItem` is false, so `isSameFamily` returns false unless `SAME_ITEM` matches exact type.
- **Unbreakable tools (maxDamage = 0 due to Unbreaking?):** Items with the `Unbreakable` NBT tag never reach 0 durability in practice; they won't trigger the break detection because `held` won't go empty.
- **Tridents:** `TridentItem` is not a subclass of any of the standard tool classes. `isSameFamily` falls back to `candidate.getClass() == original.getClass()`, which correctly matches trident for trident.

## Translation Keys

```json
"noctra.smart_replace.name": "Smart Replace",
"noctra.smart_replace.description": "Automatically replaces a broken tool with the next best from your inventory.",
"noctra.smart_replace.search_inventory": "Search Inventory",
"noctra.smart_replace.match_mode": "Match Mode",
"noctra.smart_replace.prefer_best_durability": "Prefer Best Durability",
"noctra.smart_replace.show_notification": "Show Notification",
"noctra.smart_replace.replaced": "Replaced broken %s",
"noctra.smart_replace.no_replacement": "No replacement found for %s",
"noctra.smart_replace.match_mode.same_item": "Same Item",
"noctra.smart_replace.match_mode.same_family": "Same Family"
```

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/smart_replace.png`  
**Size:** 32×32 PNG  
**Suggestion:** A cracked/broken pickaxe on the left with an arrow pointing right to an intact pickaxe. Use a red tint on the broken tool and a green or white tint on the replacement. Evokes "broken → replaced."
