# Pitch Lock

**ID:** `pitch_lock`  
**Category:** ELYTRA  
**Status:** [x] DONE  
**Class:** `modules/impl/pitch_lock/PitchLockModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.pitch_lock`

## System Notes (Updated)

- Module Ordner: `modules/impl/pitch_lock/PitchLockModule.java`
- Package: `de.snenjih.noctra.modules.impl.pitch_lock`
- Implementiert HudElement: Nein
- `KeybindSetting` für den Lock-Toggle-Keybind verwenden statt manuellem `KeyBinding`

## Description

Hält den Neigungswinkel (Pitch) der Kamera während des Gleitens auf einem konfigurierten Zielwert.
Dadurch kann eine optimale Reisegeschwindigkeit beibehalten werden, ohne die Maus ständig manuell
justieren zu müssen.

Elytra-Physik-Hintergrund: Ein Pitch von etwa -29° bis -30° (leicht nach unten geschaut) liefert
bei voller Horizontalgeschwindigkeit den besten Kompromiss aus Strecke und Höhenverlust.
Steilerer Winkel (negativer) = schneller aber mehr Höhenverlust.
Flacherer Winkel (positiv) = langsamer, gleicht Höhenverlust aus, verliert aber zu viel
Geschwindigkeit. Der Lock lässt sich mit einer konfigurierbaren Taste ein- und ausschalten,
ohne das gesamte Modul zu deaktivieren.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `target_pitch` | Float | `-29.0` | `-90.0 – 90.0` | "Target Pitch (°)" | Zielneigung in Grad. Negativ = nach oben, positiv = nach unten. -29° ist optimal für Reiseflug. |
| `lock_strength` | Float | `1.0` | `0.1 – 1.0` | "Lock Strength" | Interpolationsstärke pro Tick (1.0 = sofort, 0.1 = sanft). Werte unter 1.0 führen zu einem weichen Übergang. |
| `only_while_gliding` | Boolean | `true` | `true / false` | "Only While Gliding" | Pitch nur fixieren wenn player.isGliding(). Bei false wird auch beim normalen Fliegen/Fallen gesetzt. |

## Implementation

### Event Hooks

- `onEnable()` — Keybind registrieren via `KeybindManager.register()`
- `onClientTick(MinecraftClient client)` — Pitch-Override anwenden

### Required Mixins

Kein Mixin erforderlich. `ClientPlayerEntity` erbt `setPitch(float)` von `Entity`, das ist
direkt aufrufbar auf dem Client-Objekt. Pitch-Werte werden vom Client-Tick sofort für
Rendering und Physik verwendet; der Server empfängt den Wert regulär mit dem nächsten
Positions-Paket.

### Core Algorithm

```
1. In onEnable():
   a. Keybind registrieren:
      lockKeybind = KeybindManager.register(this, "key.mandatory.pitch_lock_toggle", GLFW.GLFW_KEY_UNKNOWN)
      // Kein Default-Key, da es schnell zu Konflikten kommt. User setzt selbst.
   b. lockActive = false (Lock startet inaktiv)

2. In onClientTick(client):
   a. Wenn client.player == null → return
   b. Keybind prüfen: while (lockKeybind.wasPressed()) { lockActive = !lockActive; }
      (Hinweis: wasPressed() in client tick aufrufen, NICHT in onEnable/onDisable)
   c. Wenn !lockActive → return
   d. Wenn only_while_gliding.get() == true UND !player.isGliding() → return
   e. float currentPitch = player.getPitch()
   f. float targetPitch  = target_pitch.get()
   g. float strength     = lock_strength.get()
   h. float newPitch     = currentPitch + (targetPitch - currentPitch) * strength
      // Stärke 1.0 → sofort snap; Stärke 0.1 → sanfte Annäherung
   i. player.setPitch(newPitch)

3. Kein onEnable/onDisable Fabric-Event-Setup nötig — onClientTick läuft über die
   zentrale Schleife in NoctraMod, solange das Modul enabled ist.
```

### Keybind-Details

- Registrierung in `onEnable()` via `KeybindManager.register(this, translationKey, defaultKey)`
- `KeybindManager` verwaltet den Toggle selbst — aber hier KEIN ModuleRegistry.toggle()
  verwenden, sondern einen eigenen `lockActive`-Flag im Modul, weil der Keybind nur den
  internen Lock-Zustand, nicht das Modul selbst, umschalten soll.
- **Alternative:** Da `KeybindManager.register()` das Modul-Toggle auslöst (ruft
  `ModuleRegistry.toggle(module)`), muss für einen internen Sub-Toggle ein eigener
  `KeyBinding` direkt via `KeyBindingHelper.registerKeyBinding()` ohne `KeybindManager`
  registriert werden. Im `onClientTick` dann `while (lockKeybind.wasPressed())` abfragen.

### Edge Cases

- **Pitch außerhalb von ±90°:** `setPitch` sollte intern geclampt werden, aber zur Sicherheit
  vor dem Setzen clampen: `Math.clamp(newPitch, -90f, 90f)`
- **Spieler schaut in Menü (Inventory/Chat offen):** `client.currentScreen != null` →
  Lock nicht anwenden, um keine Loop-Probleme mit GUI-Navigation zu erzeugen
- **Creative Mode / Spectator:** onClientTick Guard auf
  `!player.isSpectator()` (Creative ist ok, Spectator schaut sich frei um)
- **Keybind-Konflikt mit Vanilla-Tasten:** Kein Default-Key setzen; User konfiguriert in
  Vanilla Controls
- **Lock-Zustand persistieren:** `lockActive` ist flüchtig (nicht in Config speichern).
  Beim Deaktivieren des Moduls wird onDisable() aufgerufen, aber da lockActive eh
  nur via onClientTick wirkt, muss nichts explizit zurückgesetzt werden
- **Mehrere Ticks Interpolation:** Bei `strength < 1.0` nähert sich der Pitch asymptotisch
  an. Der Lock ist nie exakt auf Zielwert, was in Ordnung ist.

## Translation Keys

```json
"noctra.pitch_lock.name": "Pitch Lock",
"noctra.pitch_lock.description": "Locks your view angle while gliding for consistent travel speed.",
"noctra.pitch_lock.target_pitch": "Target Pitch (°)",
"noctra.pitch_lock.lock_strength": "Lock Strength",
"noctra.pitch_lock.only_while_gliding": "Only While Gliding",
"key.mandatory.pitch_lock_toggle": "Toggle Pitch Lock"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/pitch_lock.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Pfeil oder Kompass der nach unten geneigt ist (etwa 30°), alternativ ein
Schloss-Symbol mit Pfeil. Stilistisch passend zu den anderen Icons im Stil des Mods (Pixel-Art,
heller Stroke auf dunklem Hintergrund).
