# Sneak Toggle

**ID:** `sneak_toggle`  
**Category:** UTILITY  
**Status:** [x] DONE  
**Class:** `modules/impl/sneak_toggle/SneakToggleModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.sneak_toggle`

## System Notes (Updated)

- Module Ordner: `modules/impl/sneak_toggle/SneakToggleModule.java`
- Package: `de.snenjih.noctra.modules.impl.sneak_toggle`
- Implementiert HudElement: Nein
- Benötigt Mixin: `SneakToggleMixin` auf `KeyboardInput` oder `ClientPlayerEntity`
- Analog zu SprintToggleModule (`modules/impl/sprint_toggle/SprintToggleModule.java`)

## Description

Aktiviert dauerhaftes Schleichen als Toggle statt als Hold-Taste. Einmal aktiviert schleicht der Spieler permanent, ohne die Shift-Taste gedrückt halten zu müssen. Nützlich beim Bauen über Abgründen oder bei längeren Aktionen, die Schleichen erfordern. Das Modul kann jederzeit durch erneutes Drücken der Sneak-Taste deaktiviert werden.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `disable_on_sneak_press` | Boolean | `true` | — | "Disable on Sneak Press" | Schleichen durch erneutes Drücken der Sneak-Taste deaktivieren |
| `disable_on_sprint` | Boolean | `true` | — | "Disable on Sprint" | Schleich-Toggle deaktivieren wenn Spieler zu sprinten beginnt |

## Implementation

### Event Hooks

- `onEnable()` — Internen `sneakActive`-Flag auf `true` setzen.
- `onDisable()` — Internen `sneakActive`-Flag auf `false` setzen.
- `onClientTick(MinecraftClient client)` — Prüft ob Sneak-Taste neu gedrückt wurde (um zu togglen) und setzt `sneakActive`.

### Required Mixins

- **Class:** `mixin/SneakToggleMixin.java`
- **Target:** `net.minecraft.client.input.KeyboardInput`
- **Methode:** `tick(boolean slowDown, float f)` (Yarn: `tick`)
- **Injection:** `@Inject(at = @At("TAIL"), method = "tick(ZF)V")`
- **Zweck:** Nach dem Vanilla-Tick des `KeyboardInput`: Wenn das Modul aktiv ist und `sneakActive` true ist, `this.sneaking = true` setzen (überschreibt den Vanilla-Wert, der nur von der Taste abhängt).

*Hinweis: In 1.21.11 heißt das Feld `KeyboardInput.sneaking` (oder `input.sneaking` im `PlayerInput`-Record). Yarn-Mapping prüfen — in neueren Versionen kann es `playerInput` sein. Ggf. Access-Widener für `sneaking`-Feld nötig.*

### Core Algorithm

```
Felder in der Klasse:
    private boolean sneakActive = false;
    private boolean sneakKeyWasPressed = false;

onEnable():
    sneakActive = true
    sneakKeyWasPressed = false

onDisable():
    sneakActive = false

onClientTick(MinecraftClient client):
    if (client.player == null) return
    MinecraftClient mc = client
    boolean sneakKeyNowPressed = mc.options.sneakKey.isPressed()

    // Toggle wenn Taste frisch gedrückt wird
    if (disableOnSneakPress.get()):
        if (sneakKeyNowPressed && !sneakKeyWasPressed):
            sneakActive = !sneakActive

    // Sprint deaktiviert Schleichen
    if (disableOnSprint.get() && client.player.isSprinting()):
        sneakActive = false

    sneakKeyWasPressed = sneakKeyNowPressed

// Im SneakToggleMixin (KeyboardInput.tick TAIL):
SneakToggleModule module = NoctraMod.getRegistry().getModule("sneak_toggle");
if (module != null && module.isEnabled() && module.isSneakActive()):
    this.sneaking = true
    // Falls PlayerInput-Record (1.21.11): Mixin muss @Mutable verwenden oder
    // auf ein anderes Target (ClientPlayerEntity.tickMovement) ausweichen

// Alternative falls KeyboardInput.sneaking ein Record-Feld ist:
// Mixin auf ClientPlayerEntity.tickMovement() @At("TAIL"):
//   if (module.isEnabled() && module.isSneakActive()):
//       this.setSneaking(true)
//       this.input.sneaking = true  // falls direkt zugänglich
```

### Edge Cases

- `KeyboardInput.sneaking` ist ein Record-Feld (immutable in 1.21.11): In diesem Fall ist der Mixin-Ansatz nicht direkt möglich. Alternative: Mixin auf `ClientPlayerEntity.tickMovement()` mit `@At("TAIL")` und `setSneaking(true)`. Prüfen was `PlayerInput` in 1.21.11 ist (record oder class).
- Sprint + Sneak gleichzeitig: Vanilla verbietet Sprint während Sneak. `disableOnSprint` sorgt dafür, dass beim Sprint-Start `sneakActive = false`.
- Climbable Blocks (Leitern): Schleichen auf Leitern verhindert das Rutschen. Funktioniert korrekt, da `isSneaking()` true ist.
- Schwimmen: Schleichen im Wasser hat keinen Effekt auf die Physik in 1.21.11. Kein Edge Case.
- Modul-Toggle über Carousel: `setEnabled(false)` ruft `onDisable()` auf, welches `sneakActive = false` setzt und das Mixin deaktiviert.
- Spieler im Menü (Inventory offen): `KeyboardInput.tick` wird nicht aufgerufen wenn ein Screen offen ist. `sneakActive`-State bleibt; bei Schließen des Menüs greift Mixin wieder.
- Access-Widener: Falls `KeyboardInput.sneaking` private ist, muss `mandatory.accesswidener` einen Eintrag für das Feld enthalten: `accessible field net/minecraft/client/input/KeyboardInput sneaking Z`

## Translation Keys

```json
"noctra.sneak_toggle.name": "Sneak Toggle",
"noctra.sneak_toggle.description": "Toggle sneaking permanently without holding the Shift key.",
"noctra.sneak_toggle.disable_on_sneak_press": "Disable on Sneak Press",
"noctra.sneak_toggle.disable_on_sprint": "Disable on Sprint"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/sneak_toggle.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Schleichender Charakter (gebückte Silhouette) oder ein Pfeil nach unten mit einem Schloss-Symbol. Farbe: Grau/Blaugrau auf dunklem Hintergrund.
