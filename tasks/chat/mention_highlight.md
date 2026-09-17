# Mention Highlight

**ID:** `mention_highlight`
**Category:** CHAT
**Status:** [x] DONE
**Class:** `modules/impl/mention_highlight/MentionHighlightModule.java`
**Package:** `de.snenjih.noctra.modules.impl.mention_highlight`

## System Notes (Updated)

- Module Ordner: `modules/impl/mention_highlight/MentionHighlightModule.java`
- Package: `de.snenjih.noctra.modules.impl.mention_highlight`
- Implementiert HudElement: Nein (Chat-basiert)
- `ColorSetting` für `color`-Setting verwenden statt `IntSetting`

## Description

Scans every incoming chat message for the local player's username and re-renders the matching
substring in a configurable colour. Optionally plays a sound ping so the player gets an audible
alert when they are mentioned, which is useful on busy servers or while tabbed out.

Partial-match mode optionally treats "Steve" as a match when the own name is "Steve123", making
the feature useful for players whose server nickname is an abbreviated form of their Minecraft
username.

---

## Settings

| Setting ID       | Type    | Default      | Range / Options         | Label                   | Description                                           |
|------------------|---------|--------------|-------------------------|-------------------------|-------------------------------------------------------|
| `color`          | Int     | `0xFFFF55`   | any ARGB int            | "Highlight Colour"      | ARGB hex colour for the highlighted name substring.   |
| `play_sound`     | Boolean | `true`       | —                       | "Play Sound on Mention" | Plays `SoundEvents.BLOCK_NOTE_BLOCK_PLING` on match.  |
| `partial_match`  | Boolean | `false`      | —                       | "Partial Match"         | Matches if the own name is a prefix/suffix of a word in the message. |

`color` is stored as an `IntSetting` with min=`Integer.MIN_VALUE`, max=`Integer.MAX_VALUE` (no
clamping desired; the carousel UI will expose a colour-picker widget in a future iteration).

---

## Implementation

### Wire-up in NoctraMod

`onReceiveChat` is already declared on `Module` and will be called from the receive-message
event listener that must be added to `NoctraMod.registerEvents()`:

```java
// In NoctraMod.registerEvents(), add alongside the existing ALLOW_CHAT listener:

// Player chat messages (chat bubbles)
ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
    for (Module m : registry.getAll()) {
        if (m.isEnabled()) m.onReceiveChat(message);
    }
    return true; // never cancel here; modules only observe
});

// Game/system messages (e.g. server broadcasts, /say)
ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
    if (overlay) return true; // skip action bar messages
    for (Module m : registry.getAll()) {
        if (m.isEnabled()) m.onReceiveChat(message);
    }
    return true;
});
```

Both event listeners must be registered once at init time. They must return `true` to avoid
accidentally blocking messages. Modules that need to suppress a message should use `onSendChat`
(already wired) or a dedicated filtering module (see `message_filter.md`).

### Fabric Events Used

- `ClientReceiveMessageEvents.ALLOW_CHAT` — player chat, signed
- `ClientReceiveMessageEvents.ALLOW_GAME` — server/system messages

Both are from `net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents`.

### Required Mixins

None. The Fabric event is sufficient; no Mixin injection into `ChatHud.addMessage` is needed
because the Text object is already available in the event callback.

### Core Algorithm

`onReceiveChat(Text message)` is called with the full rendered `Text` already assembled by the
server/client.

```
1. Guard: mc.player == null → return immediately.
2. Obtain own name: String ownName = mc.player.getGameProfile().getName();
3. Extract plain string: String plain = message.getString();
4. Determine match index:
     if (partialMatch.get()):
         find any token in plain (split on non-letter chars) that contains ownName (case-insensitive)
         record the start/end char index of that token within plain
     else:
         int idx = plain.toLowerCase().indexOf(ownName.toLowerCase())
         if idx == -1 → no match, return
5. Rebuild the Text as three parts:
     Text prefix  = Text.literal(plain.substring(0, matchStart));
     Text match   = Text.literal(plain.substring(matchStart, matchEnd))
                        .styled(s -> s.withColor(color.get()));
     Text suffix  = Text.literal(plain.substring(matchEnd));
     Text rebuilt = Text.empty().append(prefix).append(match).append(suffix);
6. Inject rebuilt into chat HUD:
     MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(rebuilt);
7. Play sound if play_sound.get():
     mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1.0f, 2.0f);
```

**Important:** The rebuilt `Text` is injected via `ChatHud.addMessage()` but the *original*
message is still allowed by the `ALLOW_CHAT` / `ALLOW_GAME` handler (returns `true`). This
means the message appears **twice**: once as the original and once as the highlighted version.

To avoid duplication the module must instead use `ClientReceiveMessageEvents.MODIFY_GAME_MESSAGE`
(which provides a mutable wrapper) **or** suppress the original via `ALLOW_CHAT` returning
`false` and re-injecting the rebuilt text. The cleanest approach for 1.21.11:

```
Use ALLOW_CHAT returning false + manual addMessage() call for chat messages.
Use ALLOW_GAME returning false + manual addMessage() call for game messages.
```

This means the receive-event wiring in `NoctraMod` must be redesigned slightly for this
module: the global `ALLOW_CHAT` / `ALLOW_GAME` handlers must check whether any enabled module
wants to modify the text before deciding whether to return `true`.

Alternatively, implement this entirely in a Mixin on `ChatHud.addMessage` (see below).

### Alternative: Mixin on ChatHud (recommended for correctness)

A Mixin on `net.minecraft.client.gui.hud.ChatHud` at `addMessage(Text, ...)` gives direct
access to the Text before it is stored, avoiding the double-message problem:

```java
@Mixin(ChatHud.class)
public class ChatHudMixin {
    @ModifyArg(
        method = "addMessage(Lnet/minecraft/text/Text;...)V",
        at = @At("HEAD"),
        index = 0
    )
    private Text onAddMessage(Text message) {
        MentionHighlightModule mod = /* get from registry */;
        if (mod == null || !mod.isEnabled()) return message;
        return mod.highlight(message);
    }
}
```

Register `ChatHudMixin` in `noctra.mixins.json` under `"client"`. `highlight(Text)` is a
public method on `MentionHighlightModule` that executes steps 1-5 from the algorithm above and
returns the (potentially) modified text without any side effects.

Sound playback still happens inside `highlight()` when a match is found.

### Edge Cases

- `mc.player == null` (loading screen, main menu) — guard at the top of `onReceiveChat` /
  `highlight()`.
- Own username empty or blank — treat as no match.
- Message text shorter than own name — no match possible, fast-exit.
- `color.get()` alpha channel is `0` (fully transparent) — highlight is invisible; acceptable,
  user's misconfiguration.
- Partial-match mode with very short names (e.g. "Al") — can produce many false positives;
  document in tooltip.
- Messages that are already formatted with complex `Text` trees (hover events, click events) —
  the reconstruction above discards all formatting on the matched portion except the colour; the
  prefix and suffix also lose formatting. A full solution must walk the `Text` tree recursively.
  For MVP, the plain-string reconstruction is acceptable.

---

## Translation Keys

```json
"noctra.module.mention_highlight.name": "Mention Highlight",
"noctra.module.mention_highlight.description": "Highlights your name in chat and plays a sound ping.",
"noctra.mention_highlight.setting.color": "Highlight Colour",
"noctra.mention_highlight.setting.play_sound": "Play Sound on Mention",
"noctra.mention_highlight.setting.partial_match": "Partial Match"
```

---

## Icon

**Path:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/mention_highlight.png`
**Size:** 32x32 PNG
**Suggestion:** A speech bubble outline with a small star or exclamation mark inside, rendered
in yellow to match the default highlight colour.
