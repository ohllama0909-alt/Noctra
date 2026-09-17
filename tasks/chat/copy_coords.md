# Copy Coords

**ID:** `copy_coords`
**Category:** CHAT
**Status:** [x] DONE
**Class:** `modules/impl/copy_coords/CopyCoordsModule.java`
**Package:** `de.snenjih.noctra.modules.impl.copy_coords`

## System Notes (Updated)

- Module Ordner: `modules/impl/copy_coords/CopyCoordsModule.java`
- Package: `de.snenjih.noctra.modules.impl.copy_coords`
- Implementiert HudElement: Nein
- `KeybindSetting` für Copy-Keybind verwenden

## Description

Copies the player's current coordinates to the system clipboard and optionally sends them as a
chat message visible to other players. The output format is configurable. When the player is in
the Overworld the corresponding Nether coordinates (÷8) can be appended automatically, and vice
versa for the Nether (×8). Triggered via the client-only chat command `.coords` (consumed by
`ChatCommandDispatcher` before reaching the server).

---

## Settings

| Setting ID        | Type    | Default | Range / Options            | Label                     | Description                                                    |
|-------------------|---------|---------|----------------------------|---------------------------|----------------------------------------------------------------|
| `post_to_chat`    | Boolean | `false` | —                          | "Post to Chat"            | Also send the coordinates as a public chat message to the server. |
| `show_nether`     | Boolean | `true`  | —                          | "Show Nether Coords"      | Append the corresponding Nether/Overworld coordinate conversion. |
| `format`          | Enum    | `FULL`  | `FULL`, `COMPACT`, `ARROW` | "Format"                  | Controls the coordinate string layout (see Format Enum below). |

### Format Enum

Define a nested or top-level enum `CoordsFormat`:

```java
public enum CoordsFormat {
    FULL,    // "X: 128  Y: 64  Z: -512"
    COMPACT, // "128, 64, -512"
    ARROW    // "128 → 64 → -512"
}
```

Store as `EnumSetting<CoordsFormat>("format", "Format", CoordsFormat.FULL, CoordsFormat.class)`.

---

## Implementation

### Wire-up in NoctraMod

No new event listeners are required beyond what already exists. The feature works entirely
through `ChatCommandDispatcher`. Add a `"coords"` case:

```java
case "coords" -> handleCoords();
```

This is triggered when the player types `.coords` in chat (the `.` prefix is the dispatcher's
prefix — see `ChatCommandDispatcher.PREFIX`).

### Required Mixins

None.

### External Config

None. All settings are `ModuleSetting` values persisted by the existing `ModConfig` system.

### Chat Commands (in ChatCommandDispatcher)

```
.coords         — copy and optionally post coordinates
```

No sub-commands are needed for this module; the settings control behaviour.

Extend `ChatCommandDispatcher.handleHelp()` to include:

```java
chatHud.addMessage(Text.literal("  §f.coords              §8copy current position"));
```

### Core Algorithm

`execute(MinecraftClient mc)` — called from `ChatCommandDispatcher.handleCoords()`:

```
1. Guard: mc.player == null || mc.world == null → show ERROR notification, return.

2. BlockPos pos = mc.player.getBlockPos();
   int x = pos.getX(), y = pos.getY(), z = pos.getZ();

3. Dimension detection:
   boolean inNether = mc.world.getRegistryKey() == World.NETHER;

4. Build primary coordinate string:
   String coords = formatCoords(x, y, z, module.formatSetting.get());

5. Build conversion string if showNether.get():
   if (inNether):
       int ox = x * 8, oz = z * 8;  // Nether → Overworld
       String conv = "Overworld: " + formatCoords(ox, y, oz, formatSetting.get());
   else:
       int nx = x / 8, nz = z / 8;  // Overworld → Nether
       String conv = "Nether: " + formatCoords(nx, y, nz, formatSetting.get());
   (Y is passed through unchanged; Nether Y mapping is non-trivial and omitted for MVP.)

6. String full = coords + (showNether.get() ? "  [" + conv + "]" : "");

7. Clipboard:
   mc.keyboard.setClipboard(full);

8. Local chat feedback (always, regardless of post_to_chat):
   mc.inGameHud.getChatHud().addMessage(
       Text.literal("§7[Mandatory] §fCoords copied: §e" + full));

9. If postToChat.get():
   mc.player.networkHandler.sendChatMessage(full);
   // This sends to the server as a public message. The string must be <= 256 chars.
```

`mc.keyboard.setClipboard(String)` is the correct 1.21.11 API for writing to the system
clipboard. It is available on `MinecraftClient.keyboard` which is a `Keyboard` instance.

### formatCoords helper

```java
private String formatCoords(int x, int y, int z, CoordsFormat fmt) {
    return switch (fmt) {
        case FULL    -> "X: " + x + "  Y: " + y + "  Z: " + z;
        case COMPACT -> x + ", " + y + ", " + z;
        case ARROW   -> x + " → " + y + " → " + z;
    };
}
```

### Module as a passive container

`CopyCoordsModule` does not register any Fabric events inside `onEnable`/`onDisable`. It is
purely a settings container and an execution target called from `ChatCommandDispatcher`. The
module must still be registered in `ModuleRegistry` so that:

1. It appears in the carousel and can be toggled on/off.
2. Its settings are persisted by `ModConfig`.
3. `ChatCommandDispatcher.handleCoords()` can guard with `if (!mod.isEnabled())`.

When the module is disabled, `.coords` does nothing (shows an INFO notification: "Enable
Copy Coords first.").

### Registering in NoctraMod

```java
// In NoctraMod.onInitializeClient():
registry.register(new CopyCoordsModule());
```

### ChatCommandDispatcher integration

```java
// In ChatCommandDispatcher.handle(), add to switch:
case "coords" -> handleCoords();
```

```java
private static void handleCoords() {
    Optional<Module> opt = ModuleRegistry.getInstance().getById("copy_coords");
    if (opt.isEmpty() || !(opt.get() instanceof CopyCoordsModule mod)) {
        NotificationManager.push("copy_coords not registered", Type.ERROR);
        return;
    }
    if (!mod.isEnabled()) {
        NotificationManager.push("Enable Copy Coords first.", Type.INFO);
        return;
    }
    mod.execute(MinecraftClient.getInstance());
}
```

### Edge Cases

- `mc.player == null` — can occur if the command is somehow triggered in the main menu;
  guard immediately.
- `mc.world == null` — same guard; also covers the loading screen.
- World registry key is neither `World.OVERWORLD` nor `World.NETHER` (e.g. `World.END`) —
  `showNether` conversion is meaningless; still show `x/8` for compactness but label it
  "Nether?" or simply skip the conversion when not in OVERWORLD or NETHER.
- Resulting string longer than 256 characters — trim before `sendChatMessage()`; 256 chars
  is generous for a coordinate string.
- `mc.keyboard` may be null during very early init — not a realistic concern since this is
  triggered interactively by the player, but a null-check is harmless.
- Nether Y conversion — the Nether has a build height of Y 0–128. The Overworld Y has no
  direct mapping; pass it through unchanged (standard community convention).
- `postToChat` while in a dimension that forbids chat (some servers) — the networkHandler call
  will simply fail server-side; no special client handling needed.
- `BlockPos` integer overflow — not a concern for vanilla Minecraft world coordinates
  (capped at ±30,000,000).

---

## Translation Keys

```json
"noctra.module.copy_coords.name": "Copy Coords",
"noctra.module.copy_coords.description": "Copy your position to the clipboard with .coords.",
"noctra.copy_coords.setting.post_to_chat": "Post to Chat",
"noctra.copy_coords.setting.show_nether": "Show Nether Coords",
"noctra.copy_coords.setting.format": "Format"
```

---

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/copy_coords.png`
**Size:** 32x32 PNG
**Suggestion:** A crosshair or map pin over a simplified coordinate grid (X/Z axes), with a
small clipboard icon in the corner. Colour: light green or cyan to suggest "location captured".
