# Attack Cooldown Indicator

**ID:** `attack_cooldown_indicator`  
**Category:** COMBAT  
**Status:** [x] DONE  
**Class:** `modules/impl/attack_cooldown_indicator/AttackCooldownIndicatorModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.attack_cooldown_indicator`

## System Notes (Updated)

- Module Ordner: `modules/impl/attack_cooldown_indicator/AttackCooldownIndicatorModule.java`
- Package: `de.snenjih.noctra.modules.impl.attack_cooldown_indicator`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`
- `ColorSetting` für Balkenfarben verwenden

## Description

Zeigt den Waffen-Cooldown des Spielers als anpassbaren Balken auf dem HUD an. Vanilla zeigt bereits einen kleinen Cooldown-Indicator unter dem Crosshair, aber dieser ist klein, positionsfest und bietet keine Konfigurierbarkeit. Dieses Modul ersetzt oder ergänzt ihn mit einem größeren, farbigen Balken an frei wählbarer Position. Nützlich bei schnellen PvP-Kämpfen, wo präzises Timing entscheidend ist.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `(screenWidth / 2) - 30` | `0–1920` | "X Position" | Horizontale Position des Balkens |
| `y_pos` | Int | `(screenHeight / 2) + 20` | `0–1080` | "Y Position" | Vertikale Position des Balkens |
| `bar_width` | Int | `60` | `20–200` | "Bar Width" | Breite des Cooldown-Balkens in Pixeln |
| `bar_height` | Int | `4` | `2–20` | "Bar Height" | Höhe des Cooldown-Balkens in Pixeln |
| `show_text` | Boolean | `false` | — | "Show Percentage" | Prozentzahl (0–100%) als Text über dem Balken anzeigen |
| `color_ready` | Int | `0xFF00FF00` | ARGB | "Ready Color" | Farbe wenn Cooldown vollständig (100%) — Standard: Grün |
| `color_charging` | Int | `0xFFFF5500` | ARGB | "Charging Color" | Farbe während Cooldown läuft — Standard: Orange |
| `show_border` | Boolean | `true` | — | "Show Border" | Rahmen um den Balken zeichnen |
| `only_in_combat` | Boolean | `false` | — | "Only in Combat" | Balken nur anzeigen wenn ein Entity im Fadenkreuz ist |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, RenderTickCounter counter)` — Cooldown-Fortschritt aus `player.getAttackCooldownProgress()` lesen und Balken rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
onRenderHud(ctx, counter):
  mc = MinecraftClient.getInstance()
  player = mc.player
  if player == null → return

  // only_in_combat: Balken nur anzeigen wenn Entity im Fokus
  if onlyInCombat.get() && !(mc.targetedEntity instanceof LivingEntity) → return

  // Cooldown-Fortschritt: 0.0 (gerade angegriffen) bis 1.0 (bereit)
  float progress = player.getAttackCooldownProgress(counter.getTickDelta(true))
  // counter.getTickDelta(true) interpoliert zwischen Ticks für flüssige Animation

  int x = xPos.get()
  int y = yPos.get()
  int w = barWidth.get()
  int h = barHeight.get()

  // Hintergrund (immer dunkel)
  ctx.fill(x, y, x + w, y + h, 0x88000000)

  // Gefüllter Anteil
  int filledWidth = (int)(progress * w)
  int barColor = (progress >= 1.0f) ? colorReady.get() : colorCharging.get()
  ctx.fill(x, y, x + filledWidth, y + h, barColor)

  // Rahmen
  if showBorder.get():
    ctx.drawStrokedRectangle(x, y, w, h, 0xFF000000)

  // Optionaler Prozenttext
  if showText.get():
    int percent = (int)(progress * 100)
    String label = percent + "%"
    int textX = x + w / 2 - mc.textRenderer.getWidth(label) / 2
    ctx.drawTextWithShadow(mc.textRenderer, Text.literal(label), textX, y - 10, 0xFFFFFFFF)
```

**Farbinterpolation (optional, fortgeschritten):**
Statt binärem Farbwechsel kann zwischen `colorCharging` und `colorReady` interpoliert werden:
```
r = lerp(chargeR, readyR, progress)
g = lerp(chargeG, readyG, progress)
b = lerp(chargeB, readyB, progress)
```
Einfacher Start: Binärer Wechsel bei `progress >= 1.0f`.

**Default-Position:** Da `xPos` und `yPos` von der Bildschirmgröße abhängen, müssen sie im Konstruktor oder in `onEnable()` berechnet werden:
```java
int screenW = MinecraftClient.getInstance().getWindow().getScaledWidth();
int screenH = MinecraftClient.getInstance().getWindow().getScaledHeight();
// Defaults setzen, bevor addSetting() aufgerufen wird
```
Alternativ: Feste Defaults verwenden (z.B. 200/200) und den Nutzer manuell positionieren lassen.

### Edge Cases

- **Keine Waffe in Hand:** `getAttackCooldownProgress()` gibt trotzdem einen Wert zurück (Fäuste haben auch Cooldown). Das Verhalten ist intentional — der Balken zeigt immer den aktuellen Angriffs-Cooldown.
- **Creative-Modus:** `getAttackCooldownProgress()` gibt in Creative immer `1.0f` zurück (kein Cooldown). Der Balken zeigt konstant die Ready-Farbe — das ist korrekt.
- **Spectator-Modus:** `player` existiert, aber der Spieler kann nicht angreifen. Guard mit `player.isSpectator()` → return, um den Balken zu verstecken.
- **HUD ausgeblendet (`F1`):** `mc.options.hudHidden` prüfen → wenn true, nicht rendern.
- **`only_in_combat` + kein Entity:** Früher return, kein Rendering.

## Translation Keys

```json
"noctra.attack_cooldown_indicator.name": "Attack Cooldown",
"noctra.attack_cooldown_indicator.description": "Shows your weapon attack cooldown as a customizable HUD bar.",
"noctra.attack_cooldown_indicator.x_pos": "X Position",
"noctra.attack_cooldown_indicator.y_pos": "Y Position",
"noctra.attack_cooldown_indicator.bar_width": "Bar Width",
"noctra.attack_cooldown_indicator.bar_height": "Bar Height",
"noctra.attack_cooldown_indicator.show_text": "Show Percentage",
"noctra.attack_cooldown_indicator.color_ready": "Ready Color",
"noctra.attack_cooldown_indicator.color_charging": "Charging Color",
"noctra.attack_cooldown_indicator.show_border": "Show Border",
"noctra.attack_cooldown_indicator.only_in_combat": "Only in Combat"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/attack_cooldown_indicator.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Ein horizontaler Balken in der unteren Bildhälfte, der von Orange (links, leer) nach Grün (rechts, voll) übergeht. Darüber ein stilisiertes Schwert-Symbol. Dunkler Hintergrund.
