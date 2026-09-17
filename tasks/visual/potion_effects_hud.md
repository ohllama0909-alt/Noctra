# Potion Effects HUD

**ID:** `potion_effects_hud`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/potion_effects_hud/PotionEffectsHudModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.potion_effects_hud`

## System Notes (Updated)

- Module Ordner: `modules/impl/potion_effects_hud/PotionEffectsHudModule.java`
- Package: `de.snenjih.noctra.modules.impl.potion_effects_hud`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Ersetzt oder ergänzt die Standard-Tränke-Anzeige von Minecraft durch ein kompaktes, klar lesbares HUD-Element, das alle aktiven Statuseffekte mit Namen, Stärke-Level und verbleibender Zeit in Sekunden auflistet. Besonders nützlich im Kampf oder in Speedruns, wo schnell relevante Buff-Zeiten abgelesen werden müssen.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `2` | `0–1920` | "X Position" | Horizontale Position des HUD-Elements |
| `y_pos` | Int | `130` | `0–1080` | "Y Position" | Vertikale Position (oberstes Element) |
| `show_icons` | Boolean | `true` | — | "Show Icons" | Tränke-Sprites anzeigen |
| `show_amplifier` | Boolean | `true` | — | "Show Amplifier" | Stärke-Level (II, III…) anzeigen |
| `show_time` | Boolean | `true` | — | "Show Time" | Verbleibende Zeit in Sekunden anzeigen |
| `time_warn_seconds` | Int | `10` | `1–60` | "Time Warning (s)" | Unter diesem Wert Zeitanzeige rot färben |
| `text_color` | Int | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `warn_color` | Int | `0xFFFF5555` | ARGB Hex | "Warn Color" | Farbe wenn Zeit fast abgelaufen |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund zeichnen |
| `row_height` | Int | `13` | `10–20` | "Row Height" | Zeilenabstand in Pixeln |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Aktive Effekte auslesen, sortieren und rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
onRenderHud(DrawContext ctx, float tickDelta):
    if (mc.player == null) return

    Collection<StatusEffectInstance> effects = mc.player.getStatusEffects()
    if (effects.isEmpty()) return

    // Effekte sortieren: zuerst positive, dann negative; innerhalb jeder Gruppe nach Zeit absteigend
    List<StatusEffectInstance> sorted = effects.stream()
        .sorted(Comparator
            .comparing((StatusEffectInstance e) -> e.getEffectType().value().isBeneficial() ? 0 : 1)
            .thenComparingInt(e -> -e.getDuration()))
        .toList()

    int x = xPos.get()
    int y = yPos.get()

    // Hintergrund berechnen (optional)
    if (background.get()):
        int bgW = 140  // geschätzte maximale Breite
        int bgH = sorted.size() * rowHeight.get() + 4
        ctx.fill(x - 2, y - 2, x + bgW, y + bgH, 0x88000000)

    for (int i = 0; i < sorted.size(); i++):
        StatusEffectInstance effect = sorted.get(i)
        int rowY = y + i * rowHeight.get()

        // Icon (optional) — 18x18 Sprite
        if (showIcons.get()):
            Identifier spriteId = effect.getEffectType().value().getSpriteId()
            // In 1.21.11: StatusEffect#getSpriteId() gibt Identifier zurück
            ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, spriteId, x, rowY, 14, 14)
            iconOffset = 17
        else:
            iconOffset = 0

        // Effektname + Amplifier
        String name = Text.translatable(effect.getEffectType().value().getTranslationKey()).getString()
        if (showAmplifier.get() && effect.getAmplifier() > 0):
            name += " " + toRoman(effect.getAmplifier() + 1)  // Amplifier 0 = Level I, nicht anzeigen

        // Zeit berechnen
        int durationTicks = effect.getDuration()
        boolean infinite = durationTicks == Integer.MAX_VALUE || durationTicks < 0
        String timeStr = ""
        int timeColor = textColor.get()
        if (showTime.get() && !infinite):
            int seconds = durationTicks / 20
            int mins = seconds / 60
            int secs = seconds % 60
            timeStr = mins > 0 ? String.format(" %d:%02d", mins, secs) : String.format(" %ds", seconds)
            if (seconds <= timeWarnSeconds.get()):
                timeColor = warnColor.get()

        // Text rendern
        String line = name + (infinite ? "" : timeStr)
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(line), x + iconOffset, rowY + 2, timeColor)

Hilfsmethode toRoman(int n):
    // Nur für Zahlen 2-10 relevant; einfache Lookup-Map genügt
    String[] roman = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"}
    return n < roman.length ? roman[n] : String.valueOf(n)
```

**Hinweis zu `getSpriteId()`:** In Yarn 1.21.11 heißt die Methode `StatusEffect#getSpriteId()` und gibt den `Identifier` des Sprites zurück, der im GUI-Sprite-Atlas liegt. Testen mit einem bekannten Effekt (Speed, Regeneration).

### Edge Cases

- `effects.isEmpty()`: Frühzeitig `return`, kein Rendering.
- `mc.player == null`: Guard-Clause.
- Permanente Effekte (`durationTicks == Integer.MAX_VALUE`): Keine Zeitanzeige, kein Warnblinking.
- Sehr viele Effekte (>10): Können über den Bildschirmrand gehen. Kein automatisches Clipping — Spieler kann Y-Position anpassen.
- Amplifier 0 (Level I): Nicht als "I" anzeigen, da der Spieler "Speed" statt "Speed I" erwartet.
- Negierte Effekte (z. B. durch Milch gelöscht): Verschwinden aus `getStatusEffects()` automatisch.

## Translation Keys

```json
"noctra.potion_effects_hud.name": "Potion Effects HUD",
"noctra.potion_effects_hud.description": "Displays active status effects with name, level, and remaining time.",
"noctra.potion_effects_hud.x_pos": "X Position",
"noctra.potion_effects_hud.y_pos": "Y Position",
"noctra.potion_effects_hud.show_icons": "Show Icons",
"noctra.potion_effects_hud.show_amplifier": "Show Amplifier",
"noctra.potion_effects_hud.show_time": "Show Time",
"noctra.potion_effects_hud.time_warn_seconds": "Time Warning (s)",
"noctra.potion_effects_hud.text_color": "Text Color",
"noctra.potion_effects_hud.warn_color": "Warn Color",
"noctra.potion_effects_hud.background": "Background",
"noctra.potion_effects_hud.row_height": "Row Height"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/potion_effects_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Zwei oder drei Tränkeflaschen-Icons nebeneinander, eine davon mit einer kleinen Uhr/Timer-Anzeige darunter. Farben: violett/lila/türkis wie die Vanilla-Tränke.
