# Anti AFK

**ID:** `anti_afk`  
**Category:** UTILITY  
**Status:** [x] DONE  
**Class:** `modules/impl/AntiAfkModule.java`

## Description

Prevents AFK kicks from servers by performing minimal, configurable actions at a regular interval. Actions are intentionally subtle to avoid causing unintended side effects: the player quietly rotates a few degrees or jumps briefly. No items are used, no blocks are broken. The interval and action type are configurable. The module is entirely client-side and does not send false packets beyond what the chosen action naturally generates.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Description |
|---|---|---|---|---|---|
| `interval` | Int | `60` | 10–600 | "Interval (s)" | How many seconds between each anti-AFK action. Most servers kick after 5 minutes (300 s); 60 s is a safe default. |
| `action` | Enum | `ROTATE` | `ROTATE`, `JUMP`, `SNEAK`, `SWING` | "Action" | What the module does each interval: `ROTATE` = slight yaw rotation; `JUMP` = single jump; `SNEAK` = brief sneak toggle; `SWING` = arm swing (no attack). |
| `random_offset` | Boolean | `true` | — | "Random Offset" | Add a random ±10 % jitter to the interval to appear less bot-like. |
| `show_indicator` | Boolean | `false` | — | "Show Indicator" | Display a subtle action-bar countdown (e.g. "AFK: 45s") so the player knows the module is active. |

## Implementation

### Event Hooks

- `onClientTick` — Counts ticks. When the interval elapses, performs the selected action and resets the counter.
- `onRenderHud` — If `show_indicator` is enabled, renders the countdown to the action bar.

### Required Mixins

Kein Mixin erforderlich. All actions are achievable through the public client API:
- Yaw rotation: directly set `player.setYaw(...)` (or `player.bodyYaw`)
- Jump: `player.jump()` (public method on `LivingEntity`)
- Sneak: `player.setSneaking(true)` then `player.setSneaking(false)` next tick
- Swing: `player.swingHand(Hand.MAIN_HAND)` — plays the animation and sends a packet

### Core Algorithm

```
// State fields:
int     tickCounter   = 0
int     intervalTicks = 0     // computed from interval setting + jitter
boolean sneakToggle   = false // tracks sneak toggle state
float   baseYaw       = 0f

onEnable():
  recomputeInterval()

onDisable():
  // If we were mid-sneak, release it
  client = MinecraftClient.getInstance()
  if client.player != null && sneakToggle:
    client.player.setSneaking(false)
    sneakToggle = false

onClientTick(client):
  player = client.player
  if player == null → return

  // Show indicator
  if showIndicator.get():
    secondsLeft = (intervalTicks - tickCounter) / 20
    player.sendMessage(Text.literal("AFK: " + secondsLeft + "s"), true)

  tickCounter++
  if tickCounter < intervalTicks → return

  // Time to act
  tickCounter = 0
  recomputeInterval()   // also re-rolls jitter for next cycle
  performAction(client, player)

recomputeInterval():
  base = interval.get() * 20   // seconds → ticks
  if randomOffset.get():
    jitter = (int)(base * 0.1f * (Math.random() * 2 - 1))  // ±10%
    intervalTicks = Math.max(10 * 20, base + jitter)
  else:
    intervalTicks = base

performAction(client, player):
  switch action.get():

    case ROTATE:
      // Rotate yaw by a small amount and then immediately rotate back next action
      // Alternate between +5 and -5 degrees
      float delta = (baseYaw == 0f) ? 5.0f : -5.0f
      player.setYaw(player.getYaw() + delta)
      baseYaw = (baseYaw == 0f) ? 5.0f : 0f

    case JUMP:
      // Only jump if on ground (avoid double-jump issues)
      if player.isOnGround():
        player.jump()

    case SNEAK:
      // Toggle sneak on; schedule toggle-off next tick via a pending flag
      player.setSneaking(true)
      sneakToggle = true
      // Will be released on the next tick via the sneak-release check below

    case SWING:
      player.swingHand(Hand.MAIN_HAND)

// Sneak release logic — add to the TOP of onClientTick, before counter:
  if sneakToggle:
    player.setSneaking(false)
    sneakToggle = false
```

**`player.setYaw(float)`:** In Yarn 1.21.11, `Entity.setYaw(float)` is public. This directly sets the look direction and sends the corresponding movement packet automatically as part of the normal tick.

**HUD indicator note:** Sending an action-bar message every tick via `sendMessage(..., true)` is the vanilla way to maintain an action-bar display. It must be called every tick or the display fades. Only call it when `showIndicator` is enabled.

### Helper Methods

```java
// Recomputes intervalTicks from the current interval setting (and optional jitter).
private void recomputeInterval()

// Executes the currently configured action.
private void performAction(MinecraftClient client, ClientPlayerEntity player)
```

### Edge Cases

- **Player is not on ground (JUMP action):** Only jump when `player.isOnGround()` to avoid triggering Elytra or water swimming behavior.
- **Player is in water / lava:** ROTATE and SWING are safe in any condition. JUMP works in water (it becomes a swim stroke). SNEAK underwater is fine.
- **Player is riding an entity:** `player.hasVehicle()` — for JUMP, jump the vehicle instead (`player.getVehicle().jump()` if it is a `JumpingMount`). For ROTATE, rotate the vehicle. For SWING and SNEAK, proceed normally; they don't affect the mount.
- **Server blocks the action (anti-cheat):** Only ROTATE is completely invisible. SWING creates a packet that some strict servers log. JUMP creates movement packets. These are all normal player actions; the module does not perform anything that vanilla Minecraft wouldn't produce.
- **Player starts walking or interacting during the interval:** The counter keeps running. If the player is actively playing, anti-AFK actions fire anyway but are lost in the noise of normal player movement. This is acceptable.
- **Indicator spam:** The action-bar message is updated every tick when enabled. This suppresses all other action-bar messages (e.g. from other modules or the server) while active. Warn the player of this conflict in the description if needed. Consider only showing the indicator once per second (every 20 ticks) by rate-limiting to avoid overlay issues.
- **`onDisable` cleanup:** Release sneak if it was held. Reset `tickCounter` so the next enable starts fresh.
- **Very short intervals (10 s):** The minimum is 10 seconds (10 × 20 = 200 ticks) to avoid flooding the server with packets.

## Translation Keys

```json
"noctra.anti_afk.name": "Anti AFK",
"noctra.anti_afk.description": "Performs small actions at regular intervals to prevent AFK kicks.",
"noctra.anti_afk.interval": "Interval (s)",
"noctra.anti_afk.action": "Action",
"noctra.anti_afk.random_offset": "Random Offset",
"noctra.anti_afk.show_indicator": "Show Indicator",
"noctra.anti_afk.action.rotate": "Rotate",
"noctra.anti_afk.action.jump": "Jump",
"noctra.anti_afk.action.sneak": "Sneak",
"noctra.anti_afk.action.swing": "Swing"
```

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/anti_afk.png`  
**Size:** 32×32 PNG  
**Suggestion:** A simple clock face with a player silhouette or a walking figure inside it. Use a muted blue/gray palette. The clock emphasizes "timed interval"; the figure emphasizes "staying active." Alternatively: an hourglass with legs.
