# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Noctra** is a client-side Fabric mod for Minecraft 1.21.11. It is a vanilla+ mod — no new items, blocks, or server-side content. All features are purely client-side and optional. The mod adds a carousel-style menu (accessible from the pause menu) for toggling modular features.

- **Group ID / Package root:** `de.Snenjih` / `de.snenjih.noctra`
- **Mod ID:** `mandatory`
- **GitHub:** https://github.com/Snenjih/Noctra

---

## Build Commands

```bash
# Full build (produces JAR in build/libs/)
./gradlew build

# Compile only (fast feedback on errors)
./gradlew compileJava

# Launch Minecraft client with the mod loaded
./gradlew runClient

# Bump version (edits gradle.properties only)
./scripts/bump-version.sh 1.2.3

# Bump, commit, tag, and push in one step
./scripts/bump-version.sh 1.2.3 -y
```

The release JAR is `build/libs/noctra-<version>.jar`. The `-dev` and `-sources` JARs are build artifacts only.

---

## Tech Stack

| Dependency | Version |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.141.4+1.21.11 |
| Yarn Mappings | 1.21.11+build.6 |
| Fabric Loom | 1.17.11 (set in `settings.gradle.kts` pluginManagement) |
| Java | 21 |

The Loom version is declared in `settings.gradle.kts` → `pluginManagement.plugins`, **not** inside `build.gradle.kts`. `build.gradle.kts` uses `id("fabric-loom")` with no version.

All dependency versions live in `gradle.properties`. `mod_version` is the only field `bump-version.sh` touches.

---

## Architecture

### Module System

Every feature is a self-contained class **extending `BaseModule`** (in `modules/api/`):

```java
public class MyFeatureModule extends BaseModule {
    public MyFeatureModule() {
        super("my_feature", "My Feature", "Description.", ModuleCategory.UTILITY,
              Identifier.of("noctra", "modules/my_feature"));
        // optional: registerKeybind("key.mandatory.my_feature", GLFW.GLFW_KEY_UNKNOWN);
        // optional: addSetting(new BooleanSetting("some_opt", "Label", false));
    }
    @Override public void onEnable()  { /* subscribe to events */ }
    @Override public void onDisable() { /* unsubscribe */ }
}
```

`BaseModule.setEnabled()` calls `onEnable()`/`onDisable()` automatically — **do not call them explicitly from `ModuleRegistry`**; they fire once via `setEnabled()`.

Available hooks (all `default` no-ops on `Module`): `onClientTick`, `onRenderHud`, `onRenderWorld`, `onJoinWorld`, `onLeaveWorld`, `onSendChat`, `onReceiveChat`, `onInteractItem`, `onAttackEntity`.

Features subscribe to Minecraft events inside `onEnable()` and unsubscribe inside `onDisable()`. **Never** register permanent listeners in the constructor.

### Adding a New Feature

1. Create `modules/impl/my_feature/MyFeatureModule.java` extending `BaseModule`.
   Package: `de.snenjih.noctra.modules.impl.my_feature`
2. Add one line in `Noctra.onInitializeClient()`:
   ```java
   registry.register(new MyFeatureModule());
   ```
3. Add a 32×32 PNG icon to:
   ```
   src/main/resources/assets/noctra/textures/gui/sprites/modules/<feature_id>.png
   ```
   Return `Identifier.of("noctra", "modules/<feature_id>")` from `getIconTexture()`.
   Minecraft's GUI sprite atlas picks up anything under `textures/gui/sprites/` automatically.
4. Add translation keys to `assets/noctra/lang/en_us.json` if needed.
5. If the feature needs to intercept game events, write a new Mixin in `mixin/` and register it in `noctra.mixins.json`.

### Adding a HUD Element Module

A module that renders a movable HUD overlay also implements `HudElement`:

```java
public class MyHudModule extends BaseModule implements HudElement {
    @Override public String getHudId()         { return "my_hud"; }
    @Override public String getHudName()        { return "My HUD"; }
    @Override public int    getDefaultWidth()   { return 120; }
    @Override public int    getDefaultHeight()  { return 30; }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        // Render at given position/size (managed by HudRegistry)
        ctx.fill(x, y, x + w, y + h, 0xCC0D1B2A); // dark bg
        ctx.drawTextWithShadow(textRenderer, "My Text", x + 4, y + 4, 0xFFFFFFFF);
    }
}
```

In `Noctra.onInitializeClient()`, after `registry.register(myHudModule)`:
```java
HudRegistry.register(myHudModule, defaultX, defaultY);
```

### Available Setting Types

| Type | Usage | Notes |
|---|---|---|
| `BooleanSetting` | On/off toggle | `new BooleanSetting("id", "Label", defaultValue)` |
| `IntSetting` | Integer value | `new IntSetting("id", "Label", default, min, max)` |
| `FloatSetting` | Float value | `new FloatSetting("id", "Label", default, min, max)` |
| `KeybindSetting` | GLFW key code (int) | `new KeybindSetting("id", "Label", GLFW.GLFW_KEY_UNKNOWN)` |
| `ColorSetting` | ARGB color (int) | `new ColorSetting("id", "Label", 0xFFFFFFFF)` |
| `TextSetting` | String with max length | `new TextSetting("id", "Label", "default", maxLength)` |

Use `beginSection("Section Name")` before `addSetting(...)` to group settings visually.

### Registry & Config

`ModuleRegistry` (singleton, created in `Noctra`) holds the ordered list of modules shown in the carousel. `ModuleRegistry.register()` restores the saved enabled-state from `ModConfig` before adding the module to the list. `ModuleRegistry.toggle()` flips a module and immediately persists via `ModConfig`.

`ModConfig` reads/writes `<minecraft>/config/noctra.json` as nested JSON (v2 format):
```json
{ "_version": 2, "elytra_swap": { "enabled": true, "some_setting": 3.0 } }
```
Old flat-boolean files (v1) are auto-migrated on load. `ModConfig.getInstance()` is a static accessor.
`ModuleRegistry.register()` loads settings from config before calling `setEnabled()`.

### Menu / Carousel

`CarouselScreen` extends `Screen` and is opened from the pause menu via `GameMenuScreenMixin` (injects a "Noctra" button above the vanilla "Back to Game" button).

- Scroll physics: `scrollVelocity` decays by factor `0.85` each tick; snaps to nearest card index when velocity drops below `0.5`.
- The active card is `Math.round(scrollOffset / CARD_SPACING)`.
- `CarouselRenderer` is a pure rendering helper; it has no state.
- Toggle button is drawn at `height/2 + CARD_HEIGHT/2 + 10`, width 120.

### Mixins

| Class | Target | Purpose |
|---|---|---|
| `GameMenuScreenMixin` | `GameMenuScreen.init` | Adds "Noctra" button to pause menu |
| `ClientInteractionMixin` | `ClientPlayerInteractionManager.interactItem` | Routes right-click — calls `module.onInteractItem()` on all enabled modules in order |

Mixins are `@At("HEAD")` + `cancellable = true`. A module returns `ActionResult.PASS` to let vanilla continue, `SUCCESS` or `FAIL` to cancel. The interaction mixin only delegates to modules that specifically handle the event — other modules never see it.

---

## 1.21.11 API Specifics (Breaking vs. Older MC)

These are NOT obvious from class names; they caused build failures and must be followed:

### Fabric API 0.141.4 — rendering & input gotchas

- **`HudRenderCallback` is `@Deprecated`** → use `HudElementRegistry.addLast(Identifier, HudElement)` from `net.fabricmc.fabric.api.client.rendering.v1.hud`
- **`WorldRenderContext` / `WorldRenderEvents`** live in the **subpackage** `net.fabricmc.fabric.api.client.rendering.v1.world` — not `.v1` directly
- **`RenderTickCounter.getTickDelta()` does not exist** → use `getTickProgress(boolean)`
- **`KeyBinding` constructor takes `KeyBinding.Category`** (a Record), not a `String` — create with `KeyBinding.Category.create(Identifier.of("noctra", "mandatory"))`
- **`ClientSendMessageEvents.CHAT` is void** (notify-only) → use `.ALLOW_CHAT` (returns `boolean`: `true` = allow, `false` = cancel) for intercepting/cancelling outgoing chat

- **No `ArmorItem`** — check chest-equippable items via:
  ```java
  EquippableComponent eq = stack.get(DataComponentTypes.EQUIPPABLE);
  eq != null && eq.slot() == EquipmentSlot.CHEST
  ```
  This covers both Elytra and all chestplates with one check.

- **No `EnchantmentHelper.hasBindingCurse(ItemStack)`** — use component API:
  ```java
  stack.getEnchantments().getEnchantments().stream()
      .anyMatch(e -> e.matchesKey(Enchantments.BINDING_CURSE))
  ```

- **`isFallFlying()` → `isGliding()`** on `LivingEntity`.

- **`PlayerInventory.selectedSlot` is private** → use `getSelectedSlot()`.

- **`DrawContext.drawBorder()` is gone** → use `drawStrokedRectangle(x, y, w, h, color)`.

- **`DrawContext.drawTexture()` with RenderLayer is gone** → for GUI sprites use:
  ```java
  ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, spriteId, x, y, w, h);
  // import net.minecraft.client.gl.RenderPipelines
  ```

- **Mouse event signatures changed** — `Screen` overrides now receive a `Click` record:
  ```java
  mouseClicked(Click click, boolean releaseOnly)  // click.x(), click.y(), click.button()
  mouseReleased(Click click)
  mouseDragged(Click click, double deltaX, double deltaY)
  ```

---

## Inventory Slot Numbers (PlayerScreenHandler)

Required for any feature that programmatically moves items:

| Slot | Meaning |
|---|---|
| 5 | Helmet (HEAD) |
| **6** | **Chestplate / Elytra (CHEST)** |
| 7 | Leggings (LEGS) |
| 8 | Boots (FEET) |
| 9–35 | Main inventory |
| 36–44 | Hotbar (0–8) |
| 45 | Offhand |

Hotbar active slot: `36 + player.getInventory().getSelectedSlot()`

Vanilla-compatible inventory swap (works without the inventory screen open, syncs with server):
```java
int syncId = player.playerScreenHandler.syncId;
mc.interactionManager.clickSlot(syncId, hotbarSlot, 0, SlotActionType.PICKUP, player);
mc.interactionManager.clickSlot(syncId, CHEST_SLOT, 0, SlotActionType.PICKUP, player);
mc.interactionManager.clickSlot(syncId, hotbarSlot, 0, SlotActionType.PICKUP, player); // only if chest was non-empty
```

---

## CI / CD

- **CI** (`.github/workflows/ci.yml`): runs `./gradlew build` on every push and PR; uploads JAR artifact for 7 days.
- **Release** (`.github/workflows/release.yml`): triggered by `v*` tags; builds, generates a commit-based changelog since the previous tag, and publishes a GitHub Release with the JAR attached.
- **`scripts/bump-version.sh`**: updates `mod_version` in `gradle.properties` and optionally commits + tags + pushes with `-y`.

To release a new version:
```bash
./scripts/bump-version.sh 1.1.0 -y   # commits, tags v1.1.0, pushes → triggers release workflow
```

---

## Task System

All planned features are tracked in `tasks/TASKS.md`. When the user says **"mach eine task"**, **"do a task"**, or **"implement a task"**, follow this exact workflow:

1. **Read `tasks/TASKS.md`** — find the next entry with status `[ ] TODO`.
2. **Read the full spec file** listed on that line (e.g. `tasks/utility/auto_totem.md`).
3. **Implement the module** exactly as specified: class name, ID, settings, event hooks, mixins, translation keys, icon path.
4. **Register** the new module in `Noctra.onInitializeClient()`.
5. **Mark done** in `tasks/TASKS.md`: change `[ ]` → `[x]` on the task line.
6. **Mark done** inside the task spec file itself: `Status: [ ] TODO` → `Status: [x] DONE`.
7. **Build & test** — run `./gradlew compileJava` and verify it compiles with zero errors. If it fails, fix all errors before proceeding.
8. **Commit** — stage and commit all changed files with a message following the pattern `feat: add <ModuleName> module`. Example:
   ```bash
   git add src/ tasks/
   git commit -m "feat: add StackRefill module"
   ```

**Always keep `tasks/TASKS.md` up to date.** After completing any task, update both files before finishing. Never skip the build step — a task is only done when the code compiles cleanly.

### Task File Format

Each `tasks/<category>/<module_id>.md` contains:
- Module ID, class name, category
- Full settings spec (type, default, range/options, label, description)
- Implementation notes (which event hooks, mixin targets if needed, core algorithm)
- Edge cases to handle
- Translation keys for `en_us.json`
- Icon path and visual description

### Rules
- Never implement a task partially — either fully complete it or leave `[ ] TODO`.
- If a task requires a new Mixin, add the class to `mixin/` AND register it in `noctra.mixins.json`.
- Each new module needs a 32×32 PNG icon at `src/main/resources/assets/noctra/textures/gui/sprites/modules/<id>.png`.
