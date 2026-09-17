# Combo Counter HUD

**ID:** `combo_counter`  
**Category:** COMBAT  
**Status:** [x] DONE  
**Class:** `modules/impl/combo_counter/ComboCounterModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.combo_counter`

## Description

Zählt aufeinanderfolgende Treffer auf Entities ohne eigenen Schaden zu nehmen. Sobald der Spieler selbst Schaden erhält, wird der Combo-Zähler zurückgesetzt. Zeigt den aktuellen Combo-Wert sowie optional die höchste Combo der Session an. Optional kann der Combo-Zähler auch bei Inaktivität (kein Treffer für X Sekunden) zurückgesetzt werden. Reiner PvP-Trainings- und Spaß-Feature.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `reset_on_damage` | Boolean | `true` | — | "Reset on Damage" | Combo bei eigenem Schaden zurücksetzen |
| `reset_timeout` | Int | `5` | `0–60` | "Reset Timeout (s)" | Combo zurücksetzen wenn X Sekunden kein Treffer (0 = nie) |
| `show_max_combo` | Boolean | `true` | — | "Show Max Combo" | Höchste Combo der Session anzeigen |
| `show_only_when_active` | Boolean | `false` | — | "Show Only When Active" | HUD nur anzeigen wenn Combo > 0 |
| `min_display_combo` | Int | `2` | `1–10` | "Min Display Combo" | Erst ab dieser Combo anzeigen |
| `combo_colors` | Boolean | `true` | — | "Combo Colors" | Farbe ändert sich mit Combo-Höhe |
| `color_low` | Color | `0xFFFFFFFF` | ARGB Hex | "Color (1-4)" | Farbe bei Combo 1–4 |
| `color_mid` | Color | `0xFFFFFF55` | ARGB Hex | "Color (5-9)" | Farbe bei Combo 5–9 |
| `color_high` | Color | `0xFFFF8800` | ARGB Hex | "Color (10-19)" | Farbe bei Combo 10–19 |
| `color_max` | Color | `0xFFFF5555` | ARGB Hex | "Color (20+)" | Farbe bei Combo 20+ |
| `play_sound_at` | Int | `10` | `0–100` | "Sound at Combo" | Sound abspielen wenn Combo diesen Wert erreicht (0 = aus) |

## Implementation

### Event Hooks

- `onClientTick(MinecraftClient client)` — Spieler-Health-Delta für Schaden-Reset tracken, Timeout-Timer prüfen.
- `onRenderHud(DrawContext ctx, float tickDelta)` — Combo anzeigen.

### Required Mixins

**Mixin erforderlich** für das Zählen der ausgeteilten Treffer (Angriff auf Entity):

```java
// Ansatz 1: AttackEntityCallback (Fabric API)
// — Zählt Treffer, nicht Schaden-Events
// — Auch Fehlklicks ins Leere würden nicht zählen (gut!)
// — Registrierung in onEnable():
AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
    if (player == mc.player && entity instanceof LivingEntity) {
        recordHit();
    }
    return ActionResult.PASS;
});

// Ansatz 2: Mixin auf ClientPlayerInteractionManager.attackEntity()
// — Genauer, weil Attack tatsächlich gesendet wird
// Empfehlung: AttackEntityCallback (kein Mixin nötig)
```

**Schaden-Reset** (Health-Delta-Tracking wie in DamageDealtHud):

```java
// In onClientTick():
float currentHp = mc.player.getHealth()
if (currentHp < lastPlayerHp - 0.01f):  // Spieler hat Schaden bekommen
    if (resetOnDamage.get()):
        resetCombo()
lastPlayerHp = currentHp
```

### Core Algorithm

```
// Felder:
private int  currentCombo = 0
private int  maxCombo     = 0
private long lastHitTime  = 0L
private float lastPlayerHp = 20f

void recordHit():
    currentCombo++
    lastHitTime = System.currentTimeMillis()
    if (currentCombo > maxCombo) maxCombo = currentCombo
    
    if (playSoundAt.get() > 0 && currentCombo == playSoundAt.get()):
        mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f)

void resetCombo():
    currentCombo = 0

onClientTick(client):
    if (client.player == null) return
    
    // Health-Delta für Reset
    float hp = client.player.getHealth()
    if (resetOnDamage.get() && hp < lastPlayerHp - 0.01f):
        resetCombo()
    lastPlayerHp = hp
    
    // Timeout-Reset
    if (resetTimeout.get() > 0 && currentCombo > 0):
        long elapsed = System.currentTimeMillis() - lastHitTime
        if (elapsed > resetTimeout.get() * 1000L):
            resetCombo()

onEnable():
    // AttackEntityCallback registrieren (Lambda-Referenz für späteres Deregistrieren)
    handler = AttackEntityCallback.EVENT.register(...)
    lastPlayerHp = mc.player != null ? mc.player.getHealth() : 20f

onDisable():
    // Handler deregistrieren
    currentCombo = 0; maxCombo = 0

onRenderHud(ctx, tickDelta):
    if (mc.player == null) return
    if (showOnlyWhenActive.get() && currentCombo < minDisplayCombo.get()) return

    // Farbe basierend auf Combo
    int color = textColor.get()
    if (comboColors.get()):
        if      (currentCombo >= 20) color = colorMax.get()
        else if (currentCombo >= 10) color = colorHigh.get()
        else if (currentCombo >= 5)  color = colorMid.get()
        else                          color = colorLow.get()

    String comboStr = "Combo: x" + currentCombo
    String maxStr   = showMaxCombo.get() ? "Best: x" + maxCombo : null

    // Rendering...
```

**Hinweis zu AttackEntityCallback:** Dieses Event feuert beim Drücken der Angriffstaste auf eine Entität. Wenn der Angriff auf Cooldown war (damage = 0), wird der Treffer trotzdem gezählt. Für "echte Treffer mit Schaden" besser Health-Delta des Targets verwenden (wie in DamageDealtHud). Hier ist die einfachere Version (Taste gedrückt auf Entity) die spielerische Wahl.

### Edge Cases

- `AttackEntityCallback` deregistrieren in `onDisable()`. Event-Lambda als Feld speichern.
- Spieler stirbt: `onJoinWorld()` oder Health = 0 → Combo reset.
- Reset bei Respawn: `player.getHealth()` spring auf 20 nach Tod → kein negativer Delta-Trigger.
- `resetTimeout = 0`: Feature deaktiviert, kein Timeout-Check nötig.
- Combo 0 und `showOnlyWhenActive = true`: HUD unsichtbar bis erster Treffer.

## Translation Keys

```json
"noctra.combo_counter.name": "Combo Counter",
"noctra.combo_counter.description": "Counts consecutive hits without taking damage.",
"noctra.combo_counter.bg_color": "Background Color",
"noctra.combo_counter.border_color": "Border Color",
"noctra.combo_counter.text_color": "Text Color",
"noctra.combo_counter.text_shadow": "Text Shadow",
"noctra.combo_counter.text_scale": "Text Scale",
"noctra.combo_counter.show_background": "Show Background",
"noctra.combo_counter.reset_on_damage": "Reset on Damage",
"noctra.combo_counter.reset_timeout": "Reset Timeout (s)",
"noctra.combo_counter.show_max_combo": "Show Max Combo",
"noctra.combo_counter.show_only_when_active": "Show Only When Active",
"noctra.combo_counter.min_display_combo": "Min Display Combo",
"noctra.combo_counter.combo_colors": "Combo Colors",
"noctra.combo_counter.color_low": "Color (1-4)",
"noctra.combo_counter.color_mid": "Color (5-9)",
"noctra.combo_counter.color_high": "Color (10-19)",
"noctra.combo_counter.color_max": "Color (20+)",
"noctra.combo_counter.play_sound_at": "Sound at Combo"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/combo_counter.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Rotes "×3" oder "x10" in Flammen-Pixel-Art. Combo-typischer Kampf-Stil.
