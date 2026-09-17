# Middle Click Pick

**ID:** `middle_click_pick`  
**Category:** UTILITY  
**Status:** [x] DONE  
**Class:** `modules/impl/MiddleClickPickModule.java`

## Description

Allows players to pick up a block by middle-clicking on it in Survival mode, mimicking the Creative mode "pick block" behavior. When the targeted block exists in the player's inventory (hotbar or main inventory), the corresponding hotbar slot is switched to (if it's on the hotbar) or the block is moved from the main inventory to the active hotbar slot. Unlike Creative mode, no item is created out of thin air — the player must already have the block in their inventory.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Description |
|---|---|---|---|---|---|
| `search_inventory` | Boolean | `true` | — | "Search Inventory" | If the block is not on the hotbar, search the main inventory (9–35) and move it to the active slot. If false, only switch between hotbar slots. |
| `prefer_full_stack` | Boolean | `true` | — | "Prefer Full Stack" | When multiple stacks of the block exist, pick the slot with the largest count first. |
| `switch_to_existing` | Boolean | `true` | — | "Switch Hotbar Slot" | If the block is already on the hotbar, switch to that slot instead of moving from inventory. |

## Implementation

### Event Hooks

- `onClientTick` — Not used directly. The pick-block action is event-driven (middle-click).

**Primary hook:** A Mixin on `Mouse.onMouseButton` or on the `Screen` mouse event is needed because the middle-click action needs to be intercepted at the input level in Survival mode. Alternatively, intercept at `MinecraftClient.handleBlockBreaking` or use `ClientPlayNetworking` — but neither is clean.

The cleanest approach: Mixin into `MinecraftClient.doItemPick()`, which is the method called when Minecraft processes the middle-click pick-block in Creative. In Survival, Minecraft calls this method but skips the item-give logic. We inject here to add our Survival behavior.

### Required Mixins

- **Class:** `mixin/MiddleClickPickMixin.java`
- **Target:** `net.minecraft.client.MinecraftClient`
- **Method + Injection Point:**
  ```java
  @Inject(at = @At("HEAD"), method = "doItemPick", cancellable = true)
  private void onDoItemPick(CallbackInfo ci)
  ```
- **Purpose:** Intercept the pick-block action in Survival mode. When the player is not in Creative and middle-clicks a block, execute the inventory search and slot switch/move, then cancel the vanilla call to prevent it from trying to give creative items.

**`doItemPick` behavior in vanilla:** This method is `private` in `MinecraftClient`. The Mixin must target it by name (obfuscated in production, but Yarn mappings provide the deobfuscated name `doItemPick`). Check Yarn 1.21.11+build.6 for the exact method signature.

**Alternative without private method Mixin:** Use `KeyBinding.isPressed()` on `client.options.pickItemKey` inside `onClientTick` and handle it there. This is less precise (fires every tick the key is held, not once per click) but requires no Mixin into private methods. A `wasPressed` debounce flag prevents multi-fire.

**Recommended approach:** Use the `onClientTick` + key-press approach to avoid fragile private-method Mixins:

```java
// In onClientTick:
if client.options.pickItemKey.wasPressed():
  handleMiddleClick(client, player)
```

`KeyBinding.wasPressed()` consumes one press event per call (like a queue drain), so it naturally fires once per click even if held.

### Core Algorithm

```
onClientTick(client):
  player = client.player
  if player == null || client.currentScreen != null → return
  if player.isCreative() → return   // creative handles pick natively; don't interfere
  if player.isSpectator() → return

  // KeyBinding.wasPressed() drains one queued press per call
  if !client.options.pickItemKey.wasPressed() → return

  handleMiddleClick(client, player)

handleMiddleClick(client, player):
  // Get the block the player is looking at
  hit = client.crosshairTarget
  if hit == null || hit.getType() != HitResult.Type.BLOCK → return

  blockPos   = ((BlockHitResult) hit).getBlockPos()
  blockState = client.world.getBlockState(blockPos)
  
  if blockState.isAir() → return

  // Get the pick stack (what item represents this block in the inventory)
  // Use Block.asItem() to get the item form of the block
  pickItem = blockState.getBlock().asItem()
  if pickItem == Items.AIR → return   // block has no item form (e.g. fire, water)

  currentHotbar = player.getInventory().getSelectedSlot()

  // 1. Check if the item is already in the current hotbar slot
  currentStack = player.getInventory().getStack(currentHotbar)
  if currentStack.isOf(pickItem) → return   // already holding it, nothing to do

  // 2. Search hotbar (0–8)
  if switchToExisting.get():
    hotbarSlot = findInRange(player, pickItem, 0, 8, preferFullStack.get())
    if hotbarSlot != -1:
      switchHotbarTo(client, player, hotbarSlot)
      return

  // 3. Search main inventory if enabled
  if searchInventory.get():
    invSlot = findInRange(player, pickItem, 9, 35, preferFullStack.get())
    if invSlot != -1:
      moveToHotbar(client, player, invSlot, currentHotbar)
      return

  // 4. Not found — optionally notify
  // (silent fail; vanilla creative would create the block, we cannot)

findInRange(player, targetItem, fromInclusive, toInclusive, preferFull):
  bestSlot  = -1
  bestCount = -1
  for i in fromInclusive..toInclusive:
    stack = player.getInventory().getStack(i)
    if !stack.isOf(targetItem) → continue
    if preferFull:
      if stack.getCount() > bestCount:
        bestCount = stack.getCount()
        bestSlot  = i
    else:
      return i   // first found
  return bestSlot

moveToHotbar(client, player, inventorySlot, hotbarIndex):
  // inventorySlot is an inventory index 9–35; screen-handler slot is the same index.
  // hotbarIndex is 0–8; screen-handler slot is 36 + hotbarIndex.
  syncId      = player.playerScreenHandler.syncId
  screenSrc   = inventorySlot           // same as inventory index for main inventory
  screenDst   = 36 + hotbarIndex

  // Check if hotbar slot has something — if so, we need the three-click swap
  hotbarStack = player.getInventory().getStack(hotbarIndex)
  
  mc.interactionManager.clickSlot(syncId, screenSrc, 0, SlotActionType.PICKUP, player)
  mc.interactionManager.clickSlot(syncId, screenDst, 0, SlotActionType.PICKUP, player)
  if !hotbarStack.isEmpty():
    mc.interactionManager.clickSlot(syncId, screenSrc, 0, SlotActionType.PICKUP, player)

switchHotbarTo(client, player, hotbarIndex):
  client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarIndex))
  ((AccessorPlayerInventory) player.getInventory()).setSelectedSlot(hotbarIndex)
  // Or packet-only approach if accessor Mixin is not implemented
```

**`Block.asItem()` vs `BlockItem`:** `blockState.getBlock().asItem()` returns the `Item` that represents this block, or `Items.AIR` if the block has no item form. This is the correct 1.21.11 API.

**Pick with block entity data:** When a block has a block entity (e.g. a chest, furnace), vanilla Creative pick includes the NBT of the block entity. This module does NOT do that — it simply matches by item type. Block entity data matching is out of scope.

### Helper Methods

```java
// Searches inventory indices [from, to] for the target item. Returns inventory index or -1.
private int findInRange(ClientPlayerEntity player, Item targetItem,
                        int fromInclusive, int toInclusive, boolean preferFull)

// Moves an item from a main-inventory slot to the given hotbar slot via clickSlot.
private void moveToHotbar(MinecraftClient client, ClientPlayerEntity player,
                          int inventorySlot, int hotbarIndex)

// Switches hotbar selection to the given hotbar index (0–8).
private void switchHotbarTo(MinecraftClient client, ClientPlayerEntity player, int hotbarIndex)
```

### Edge Cases

- **Creative mode:** Let vanilla handle it. `if player.isCreative() → return` at the start. Vanilla Creative pick always works.
- **Spectator mode:** Skip — spectators have no inventory.
- **Block has no item form (water, lava, fire, end portal frame):** `block.asItem()` returns `Items.AIR`. Return early.
- **Block is air (clicked on empty space):** `blockState.isAir()` guards this.
- **Block not in inventory:** Silent fail (no notification). This matches Creative behavior where a new item appears — in Survival, nothing happens, which is intuitive enough.
- **Hotbar full and `search_inventory` is on:** The `moveToHotbar` call performs a three-click swap, replacing the currently held item in that slot. The displaced item goes to the source inventory slot. This is intentional — the player asked to pick a specific block, so the current slot contents are displaced. The `prefer_full_stack` option helps choose which stack to pull, minimizing splits.
- **Multiple stacks of the same block:** `prefer_full_stack` picks the largest. When false, the lowest slot index wins (hotbar before main inventory).
- **`wasPressed()` on key binding:** `KeyBinding.wasPressed()` is the idiomatic way to check key presses in Fabric mods. It dequeues one press per call. If called every tick, it fires at most once per physical click regardless of frame rate. Do not use `isPressed()` (which is true for the entire duration the key is held).
- **Middle click while in a GUI:** `client.currentScreen != null` blocks this. In GUIs the middle-click has vanilla meaning (clone item stack in Creative / no-op in Survival).
- **Pick on mob (ENTITY hit result):** `hit.getType() != HitResult.Type.BLOCK` guard covers this.

## Translation Keys

```json
"noctra.middle_click_pick.name": "Middle Click Pick",
"noctra.middle_click_pick.description": "Pick up targeted blocks from your inventory by middle-clicking, like in Creative mode.",
"noctra.middle_click_pick.search_inventory": "Search Inventory",
"noctra.middle_click_pick.prefer_full_stack": "Prefer Full Stack",
"noctra.middle_click_pick.switch_to_existing": "Switch Hotbar Slot"
```

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/middle_click_pick.png`  
**Size:** 32×32 PNG  
**Suggestion:** A mouse with the scroll wheel / middle button highlighted in a bright color (white or yellow glow), with a small block icon (stone or grass) appearing above the cursor to indicate "pick." Clean, recognizable silhouette on a dark background.
