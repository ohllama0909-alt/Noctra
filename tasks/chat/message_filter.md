# Message Filter

**ID:** `message_filter`
**Category:** CHAT
**Status:** [x] DONE
**Class:** `modules/impl/message_filter/MessageFilterModule.java`
**Package:** `de.snenjih.noctra.modules.impl.message_filter`

## System Notes (Updated)

- Module Ordner: `modules/impl/message_filter/MessageFilterModule.java`
- Package: `de.snenjih.noctra.modules.impl.message_filter`
- Implementiert HudElement: Nein
- `TextSetting` für Filter-Pattern verwenden

## Description

Silently drops incoming chat and game messages that match any entry in a user-defined filter
list. Each entry can be treated as either a plain substring match or a full Java regex pattern,
controlled by a per-module toggle. A running counter of suppressed messages is optionally shown
in the action bar. Two client commands allow managing the list at runtime without opening any
screen: `.filter add <pattern>` and `.filter list`.

---

## Settings

| Setting ID     | Type    | Default | Range / Options | Label                   | Description                                            |
|----------------|---------|---------|-----------------|-------------------------|--------------------------------------------------------|
| `use_regex`    | Boolean | `false` | —               | "Use Regex"             | Treat patterns as Java regular expressions.            |
| `show_counter` | Boolean | `true`  | —               | "Show Filter Count"     | Display suppressed-message count in the action bar.    |

The filter patterns themselves are **not** stored as `ModuleSetting` values because `ModuleSetting`
has no string list type. They are persisted in a separate JSON file (see External Config).

---

## Implementation

### Wire-up in NoctraMod

The existing outgoing-chat handler already calls `ChatCommandDispatcher.handle()`. The new
`.filter` sub-command must be registered there (or in a dedicated handler called from
`ChatCommandDispatcher`). See "Chat Commands" section below.

For incoming message suppression, add `ALLOW_CHAT` and `ALLOW_GAME` listeners in
`NoctraMod.registerEvents()` that delegate to the module (similar structure to the
`mention_highlight` receive-event wiring described in `mention_highlight.md`):

```java
ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, ts) -> {
    for (Module m : registry.getAll()) {
        if (!m.isEnabled()) continue;
        ActionResult r = m.onReceiveChat(message);  // reuse ActionResult return
        if (r != ActionResult.PASS) return false;   // suppress
    }
    return true;
});

ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
    if (overlay) return true;
    for (Module m : registry.getAll()) {
        if (!m.isEnabled()) continue;
        ActionResult r = m.onReceiveChat(message);
        if (r != ActionResult.PASS) return false;
    }
    return true;
});
```

This requires changing `Module.onReceiveChat` signature from `void` to `ActionResult`.
Current signature: `default void onReceiveChat(Text message) {}`
New signature: `default ActionResult onReceiveChat(Text message) { return ActionResult.PASS; }`

All existing modules that override `onReceiveChat` (currently none) must be updated. The
`MentionHighlightModule` must return `ActionResult.PASS` even when it re-injects a modified
message (it only observes, it suppresses the original and re-adds — see that spec for details).

### Required Mixins

None. Fabric's `ClientReceiveMessageEvents.ALLOW_CHAT` and `ALLOW_GAME` are sufficient to
cancel message display.

### External Config

Filter patterns are stored in a separate flat JSON file:

**File:** `config/mandatory_filters.json`
**Format:**
```json
{
  "patterns": [
    "spam keyword",
    ".*advertisement.*"
  ]
}
```

**Helper class:** `config/FilterConfig.java`

```java
package de.snenjih.noctra.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class FilterConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("mandatory_filters.json");
    private static final int MAX_PATTERNS = 8;

    private final List<String> patterns = new ArrayList<>();

    public void load() {
        if (!Files.exists(PATH)) return;
        try (Reader r = Files.newBufferedReader(PATH)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            JsonArray arr = obj.getAsJsonArray("patterns");
            patterns.clear();
            for (JsonElement el : arr) patterns.add(el.getAsString());
        } catch (Exception e) {
            System.err.println("[Mandatory] Failed to load filter config: " + e.getMessage());
        }
    }

    public void save() {
        JsonArray arr = new JsonArray();
        patterns.forEach(arr::add);
        JsonObject obj = new JsonObject();
        obj.add("patterns", arr);
        try (Writer w = Files.newBufferedWriter(PATH)) {
            GSON.toJson(obj, w);
        } catch (IOException e) {
            System.err.println("[Mandatory] Failed to save filter config: " + e.getMessage());
        }
    }

    /** Returns false if the list is full (8 entries). */
    public boolean add(String pattern) {
        if (patterns.size() >= MAX_PATTERNS) return false;
        if (!patterns.contains(pattern)) {
            patterns.add(pattern);
            save();
        }
        return true;
    }

    public boolean remove(int index) {
        if (index < 0 || index >= patterns.size()) return false;
        patterns.remove(index);
        save();
        return true;
    }

    public List<String> getPatterns() {
        return Collections.unmodifiableList(patterns);
    }
}
```

`MessageFilterModule` holds a `FilterConfig` instance (loaded in `onEnable`, kept alive so the
counter survives disables/enables within one game session).

### Chat Commands

Add a `"filter"` case to `ChatCommandDispatcher.handle()`:

```
.filter add <pattern>   — add a pattern (max 8)
.filter remove <n>      — remove by 1-based index
.filter list            — print all patterns with indices
.filter clear           — remove all patterns
```

Example extension inside `ChatCommandDispatcher`:

```java
case "filter" -> handleFilter(parts);
```

```java
private static void handleFilter(String[] parts) {
    MessageFilterModule mod = getFilterModule(); // obtain from registry
    if (mod == null) { push("message_filter not registered", ERROR); return; }
    if (parts.length < 2) { push("Usage: .filter <add|remove|list|clear>", ERROR); return; }
    switch (parts[1]) {
        case "add" -> {
            if (parts.length < 3 || parts[2].isBlank()) { push("Usage: .filter add <pattern>", ERROR); return; }
            boolean ok = mod.getFilterConfig().add(parts[2]);
            push(ok ? "Filter added: " + parts[2] : "Filter list full (max 8)", ok ? SUCCESS : ERROR);
        }
        case "remove" -> {
            try {
                int idx = Integer.parseInt(parts[2]) - 1;
                boolean ok = mod.getFilterConfig().remove(idx);
                push(ok ? "Filter removed." : "Invalid index.", ok ? SUCCESS : ERROR);
            } catch (NumberFormatException e) { push("Usage: .filter remove <n>", ERROR); }
        }
        case "list" -> {
            List<String> pats = mod.getFilterConfig().getPatterns();
            var chatHud = MinecraftClient.getInstance().inGameHud.getChatHud();
            chatHud.addMessage(Text.literal("§6[Mandatory] Filters (" + pats.size() + "/8):"));
            for (int i = 0; i < pats.size(); i++)
                chatHud.addMessage(Text.literal("  §f" + (i + 1) + ". §7" + pats.get(i)));
        }
        case "clear" -> {
            mod.getFilterConfig().getPatterns().forEach(_ -> mod.getFilterConfig().remove(0));
            push("All filters cleared.", INFO);
        }
        default -> push("Unknown filter sub-command.", ERROR);
    }
}
```

`getFilterModule()` is a private helper that casts from `ModuleRegistry.getInstance().getById("message_filter")`.

### Core Algorithm

`onReceiveChat(Text message)` (returns `ActionResult`):

```
1. Guard: patterns list is empty → return PASS.
2. String plain = message.getString();
3. For each pattern in filterConfig.getPatterns():
     boolean matched;
     if (useRegex.get()):
         try { matched = plain.matches(pattern); }
         catch (PatternSyntaxException e):
             // invalid regex → fall back to substring match, log warning once
             matched = plain.contains(pattern);
     else:
         matched = plain.toLowerCase().contains(pattern.toLowerCase());
     if (matched):
         suppressedCount++;
         return ActionResult.FAIL;   // signals caller to return false from ALLOW_CHAT
4. return PASS.
```

`suppressedCount` is a plain `int` field on the module instance, incremented per suppressed
message, reset to 0 on `onDisable()`.

### Counter HUD (action bar)

`onRenderHud` is used for persistent HUD elements. The action bar (`player.sendMessage(..., true)`)
is simpler but overwrites whatever the server sends. Use the action bar only if no server action
bar message is active (check `mc.inGameHud.getOverlayMessage() == null` — note: this field is
private in 1.21.11, so check via Mixin accessor or use `onRenderHud` overlay drawing instead).

Recommended approach: render directly in `onRenderHud` in the bottom-centre area (above the
hotbar) when `showCounter.get() && suppressedCount > 0`:

```java
@Override
public void onRenderHud(DrawContext ctx, float tickDelta) {
    if (!showCounter.get() || suppressedCount == 0) return;
    MinecraftClient mc = MinecraftClient.getInstance();
    String text = "§7[Filter] §f" + suppressedCount + " blocked";
    int x = mc.getWindow().getScaledWidth() / 2
             - mc.textRenderer.getWidth(text) / 2;
    int y = mc.getWindow().getScaledHeight() - 48; // above hotbar
    ctx.drawTextWithShadow(mc.textRenderer, text, x, y, 0xFFFFFFFF);
}
```

### Edge Cases

- Empty pattern string (`""`) — matches every message; add validation in `FilterConfig.add()` to
  reject blank strings.
- Regex with catastrophic backtracking — wrap `matches()` in a try/catch with a reasonable
  timeout using `java.util.concurrent.Future` (optional, but recommended if regex is enabled).
  For MVP, a plain try/catch on `PatternSyntaxException` with substring fallback is sufficient.
- `mc.player == null` — guard at the top of `onReceiveChat`.
- Module disabled mid-session — `suppressedCount` resets to 0 in `onDisable()`.
- FilterConfig file deleted externally while the game is running — `patterns` list becomes empty
  on next `load()`, effectively disabling filtering without crashing.
- Pattern list at capacity (8) — `add()` returns `false`; show ERROR notification.

---

## Translation Keys

```json
"noctra.module.message_filter.name": "Message Filter",
"noctra.module.message_filter.description": "Hides chat messages matching your filter list.",
"noctra.message_filter.setting.use_regex": "Use Regex",
"noctra.message_filter.setting.show_counter": "Show Filter Count"
```

---

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/message_filter.png`
**Size:** 32x32 PNG
**Suggestion:** A funnel/filter shape with an X or slash through a speech bubble, rendered in
a muted red to convey blocking/suppression.
