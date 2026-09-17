# Fullbright

**ID:** `fullbright`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/fullbright/FullbrightModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.fullbright`

## System Notes (Updated)

- Module Ordner: `modules/impl/fullbright/FullbrightModule.java`
- Package: `de.snenjih.noctra.modules.impl.fullbright`
- Implementiert HudElement: Nein (kein HUD-Overlay benötigt)
- Neue Setting-Typen verfügbar: `KeybindSetting`, `ColorSetting`, `TextSetting`

## Description

Überschreibt den Gamma-Wert von Minecraft auf ein Maximum, sodass auch dunkle Bereiche (Höhlen, Nacht, Nether) vollständig hell dargestellt werden. Beim Deaktivieren wird der ursprüngliche Gamma-Wert des Spielers wiederhergestellt. Spart Fackeln und verbessert die Sicht in dunklen Strukturen erheblich.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `gamma_value` | Float | `16.0` | `1.0–16.0` | "Gamma Value" | Gamma-Zielwert (16.0 = maximale Helligkeit) |

## Implementation

### Event Hooks

- `onEnable()` — Originalen Gamma-Wert speichern, Gamma auf `gammaValue` setzen.
- `onDisable()` — Gamma auf den gespeicherten Originalwert zurücksetzen.
- `onClientTick(MinecraftClient client)` — Gamma-Wert jedes Tick erzwingen, falls externe Systeme (z. B. Optionen-Screen) ihn zurückgesetzt haben.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
Felder in der Klasse:
    private double savedGamma = 1.0;

onEnable():
    MinecraftClient mc = MinecraftClient.getInstance()
    savedGamma = mc.options.getGamma().getValue()   // SimpleOption<Double>
    mc.options.getGamma().setValue(gammaValue.get().doubleValue())

onClientTick(MinecraftClient client):
    if (client.options == null) return
    double current = client.options.getGamma().getValue()
    double target  = gammaValue.get().doubleValue()
    if (Math.abs(current - target) > 0.01)
        client.options.getGamma().setValue(target)    // Wert erzwingen

onDisable():
    MinecraftClient mc = MinecraftClient.getInstance()
    mc.options.getGamma().setValue(savedGamma)
```

### Edge Cases

- `mc.options == null` (sehr früher Init): Guard-Clause in `onEnable` und `onClientTick`.
- Spieler ändert Gamma im Optionen-Screen während Fullbright aktiv ist: `onClientTick` korrigiert den Wert im nächsten Tick zurück. Dies ist bewusstes Verhalten — das Modul ist aktiv.
- Modul wird deaktiviert, bevor Welt geladen ist: `savedGamma` bleibt `1.0` (Default), kein Problem.
- `gammaValue`-Setting geändert während aktiv: `onClientTick` wendet den neuen Wert automatisch an.
- Singleplayer vs. Multiplayer: Gamma ist rein client-seitig, kein Unterschied.

## Translation Keys

```json
"noctra.fullbright.name": "Fullbright",
"noctra.fullbright.description": "Overrides gamma for maximum visibility in dark areas.",
"noctra.fullbright.gamma_value": "Gamma Value"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/fullbright.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Stilisierte Sonne oder glühendes Auge; helles Gelb/Orange auf dunklem Hintergrund. Alternativ: Fackel mit Glüheffekt.
