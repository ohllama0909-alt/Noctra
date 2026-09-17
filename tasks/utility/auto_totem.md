# Auto Totem

**ID:** `auto_totem`  
**Category:** UTILITY  
**Status:** [x] DONE  
**Class:** `modules/impl/AutoTotemModule.java`

## Description

Automatically moves a Totem of Undying from the player's inventory into the offhand slot whenever the offhand is empty or does not hold a totem. The swap happens silently in the background every tick, so the player always has a totem ready without manual intervention. Useful on hardcore or PvP servers where forgetting a totem is fatal.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Description |
|---|---|---|---|---|---|
| `prefer_hotbar` | Boolean | `false` | — | "Prefer Hotbar" | When true, searches the hotbar (slots 36–44) first before searching the main inventory (slots 9–35). When false, searches main inventory first. |
| `show_warning` | Boolean | `true` | — | "Show Warning" | Displays an action-bar message when no totem is found and the offhand is empty. |

## Implementation

### Event Hooks

- `onClientTick` — Each tick, check if the offhand holds a totem. If not, search inventory for one and execute the slot-swap. Also emit warning if enabled and no totem exists.

### Required Mixins

Kein Mixin erforderlich. The swap uses `interactionManager.clickSlot()` which is available on the client without a Mixin.

### Core Algorithm

```
onClientTick(client):
  player = client.player
  if player == null → return
  if client.currentScreen != null → return   // never swap while a screen is open
  if player.isCreative() → return            // creative has no inventory management need

  offhand = player.getInventory().offHand.get(0)  // slot index 45 in screen handler
  if offhand is a TotemOfUndying → return         // already has totem, nothing to do

  totemSlot = findTotem(player, preferHotbar.get())
  if totemSlot == -1:
    if showWarning.get() && offhand.isEmpty():
      player.sendMessage(Text.translatable("noctra.auto_totem.no_totem"), true)
    return

  // Perform the swap: move totem from inventory to offhand (slot 45)
  syncId = player.playerScreenHandler.syncId
  // Use SWAP action (button = offhand swap key, which is action type SWAP with button 40)
  mc.interactionManager.clickSlot(syncId, totemSlot, 40, SlotActionType.SWAP, player)
  // Button 40 = offhand swap (F key equivalent in screen handler)

findTotem(player, preferHotbar):
  inventory = player.getInventory()
  
  if preferHotbar:
    // Search hotbar slots 0–8 (screen-handler slots 36–44)
    for i in 0..8:
      if inventory.getStack(i) is TotemOfUndying → return 36 + i
    // Then main inventory slots 9–35
    for i in 9..35:
      if inventory.getStack(i) is TotemOfUndying → return i
  else:
    // Search main inventory slots 9–35 first
    for i in 9..35:
      if inventory.getStack(i) is TotemOfUndying → return i
    // Then hotbar
    for i in 0..8:
      if inventory.getStack(i) is TotemOfUndying → return 36 + i

  return -1   // not found
```

**Slot mapping note:** `PlayerInventory.getStack(i)` uses inventory indices (0–8 = hotbar, 9–35 = main). The screen-handler slot numbers differ: hotbar slot `i` → screen-handler slot `36 + i`; main inventory slot `i` (9–35) → same screen-handler slot index `i`. This matters for `clickSlot()`.

**SWAP with button 40:** `SlotActionType.SWAP` with button `40` is the vanilla mechanism for the F-key offhand swap. It moves the item in the given slot directly to the offhand without needing a cursor pickup sequence. No need for a three-click PICKUP chain.

### Helper Methods

```java
// Returns the screen-handler slot index of the first totem found, or -1.
private int findTotem(ClientPlayerEntity player, boolean preferHotbar)

// Returns true if the given stack is a Totem of Undying.
private static boolean isTotem(ItemStack stack)
// Implementation: stack.isOf(Items.TOTEM_OF_UNDYING)
```

### Edge Cases

- **Screen open:** Skip the tick entirely when `client.currentScreen != null` to avoid interfering with the player manually managing inventory.
- **Creative mode:** Skip — creative players neither have a normal inventory nor need totems.
- **Offhand already occupied (non-totem):** Do NOT replace whatever is in the offhand (e.g. shield). Only act when offhand is empty. If the offhand holds something other than a totem and is not empty, skip silently.
- **No totem in inventory:** Optionally show action-bar warning (controlled by `show_warning`). Do not spam — the warning fires every tick while the condition persists. To avoid spam, track a `lastWarnedAt` tick counter and only warn every 40 ticks (2 seconds).
- **Stack of totems:** `SlotActionType.SWAP` moves the entire stack; in practice totems do not stack, so this is always 1 item.
- **Spectator mode:** Skip — `player.isSpectator()` check before acting.

## Translation Keys

```json
"noctra.auto_totem.name": "Auto Totem",
"noctra.auto_totem.description": "Keeps a Totem of Undying in your offhand automatically.",
"noctra.auto_totem.prefer_hotbar": "Prefer Hotbar",
"noctra.auto_totem.show_warning": "Show Warning",
"noctra.auto_totem.no_totem": "Auto Totem: No totem in inventory!"
```

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/auto_totem.png`  
**Size:** 32×32 PNG  
**Suggestion:** A golden totem-of-undying face (the vanilla item texture, simplified) centered on a dark background with a small shield or heart overlay in the bottom-right corner to indicate "protection."
