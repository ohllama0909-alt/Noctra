# Tool Selector

**ID:** `tool_selector`  
**Category:** UTILITY  
**Status:** [x] DONE  
**Class:** `modules/impl/ToolSelectorModule.java`

## Description

Automatically switches the active hotbar slot to the most effective tool for the block the player is targeting. Evaluated each tick based on the block the crosshair is aimed at. When the player stops targeting a mineable block (or targets air/fluid), optionally restores the previously held slot. The selection is based on mining speed (`getMiningSpeedMultiplier`), so the fastest tool in the hotbar wins.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Description |
|---|---|---|---|---|---|
| `auto_restore` | Boolean | `true` | — | "Restore Slot" | Restore the original hotbar slot when the player stops looking at a mineable block. |
| `require_hold` | Boolean | `false` | — | "Only While Mining" | Only switch tools when the player is actively holding the mine key (left mouse button down), not just on crosshair target. |
| `min_speed_gain` | Float | `1.5` | 1.0–10.0 | "Min Speed Gain" | Only switch if the best tool is at least this much faster than the current tool. Prevents switching for marginal gains (e.g. 1.001× speed from a wrong-level pickaxe). |

## Implementation

### Event Hooks

- `onClientTick` — Each tick: check crosshair target, evaluate tools, switch hotbar slot if a better tool is found.

### Required Mixins

Kein Mixin erforderlich. Block state and crosshair target are accessible via `client.crosshairTarget` and `client.world.getBlockState(pos)`. Hotbar slot switching uses `UpdateSelectedSlotC2SPacket` + accessor (same as Auto Eat, see below).

**Shared accessor:** If the `PlayerInventoryAccessor` Mixin is implemented for Auto Eat, reuse it here. If not, use the packet-only approach.

### Core Algorithm

```
// State fields:
int  originalSlot     = -1   // hotbar index before any auto-switch (-1 = not saved)
int  lastSwitchedSlot = -1   // the slot we switched to (to detect player manual override)
boolean wasTargetingBlock = false

onClientTick(client):
  player = client.player
  world  = client.world
  if player == null || world == null || client.currentScreen != null → maybeRestore(); return
  if player.isCreative() || player.isSpectator() → maybeRestore(); return

  // Get crosshair target
  hit = client.crosshairTarget
  if hit == null || hit.getType() != HitResult.Type.BLOCK:
    // Not targeting a block
    if wasTargetingBlock:
      maybeRestore(client, player)
      wasTargetingBlock = false
    return

  blockPos   = ((BlockHitResult) hit).getBlockPos()
  blockState = world.getBlockState(blockPos)

  if blockState.isAir() || !blockState.isToolRequired() && blockState.getHardness(world, blockPos) < 0:
    // Unbreakable or air
    if wasTargetingBlock: maybeRestore(client, player)
    wasTargetingBlock = false
    return

  // Check require_hold: if enabled, only act when mining key is held
  if requireHold.get() && !client.options.attackKey.isPressed():
    // Not holding attack — restore if we had switched
    if wasTargetingBlock && lastSwitchedSlot != -1: maybeRestore(client, player)
    return

  wasTargetingBlock = true

  // Detect manual slot change: if player changed slot away from our switch, forget saved slot
  currentSlot = player.getInventory().getSelectedSlot()
  if lastSwitchedSlot != -1 && currentSlot != lastSwitchedSlot:
    // Player manually switched — cancel our tracking
    originalSlot     = -1
    lastSwitchedSlot = -1

  // Find best tool in hotbar for this block
  bestSlot  = findBestTool(player, world, blockState, blockPos)
  if bestSlot == -1 → return

  if bestSlot == currentSlot → return  // already using best tool

  // Compute speed of current tool vs best tool
  currentSpeed = getSpeed(player.getInventory().getStack(currentSlot), blockState)
  bestSpeed    = getSpeed(player.getInventory().getStack(bestSlot), blockState)

  if bestSpeed < currentSpeed * minSpeedGain.get() → return  // gain too small

  // Save original slot if not yet saved
  if originalSlot == -1:
    originalSlot = currentSlot

  // Switch to best tool
  switchHotbarTo(client, player, bestSlot)
  lastSwitchedSlot = bestSlot

maybeRestore(client, player):
  if !autoRestore.get() || originalSlot == -1 → clear state; return
  switchHotbarTo(client, player, originalSlot)
  originalSlot     = -1
  lastSwitchedSlot = -1

findBestTool(player, world, blockState, blockPos):
  bestSlot  = -1
  bestSpeed = 1.0f   // default = no tool (bare hand speed)

  for i in 0..8:
    stack = player.getInventory().getStack(i)
    if stack.isEmpty() → continue
    speed = getSpeed(stack, blockState)
    if speed > bestSpeed:
      bestSpeed = speed
      bestSlot  = i

  // Only return a slot if it's genuinely faster than bare hand
  // (bestSlot == -1 means no tool in hotbar is faster than bare hand)
  return bestSlot

getSpeed(stack, blockState):
  // getMiningSpeedMultiplier is available on ItemStack in MC 1.21.11
  return stack.getMiningSpeedMultiplier(blockState)
  // Returns 1.0f for non-tools, or the tool-specific multiplier (e.g. 8.0f for iron pickaxe on stone)

switchHotbarTo(client, player, slotIndex):
  // Send packet to server
  client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slotIndex))
  // Update client-side (accessor mixin or direct field access)
  ((AccessorPlayerInventory) player.getInventory()).setSelectedSlot(slotIndex)
```

**`getMiningSpeedMultiplier` API note:** In Fabric/Yarn for 1.21.11, `ItemStack.getMiningSpeedMultiplier(BlockState state)` returns the speed multiplier the item provides for that block state. Returns `1.0f` for items that have no specific speed boost. This is the cleanest single-call speed check.

### Helper Methods

```java
// Returns hotbar slot index (0–8) of the fastest tool for blockState, or -1 if bare hand is best.
private int findBestTool(ClientPlayerEntity player, ClientWorld world, BlockState blockState, BlockPos blockPos)

// Returns the mining speed of a stack against the given block state.
private float getSpeed(ItemStack stack, BlockState blockState)

// Switches the hotbar selection to the given index (0–8).
private void switchHotbarTo(MinecraftClient client, ClientPlayerEntity player, int slot)

// Restores the original slot if auto_restore is enabled and an original slot was saved.
private void maybeRestore(MinecraftClient client, ClientPlayerEntity player)
```

### Edge Cases

- **No tool in hotbar is effective:** `findBestTool` returns -1 and no switch occurs. The player continues with whatever they have.
- **Multiple equally-fast tools:** The first one found (lowest hotbar index) wins. This is stable and predictable.
- **Silk Touch vs Fortune:** The module does NOT consider enchantments — it selects purely on speed. If the player wants a specific enchant, they should switch manually. This is intentional (complexity vs. usefulness tradeoff).
- **Wrong tool type (e.g. wrong pickaxe tier):** `getMiningSpeedMultiplier` returns the actual speed accounting for tier, so a wooden pickaxe on obsidian will correctly return 1.0f (no effective speed boost) and not be selected.
- **Player opens inventory mid-switch:** `client.currentScreen != null` blocks the tick, and `maybeRestore` is called to undo any pending switch.
- **Silk Touch blocks:** The module does not check `isToolRequired()` for drops — it only maximizes speed. This is by design.
- **Auto Restore disabled:** `originalSlot` never gets used; the player must manually switch back after breaking the block.
- **Manual override detection:** If the player switches hotbar slot while the module had auto-switched, `lastSwitchedSlot != currentSlot` is true and the module clears its saved state, respecting the manual choice.
- **Unbreakable blocks (hardness -1):** Detected via `getHardness() < 0` and skipped.
- **Fluid targets:** `HitResult.Type.BLOCK` only fires for solid blocks, so fluids are naturally excluded.

## Translation Keys

```json
"noctra.tool_selector.name": "Tool Selector",
"noctra.tool_selector.description": "Automatically switches to the best tool in your hotbar for the targeted block.",
"noctra.tool_selector.auto_restore": "Restore Slot",
"noctra.tool_selector.require_hold": "Only While Mining",
"noctra.tool_selector.min_speed_gain": "Min Speed Gain"
```

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/tool_selector.png`  
**Size:** 32×32 PNG  
**Suggestion:** A pickaxe and an axe crossed diagonally with a small cursor/crosshair overlay on the block they're pointing at. Steel-gray and wooden tones. Evokes "right tool, right block."
