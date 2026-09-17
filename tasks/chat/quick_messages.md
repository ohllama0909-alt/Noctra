# Quick Messages

**ID:** `quick_messages`
**Category:** CHAT
**Status:** [x] DONE
**Class:** `modules/impl/quick_messages/QuickMessagesModule.java`
**Package:** `de.snenjih.noctra.modules.impl.quick_messages`

## System Notes (Updated)

- Module Ordner: `modules/impl/quick_messages/QuickMessagesModule.java`
- Package: `de.snenjih.noctra.modules.impl.quick_messages`
- Implementiert HudElement: Nein
- `TextSetting` für Nachrichtenvorlagen verwenden
- `KeybindSetting` für Schnelltasten

## Description

Lets the player pre-define up to 5 short chat messages and send any of them instantly via a
keybind or a client chat command. Useful for frequently-typed phrases, greetings, or server
macros. Because `ModuleSetting` has no string type, the message texts are stored in a dedicated
JSON file rather than inside `ModConfig`.

---

## Settings

No `ModuleSetting` values are needed. All per-slot configuration (the message text) lives in
the external config file. The module itself has one toggle-level setting that controls whether
keybinds are active:

| Setting ID      | Type    | Default | Range / Options | Label              | Description                                         |
|-----------------|---------|---------|-----------------|--------------------|-----------------------------------------------------|
| `keybinds_active` | Boolean | `true` | —              | "Enable Keybinds"  | When off, only `.qm <n>` commands work; keybinds are ignored. |

---

## Implementation

### External Config

**File:** `config/mandatory_quickmsg.json`
**Format:**
```json
{
  "slots": {
    "1": "Hello everyone!",
    "2": "gg wp",
    "3": "brb",
    "4": "",
    "5": ""
  }
}
```

Empty string (`""`) means the slot is unset; sending an empty slot has no effect and shows an
ERROR notification.

**Helper class:** `config/QuickMessageConfig.java`

```java
package de.snenjih.noctra.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;

public final class QuickMessageConfig {

    private static final Gson GSON  = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH  = FabricLoader.getInstance()
            .getConfigDir().resolve("mandatory_quickmsg.json");
    private static final int  SLOTS = 5;

    private final String[] messages = new String[SLOTS + 1]; // index 1..5, index 0 unused

    public QuickMessageConfig() {
        for (int i = 1; i <= SLOTS; i++) messages[i] = "";
    }

    public void load() {
        if (!Files.exists(PATH)) return;
        try (Reader r = Files.newBufferedReader(PATH)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject slots = obj.getAsJsonObject("slots");
            for (int i = 1; i <= SLOTS; i++) {
                String key = String.valueOf(i);
                if (slots.has(key)) messages[i] = slots.get(key).getAsString();
            }
        } catch (Exception e) {
            System.err.println("[Mandatory] Failed to load quick-msg config: " + e.getMessage());
        }
    }

    public void save() {
        JsonObject slots = new JsonObject();
        for (int i = 1; i <= SLOTS; i++) slots.addProperty(String.valueOf(i), messages[i]);
        JsonObject root = new JsonObject();
        root.add("slots", slots);
        try (Writer w = Files.newBufferedWriter(PATH)) {
            GSON.toJson(root, w);
        } catch (IOException e) {
            System.err.println("[Mandatory] Failed to save quick-msg config: " + e.getMessage());
        }
    }

    /** 1-based slot index. Returns empty string if unset or out of range. */
    public String get(int slot) {
        if (slot < 1 || slot > SLOTS) return "";
        return messages[slot];
    }

    /** 1-based. Trims the value. Returns false if index is out of range. */
    public boolean set(int slot, String text) {
        if (slot < 1 || slot > SLOTS) return false;
        messages[slot] = text == null ? "" : text.strip();
        save();
        return true;
    }

    public int slotCount() { return SLOTS; }
}
```

`QuickMessagesModule` holds a `QuickMessageConfig` instance loaded in `onEnable()`.

### Keybinds

Register one `KeyBinding` per slot in the module constructor using `KeybindManager.register()`.
Because `KeybindManager.register()` is static and takes a `Module` and a translation key, a
module can register multiple keybinds by calling the method multiple times and storing the
returned `KeyBinding` references.

```java
private final KeyBinding[] keybinds = new KeyBinding[6]; // index 1..5

public QuickMessagesModule() {
    super("quick_messages", "Quick Messages", "Send pre-defined messages.",
          ModuleCategory.UTILITY, Identifier.of("noctra", "modules/quick_messages"));
    addSetting(keybindsActive = new BooleanSetting("keybinds_active", "Enable Keybinds", true));
}

@Override
public void onEnable() {
    config = new QuickMessageConfig();
    config.load();
    // Register keybinds here so they appear in vanilla Controls screen.
    // GLFW_KEY_UNKNOWN = unbound by default.
    for (int i = 1; i <= 5; i++) {
        keybinds[i] = KeybindManager.register(this,
                "key.mandatory.quick_msg_" + i, GLFW.GLFW_KEY_UNKNOWN);
    }
}
```

**Note:** `KeybindManager.register()` currently maps one keybind to one module-toggle action
(`ModuleRegistry.toggle(module)`). For quick-messages the keybind must *send* a message, not
toggle the module. Two options:

**Option A (preferred):** Change `KeybindManager` to support arbitrary `Runnable` actions
instead of only toggle-actions. Add an overload:

```java
public static KeyBinding registerAction(String translationKey, int defaultGlfwKey, Runnable action) {
    KeyBinding kb = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(translationKey, defaultGlfwKey, CATEGORY));
    BINDINGS_ACTIONS.put(kb, action);
    return kb;
}
```

And in `onTick()` also drain `BINDINGS_ACTIONS`.

**Option B (simpler, no KeybindManager change):** Register keybinds directly via
`KeyBindingHelper.registerKeyBinding()` inside `onEnable()`, poll them in `onClientTick()`, and
unregister (no-op, Fabric does not support unregistration) on `onDisable()` by stopping polling
via `enabled` guard.

```java
@Override
public void onClientTick(MinecraftClient mc) {
    if (!keybindsActive.get() || mc.player == null) return;
    for (int i = 1; i <= 5; i++) {
        while (keybinds[i] != null && keybinds[i].wasPressed()) {
            sendSlot(mc, i);
        }
    }
}
```

Option B is recommended for minimal API surface change.

### Chat Commands

Extend `ChatCommandDispatcher` with a `"qm"` case:

```
.qm <n>              — send slot n (1–5)
.qm set <n> <text>   — set slot n to <text>
.qm list             — display all slots
.qm clear <n>        — clear slot n
```

```java
case "qm" -> handleQm(parts);
```

```java
private static void handleQm(String[] parts) {
    QuickMessagesModule mod = getQmModule(); // cast from registry
    if (mod == null) { push("quick_messages not registered", ERROR); return; }
    if (parts.length < 2) { push("Usage: .qm <n> | set <n> <text> | list | clear <n>", ERROR); return; }

    switch (parts[1]) {
        case "set" -> {
            if (parts.length < 4) { push("Usage: .qm set <n> <text>", ERROR); return; }
            try {
                int slot = Integer.parseInt(parts[2]);
                boolean ok = mod.getConfig().set(slot, parts[3]);
                push(ok ? "Slot " + slot + " set." : "Invalid slot (1-5).", ok ? SUCCESS : ERROR);
            } catch (NumberFormatException e) { push("Slot must be a number (1-5).", ERROR); }
        }
        case "list" -> {
            var chat = MinecraftClient.getInstance().inGameHud.getChatHud();
            chat.addMessage(Text.literal("§6[Mandatory] Quick Messages:"));
            for (int i = 1; i <= 5; i++) {
                String msg = mod.getConfig().get(i);
                String display = msg.isEmpty() ? "§8(empty)" : "§f" + msg;
                chat.addMessage(Text.literal("  §7" + i + ". " + display));
            }
        }
        case "clear" -> {
            try {
                int slot = Integer.parseInt(parts[2]);
                boolean ok = mod.getConfig().set(slot, "");
                push(ok ? "Slot " + slot + " cleared." : "Invalid slot.", ok ? SUCCESS : ERROR);
            } catch (NumberFormatException e) { push("Slot must be a number (1-5).", ERROR); }
        }
        default -> {
            // .qm <n> — send slot
            try {
                int slot = Integer.parseInt(parts[1]);
                mod.sendSlot(MinecraftClient.getInstance(), slot);
            } catch (NumberFormatException e) {
                push("Usage: .qm <n> | set <n> <text> | list | clear <n>", ERROR);
            }
        }
    }
}
```

### Core Algorithm

`sendSlot(MinecraftClient mc, int slot)`:

```
1. Guard: mc.player == null → show ERROR notification, return.
2. Guard: mc.world == null → show ERROR notification, return.
3. String msg = config.get(slot);
4. If msg.isEmpty() → show ERROR notification "Slot <n> is empty.", return.
5. mc.player.networkHandler.sendChatMessage(msg);
   // This sends the message to the server as a regular chat message.
   // Prepending "/" is intentional if the user wants a server command.
```

`sendChatMessage(String)` is the correct 1.21.11 API for sending player chat. Do not use
`ClientPlayNetworkHandler.sendCommand()` — that strips the leading `/` from commands, which
is wrong here since the message could be plain text or a command at the user's choice.

If `msg` starts with `/` the server will interpret it as a command; otherwise as chat. This is
the expected behaviour and requires no special handling.

### onEnable / onDisable

```java
@Override
public void onEnable() {
    config = new QuickMessageConfig();
    config.load();
    // Register keybinds (Option B: direct registration)
    for (int i = 1; i <= 5; i++) {
        keybinds[i] = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.mandatory.quick_msg_" + i,
                               GLFW.GLFW_KEY_UNKNOWN, KB_CATEGORY));
    }
}

@Override
public void onDisable() {
    // Keybinds remain registered (Fabric does not support unregistration).
    // onClientTick is only called when enabled, so they simply stop firing.
    config = null;
}
```

### Edge Cases

- `mc.player == null` — guard at top of `sendSlot()` and `onClientTick()`.
- Message longer than 256 characters — Minecraft's chat API will reject it server-side;
  trim to 256 chars in `sendSlot()` and warn via notification.
- User types a message via `.qm <n>` while in a chat screen — `onClientTick` is not reached
  from the GUI path, but the `.qm` command is handled by `ChatCommandDispatcher` (already
  intercepted before the message reaches the server), so this path works.
- Keybind pressed while a screen is open — `wasPressed()` still fires; add guard
  `if (mc.currentScreen != null) return;` in `onClientTick` to avoid chat-opening conflicts.
- Config file missing on first launch — `QuickMessageConfig` initialises all slots to `""`
  without loading; a save will create the file the first time `.qm set` is used.

---

## Translation Keys

```json
"noctra.module.quick_messages.name": "Quick Messages",
"noctra.module.quick_messages.description": "Send pre-defined chat messages via keybind or command.",
"noctra.quick_messages.setting.keybinds_active": "Enable Keybinds",
"key.mandatory.quick_msg_1": "Quick Message 1",
"key.mandatory.quick_msg_2": "Quick Message 2",
"key.mandatory.quick_msg_3": "Quick Message 3",
"key.mandatory.quick_msg_4": "Quick Message 4",
"key.mandatory.quick_msg_5": "Quick Message 5"
```

---

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/quick_messages.png`
**Size:** 32x32 PNG
**Suggestion:** A speech bubble containing the number "1" or a small lightning bolt, conveying
"instant send". Colour: light blue or white on a dark background.
