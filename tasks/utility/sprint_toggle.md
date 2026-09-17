# Sprint Toggle

**ID:** `sprint_toggle`  
**Category:** UTILITY  
**Status:** [x] DONE  
**Class:** `modules/impl/SprintToggleModule.java`

## Description

Aktiviert dauerhaftes Sprinten ohne dass die Sprint-Taste gehalten werden muss. Das Modul erzwingt den Sprint-Zustand des Spielers in jedem Tick, sofern die Vanilla-Bedingungen (Hunger, nicht schleichen, etc.) erfüllt sind. Optionale Einstellung erlaubt Sprint auch beim Schwimmen.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `sprint_in_water` | Boolean | `false` | — | "Sprint in Water" | Sprint auch beim Schwimmen erzwingen |
| `sprint_in_air` | Boolean | `true` | — | "Sprint in Air" | Sprint während des Springens/Fliegens erzwingen |

## Implementation

### Event Hooks

- `onClientTick(MinecraftClient client)` — Prüft Sprint-Bedingungen und erzwingt `player.setSprinting(true)` falls alle Bedingungen erfüllt.

### Required Mixins

Kein Mixin erforderlich. `player.setSprinting(true)` in `onClientTick` reicht, da Vanilla den Sprint-Zustand jeden Tick über `KeyboardInput` setzt — der `setSprinting(true)`-Aufruf danach überschreibt diesen.

*Hinweis:* Vanilla setzt `setSprinting` in `ClientPlayerEntity.tickMovement()` basierend auf `input.playerInput.sprint()`. Da `onClientTick` nach dem Tick-Event läuft, wird der Wert korrekt am Ende überschrieben. Falls Timing-Probleme auftreten, kann alternativ ein Mixin auf `KeyboardInput.tick()` nötig sein (siehe Edge Cases).

### Core Algorithm

```
Felder in der Klasse:
    // keine persistenten Felder

onClientTick(MinecraftClient client):
    if (client.player == null) return
    ClientPlayerEntity player = client.player

    // Sprint-Bedingungen prüfen (analog zu Vanilla)
    boolean canSprint =
        !player.isSneaking()                              // nicht schleichen
        && player.getHungerManager().getFoodLevel() > 6  // Hunger > 3 Stücke
        && !player.isUsingItem()                          // kein Item in Verwendung (Bogen, Schwert blocken)
        && !player.hasStatusEffect(StatusEffects.BLINDNESS)

    if (!sprintInWater.get() && player.isTouchingWater()) canSprint = false
    if (!sprintInAir.get() && !player.isOnGround() && !player.isGliding()) canSprint = false

    // Vorwärtsbewegung nötig (sonst kein Sprint möglich)
    // player.input.playerInput.forward() ist das korrekte Feld in 1.21.11
    // Alternativ: player.forwardSpeed > 0
    boolean movingForward = player.forwardSpeed > 0.0f

    if (canSprint && movingForward):
        player.setSprinting(true)
```

### Edge Cases

- `player.isSneaking()`: Vanilla verhindert Sprint während Schleichen; dieser Check muss beibehalten werden.
- Hunger == 0: `getFoodLevel() > 6` ist false → kein Sprint. Verhindert "Sprint durch Verhungern"-Bug.
- Spieler blockiert mit Schild/Bogen (`isUsingItem()`): Sprint wird unterbrochen wie in Vanilla.
- Elytra-Gleiten (`player.isGliding()`): Sprint während Gleiten ist irrelevant (Elytra hat eigene Physik). `setSprinting(true)` hat keinen unerwünschten Effekt, schadet aber auch nicht.
- Creative Mode / Spectator: In Creative ist Hunger immer voll, Sprint funktioniert immer. In Spectator gibt es keinen `ClientPlayerEntity` mit Hunger — Guard-Clause `if (player.isSpectator()) return`.
- Timing: Vanilla setzt Sprint in `tickMovement()`. Falls `onClientTick` zu früh feuert, kann Vanilla den Wert danach zurücksetzen. In diesem Fall: Mixin auf `ClientPlayerEntity.tickMovement()` mit `@Inject(at = @At("TAIL"))` als Fallback.
- Keybind zum Deaktivieren: Kein Keybind in dieser Spec (Modul-Toggle im Carousel). Kann via `registerKeybind` ergänzt werden.
- Server-Side Sprint: `setSprinting(true)` sendet ein Vanilla-Paket an den Server. Kein Anti-Cheat-Problem, da die Bedingungen identisch zu Vanilla-Sprint sind.

## Translation Keys

```json
"noctra.sprint_toggle.name": "Sprint Toggle",
"noctra.sprint_toggle.description": "Automatically sprints without holding the sprint key.",
"noctra.sprint_toggle.sprint_in_water": "Sprint in Water",
"noctra.sprint_toggle.sprint_in_air": "Sprint in Air"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/sprint_toggle.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Stilisierter rennender Charakter oder Schuh mit Bewegungs-Linien. Farbe: Hellgrün/Türkis auf dunklem Hintergrund.
