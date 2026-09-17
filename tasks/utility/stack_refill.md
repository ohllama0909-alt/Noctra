# Stack Refill

**ID:** `stack_refill`  
**Category:** UTILITY  
**Status:** [x] DONE  
**Class:** `modules/impl/StackRefillModule.java`

## Description

Automatically refills the selected hotbar slot from the player's main inventory when the held stack is consumed or runs out. Triggered when the held stack transitions from a non-empty count to zero (or to an empty item) while the player is not inside a screen. Covers consumables (food, potions, arrows, throwable items) and blocks. The replacement stack must match the original item type exactly.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Description |
|---|---|---|---|---|---|
| `refill_blocks` | Boolean | `true` | — | "Refill Blocks" | Also refill block stacks (not just consumables). |
| `refill_tools` | Boolean | `false` | — | "Refill Tools" | Refill tools/weapons when they break (complement to Smart Replace). If false, only non-damageable items are refilled. |
| `show_notification` | Boolean | `false` | — | "Show Notification" | Show action-bar message when a refill occurs. |

## Implementation

### Event Hooks

- `onClientTick` — Each tick: record the current held stack (type + count). On the next tick, compare. If the stack type is the same but count dropped to zero, OR if the stack type changed to empty, attempt a refill.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
// State fields:
Item   lastItem  = Items.AIR
int    lastCount = 0

onClientTick(client):
  player = client.player
  if player == null || client.currentScreen != null → update snapshot and return

  held = player.getMainHandStack()

  needsRefill = false

  if lastItem != Items.AIR:
    // Case A: stack ran out completely (same item, count hit 0)
    if held.isEmpty() && lastItem != Items.AIR:
      needsRefill = true

    // Case B: item type changed to something different mid-use (edge case: stack fully consumed
    // and slot is immediately filled by vanilla with next stack from same slot — this shouldn't
    // happen but guard anyway)

  if needsRefill:
    refill(client, player, lastItem)

  // Update snapshot
  lastItem  = held.isEmpty() ? Items.AIR : held.getItem()
  lastCount = held.getCount()

refill(client, player, targetItem):
  if targetItem == Items.AIR → return

  // Skip non-damageable tools if refill_tools is off
  // (A "tool" here means any item with max damage > 0)
  ItemStack dummyForCheck = new ItemStack(targetItem)
  if dummyForCheck.isDamageable() && !refillTools.get() → return

  // Skip blocks if refill_blocks is off
  if targetItem is a BlockItem && !refillBlocks.get() → return

  sourceSlot = findInMainInventory(player, targetItem)
  if sourceSlot == -1 → return

  // Active hotbar slot in screen-handler coords: 36 + getSelectedSlot()
  hotbarSlot = 36 + player.getInventory().getSelectedSlot()

  syncId = player.playerScreenHandler.syncId

  // Use PICKUP swap:
  // 1. Pick up source stack
  mc.interactionManager.clickSlot(syncId, sourceSlot, 0, SlotActionType.PICKUP, player)
  // 2. Place into hotbar slot
  mc.interactionManager.clickSlot(syncId, hotbarSlot, 0, SlotActionType.PICKUP, player)
  // 3. If hotbar slot was not empty (it should be empty if lastItem ran out, but guard):
  //    place remainder back (cursor should be empty after step 2 if hotbar was truly empty)
  //    Nothing more needed.

  if showNotification.get():
    player.sendMessage(Text.translatable("noctra.stack_refill.refilled",
        Text.translatable(targetItem.getTranslationKey())), true)

findInMainInventory(player, targetItem):
  // Search main inventory slots 9–35 (inventory indices)
  // Prefer largest stack first to avoid generating many tiny leftovers
  bestSlot  = -1
  bestCount = 0
  for i in 9..35:
    stack = player.getInventory().getStack(i)
    if stack.isOf(targetItem) && stack.getCount() > bestCount:
      bestSlot  = i
      bestCount = stack.getCount()
  return bestSlot   // screen-handler slot index equals inventory index for slots 9–35
```

**Timing note:** The refill should fire on the tick *after* the stack reaches zero, not the same tick. The snapshot comparison catches the transition from non-empty to empty. This avoids false positives.

**Why not hotbar search?** The module only searches main inventory (9–35) to avoid pulling from another hotbar slot you might want to keep separate. This is the conventional behavior for stack refill mods.

### Helper Methods

```java
// Finds the main-inventory slot (screen-handler index, 9–35) containing the most
// of the target item type. Returns -1 if none found.
private int findInMainInventory(ClientPlayerEntity player, Item targetItem)

// Records current held stack into lastItem / lastCount. Called at end of every tick.
private void updateSnapshot(ClientPlayerEntity player)
```

### Edge Cases

- **Creative mode:** Skip entirely — creative inventory is infinite and clicking slots behaves differently.
- **Spectator mode:** Skip.
- **Screen open:** Update the snapshot but do not perform the refill. If the player opened inventory exactly when the stack ran out, they can manage it manually.
- **Item with NBT / components:** The refill matches by `Item` type only (not NBT). This means a stack of regular arrows refills from tipped arrows of a different type. For now, simple item-type matching is sufficient; this is the expected behavior for most use cases. A future enhancement could match by `ItemStack.areItemsAndComponentsEqual()`.
- **Durability:** If `refill_tools` is false and the item has `maxDamage > 0`, skip. If true, only search for an undamaged (or same durability class) tool — match by item type regardless of damage value.
- **Stack partially consumed:** Do NOT refill partial stacks. Only refill when the slot actually reaches empty (`held.isEmpty()` is true). Watching count changes would fire constantly while eating.
- **Double refill:** After a refill, `lastItem` and `lastCount` are updated from the new held stack on the next snapshot update. The check prevents re-triggering.
- **Offhand:** Only monitor and refill the main hand (selected hotbar slot). Offhand management is handled by Auto Totem or the player manually.

## Translation Keys

```json
"noctra.stack_refill.name": "Stack Refill",
"noctra.stack_refill.description": "Automatically refills your held stack from inventory when it runs out.",
"noctra.stack_refill.refill_blocks": "Refill Blocks",
"noctra.stack_refill.refill_tools": "Refill Tools",
"noctra.stack_refill.show_notification": "Show Notification",
"noctra.stack_refill.refilled": "Refilled: %s"
```

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/stack_refill.png`  
**Size:** 32×32 PNG  
**Suggestion:** Two overlapping item boxes (like a stack of items icon) with a small downward arrow between them indicating "pull from below." Use a warm amber/yellow color scheme to evoke inventory items.
