# Auto Eat

**ID:** `auto_eat`  
**Category:** UTILITY  
**Status:** [x] DONE  
**Class:** `modules/impl/AutoEatModule.java`

## Description

Automatically eats food when the player's hunger level drops below a configurable threshold. The module searches the hotbar and optionally the main inventory for the best available food item, moves it to the selected hotbar slot if needed, and simulates holding the right-mouse button to eat. Handles both instant-eat items and normal food (which requires holding use). Takes saturation value into account when `prefer_best` is enabled.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Description |
|---|---|---|---|---|---|
| `hunger_threshold` | Int | `16` | 1–20 | "Eat Below" | Start eating when hunger (food level) is at or below this value. 20 = always eat, 1 = only eat when nearly starving. |
| `prefer_best` | Boolean | `true` | — | "Prefer Best Food" | When true, pick the food with the highest saturation value from available stacks. When false, use the first food found. |
| `search_inventory` | Boolean | `false` | — | "Search Inventory" | Also search main inventory (slots 9–35) if no food is found on the hotbar. |
| `eat_golden_apple` | Boolean | `true` | — | "Eat Golden Apples" | Include golden apples and enchanted golden apples in the food search. |

## Implementation

### Event Hooks

- `onClientTick` — Check hunger, find food, move to hand if necessary, and simulate eating by calling `client.options.useKey.setPressed(true)` on the tick food needs to be used and releasing it once the eat animation completes.

### Required Mixins

Kein Mixin erforderlich. The module uses `client.options.useKey` (the use-item key binding) to simulate holding right-click. Minecraft checks this binding each tick internally and calls `interactItem` when it is pressed.

**Alternative approach (more reliable):** Use `interactionManager.interactItem(player, Hand.MAIN_HAND)` directly inside `onClientTick`. This is a client-to-server packet call that triggers the eating process. Hold this call every tick until the player is done eating (`player.isUsingItem()` becomes false after completion).

The simulated-keypress approach is simpler and more vanilla-like.

### Core Algorithm

```
// State fields:
boolean eating       = false
Item    foodMovedFrom = null  // item we temporarily moved to hotbar
int     originalSlot = -1    // original hotbar slot index before food move
int     eatTicksLeft = 0

onClientTick(client):
  player = client.player
  if player == null || client.currentScreen != null:
    stopEating(client)
    return

  if player.isCreative() || player.isSpectator() → return

  // If currently eating, check if done
  if eating:
    if !player.isUsingItem():
      // Eating finished (or was interrupted)
      eating = false
      eatTicksLeft = 0
      // Restore original slot if we moved food
      // (food was consumed in place; nothing to restore since it was eaten)
      // If food not consumed (player moved away), swap back:
      // Actually: after eating completes the held item count just decremented; no restoration needed.
    else:
      // Still eating — keep simulating the held key
      simulateUseKey(client)
    return

  // Check if we should start eating
  foodLevel = player.getHungerManager().getFoodLevel()
  if foodLevel > hungerThreshold.get() → return

  // Also don't eat if already at full hunger
  if foodLevel >= 20 → return

  // Don't eat while flying with elytra (can interrupt glide)
  if player.isGliding() → return

  // Find best food
  foodSlot = findFood(player)  // returns hotbar index 0–8, or inventory index 9–35
  if foodSlot == -1 → return

  // If food is not in the current hotbar slot, move it there or switch hotbar slots
  currentHotbar = player.getInventory().getSelectedSlot()

  if foodSlot is a hotbar index (0–8):
    if foodSlot != currentHotbar:
      player.getInventory().selectedSlot = foodSlot  // switch hotbar selection
      // Note: selectedSlot is private → use packet or reflection.
      // Better: use ClientPlayerInteractionManager trick:
      // player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(foodSlot))
      // AND set local: player.getInventory().selectedSlot via reflection or accessor Mixin
      //
      // ALTERNATIVE APPROACH: Move food to current slot via clickSlot SWAP, eat, then swap back.
      // Simplest: just switch hotbar selection via the slot-select packet.
      switchHotbarTo(client, player, foodSlot)
  else:
    // Food is in main inventory — move it to current hotbar slot via PICKUP swap
    moveToHotbar(client, player, foodSlot, currentHotbar)
    foodMovedFrom = player.getInventory().getStack(foodSlot).getItem()

  // Start eating
  eating = true
  simulateUseKey(client)

simulateUseKey(client):
  // Trigger interactItem directly — cleaner than faking the key binding
  if client.player != null && !client.player.isUsingItem():
    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND)

switchHotbarTo(client, player, slotIndex):
  // Send slot-select packet to server and update client-side
  client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slotIndex))
  player.getInventory().selectedSlot = slotIndex
  // selectedSlot IS private in PlayerInventory — need accessor mixin OR:
  // Use: ((AccessorPlayerInventory) player.getInventory()).setSelectedSlot(slotIndex)
  // See: Required Mixins section for accessor.

moveToHotbar(client, player, inventorySlot, hotbarIndex):
  syncId      = player.playerScreenHandler.syncId
  screenSlot  = inventorySlot  // main inventory slots 9–35 are same index in screen handler
  hotbarSlot  = 36 + hotbarIndex
  mc.interactionManager.clickSlot(syncId, screenSlot, 0, SlotActionType.PICKUP, player)
  mc.interactionManager.clickSlot(syncId, hotbarSlot, 0, SlotActionType.PICKUP, player)
  // If hotbar was not empty, put remainder back
  held = player.getStackInHand(Hand.MAIN_HAND)
  if !held.isEmpty():
    mc.interactionManager.clickSlot(syncId, screenSlot, 0, SlotActionType.PICKUP, player)

findFood(player):
  // Collect candidate slots; hotbar preferred (0–8), then main if search_inventory is on
  // Score each food item by saturation if prefer_best, else return first found
  bestSlot  = -1
  bestScore = -1

  searchRange = (searchInventory.get()) ? 0..35 : 0..8

  for i in searchRange:
    stack = player.getInventory().getStack(i)
    if stack.isEmpty() → continue
    if !isValidFood(stack) → continue

    score = prefer_best ? getSaturation(stack) : 1.0f
    if score > bestScore:
      bestScore = score
      bestSlot  = i

  return bestSlot

isValidFood(stack):
  // Must have FoodComponent
  FoodComponent food = stack.get(DataComponentTypes.FOOD)
  if food == null → return false

  // Respect eat_golden_apple setting
  if (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)):
    return eatGoldenApple.get()

  // Chorus fruit: may teleport player — include anyway (player chose to enable the module)
  return true

getSaturation(stack):
  FoodComponent food = stack.get(DataComponentTypes.FOOD)
  return food != null ? food.saturation() : 0f
```

### Required Mixins

One accessor Mixin is needed to write `PlayerInventory.selectedSlot` (it is private):

- **Class:** `mixin/accessor/PlayerInventoryAccessor.java`
- **Target:** `net.minecraft.entity.player.PlayerInventory`
- **Type:** `@Mixin` + `@Accessor`
- **Purpose:** Expose a setter for the `selectedSlot` field so the module can switch hotbar slots client-side without a Mixin injection.

```java
@Mixin(PlayerInventory.class)
public interface PlayerInventoryAccessor {
    @Accessor("selectedSlot")
    void setSelectedSlot(int slot);
}
```

Register in `noctra.mixins.json` under `"client"` array.

**Alternative without accessor:** Send `UpdateSelectedSlotC2SPacket` only (no local update). The server will update the selection and the client receives the confirmation. The local held-item display lags by one tick but eating still works correctly because the server drives `interactItem`.

### Helper Methods

```java
private int findFood(ClientPlayerEntity player)
private boolean isValidFood(ItemStack stack)
private float getSaturation(ItemStack stack)
private void switchHotbarTo(MinecraftClient client, ClientPlayerEntity player, int slot)
private void moveToHotbar(MinecraftClient client, ClientPlayerEntity player, int inventorySlot, int hotbarIndex)
private void stopEating(MinecraftClient client)
```

### Edge Cases

- **Player takes damage while eating:** `player.isUsingItem()` becomes false when interrupted. The `eating` flag resets. Next tick re-evaluates hunger threshold.
- **Food runs out mid-eat:** The count drops to 0; next evaluation call to `findFood` finds a new stack or returns -1 and stops.
- **Healing with golden apple:** Respect `eat_golden_apple`. Golden apples give absorption/regeneration regardless of hunger; eating them when hunger is full is wasteful. The hunger threshold still gates them.
- **Chorus fruit teleport:** The module does not exclude chorus fruit. Since this is automation the player opted into, unexpected teleports are accepted behavior. A future setting could exclude it.
- **Sprint eating (Minecraft prevents sprinting while eating):** Minecraft handles this automatically on the server side; no special handling needed.
- **Full saturation bar but hunger < 20:** Still eat if food level is at or below threshold; saturation is hidden from the food level value. Don't second-guess this.
- **Inventory screen open:** `client.currentScreen != null` — skip the tick. Player is managing inventory manually.
- **Already using an item (e.g. bow):** `player.isUsingItem()` is true but it's a bow. Guard: only proceed if `player.getActiveItem()` has a `FoodComponent`. Otherwise, do not interrupt.

## Translation Keys

```json
"noctra.auto_eat.name": "Auto Eat",
"noctra.auto_eat.description": "Automatically eats food when your hunger drops below the threshold.",
"noctra.auto_eat.hunger_threshold": "Eat Below",
"noctra.auto_eat.prefer_best": "Prefer Best Food",
"noctra.auto_eat.search_inventory": "Search Inventory",
"noctra.auto_eat.eat_golden_apple": "Eat Golden Apples"
```

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/auto_eat.png`  
**Size:** 32×32 PNG  
**Suggestion:** A drumstick (cooked chicken leg) icon with a small hunger-bar indicator below it or a small sparkle/arrow indicating automation. Use warm orange/brown tones matching the vanilla food icons.
