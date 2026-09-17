# Food Tooltip

**ID:** `food_tooltip`  
**Category:** UTILITY  
**Status:** [x] DONE  
**Class:** `modules/impl/FoodTooltipModule.java`

## Description

Erweitert den Item-Tooltip von Nahrungsmitteln um konkrete Werte: wiederhergestellte Hunger-Punkte, tatsächlich gewonnene Sättigung (berechnet auf Basis des aktuellen Saturationswerts) und eventuelle Trank-Effekte. Erspart das Nachschlagen im Wiki und hilft bei der Optimierung des Nahrungsmanagements.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `show_saturation` | Boolean | `true` | — | "Show Saturation" | Sättigungswert im Tooltip anzeigen |
| `show_effects` | Boolean | `true` | — | "Show Effects" | Trank-Effekte (z. B. Vergiftung) anzeigen |
| `show_effective_saturation` | Boolean | `false` | — | "Show Effective Saturation" | Tatsächliche Sättigungszunahme basierend auf aktuellem Hunger anzeigen |

## Implementation

### Event Hooks

Keine direkten Lifecycle-Event-Hooks. Die Logik steckt vollständig im Mixin.

### Required Mixins

- **Class:** `mixin/FoodTooltipMixin.java`
- **Target:** `net.minecraft.item.ItemStack`
- **Methode:** `getTooltip(Item.TooltipContext context, @Nullable TooltipType type)`
- **Injection:** `@Inject(at = @At("RETURN"), method = "getTooltip")`
- **Zweck:** Fügt nach dem Vanilla-Tooltip-Ende zusätzliche Zeilen mit Hunger/Sättigungsdaten ein, falls das Modul aktiv ist und `FoodComponent` vorhanden.

Alternativ kann das Mixin auf `Screen.renderTooltip()` injizieren, aber das ist komplizierter. Der direkte `getTooltip`-Hook ist sauberer.

### Core Algorithm

```
// Im Mixin: @Inject(at = @At("RETURN"), method = "getTooltip(...)")
// CallbackInfoReturnable<List<Text>> cir

FoodTooltipModule module = NoctraMod.getRegistry().getModule("food_tooltip");
if (module == null || !module.isEnabled()) return;

FoodComponent food = stack.get(DataComponentTypes.FOOD);
if (food == null) return;

List<Text> tooltip = cir.getReturnValue();

int nutrition = food.nutrition();   // Hunger-Punkte in halben Stücken
float saturationMod = food.saturation();

// Hunger-Punkte anzeigen (immer)
// "Hunger: +4 (🍗🍗)"  — oder einfach "+4 hunger"
tooltip.add(Text.literal("Hunger: +" + nutrition)
    .styled(s -> s.withColor(0xFFAA00)));  // Orange

if (showSaturation.get()):
    // Sättigungs-Modifier (vanilla Wert, nicht effektiv)
    tooltip.add(Text.literal("Saturation: " + String.format("%.1f", saturationMod))
        .styled(s -> s.withColor(0xFF55FF)));  // Lila

if (showEffectiveSaturation.get()):
    // Tatsächliche Sättigungszunahme = min(nutrition * saturationMod * 2, 20 - currentSaturation)
    MinecraftClient mc = MinecraftClient.getInstance();
    if (mc.player != null):
        float currentSaturation = mc.player.getHungerManager().getSaturationLevel()
        float effective = Math.min(nutrition * saturationMod * 2, 20f - currentSaturation)
        tooltip.add(Text.literal("Effective Saturation: +" + String.format("%.1f", effective))
            .styled(s -> s.withColor(0x55FFFF)));  // Cyan

if (showEffects.get()):
    List<FoodComponent.StatusEffectEntry> effects = food.effects()
    for (FoodComponent.StatusEffectEntry entry : effects):
        StatusEffectInstance effect = entry.effect()
        float chance = entry.probability()
        String effectName = effect.getEffectType().value().getName().getString()  // lokalisierter Name
        int durationTicks = effect.getDuration()
        int durationSec = durationTicks / 20
        String line = effectName + " " + toRoman(effect.getAmplifier() + 1)
                    + " (" + durationSec + "s)"
                    + (chance < 1.0f ? " " + (int)(chance * 100) + "%" : "")
        tooltip.add(Text.literal(line).styled(s -> s.withColor(0x55FF55)));  // Grün für positive, Rot für negative
```

### Edge Cases

- `FoodComponent` ist null (kein Nahrungsmittel): Guard-Clause, kein Tooltip.
- `mc.player == null` für effektive Sättigung (z. B. Hauptmenü, Tooltip in Kreativmenü): Guard-Clause; effektive Sättigung wird nicht berechnet.
- Tooltip-Liste ist null: Kann bei `getTooltip` mit `type == null` theoretisch vorkommen. Null-Check vor `.add()`.
- `food.effects()` leer: Keine Zeilen werden hinzugefügt, kein Fehler.
- Sättigung bereits voll (20.0): Effektive Sättigungszunahme zeigt `+0.0` — das ist informativ korrekt.
- Modul in `onEnable()`/`onDisable()` subscribed keine Events: Das Mixin liest den `isEnabled()`-Flag direkt.
- Mehrfaches Aufrufen von `getTooltip` (z. B. bei hover, Rendering): Tooltip wird jedes Mal neu gebaut. Kein State nötig.

## Translation Keys

```json
"noctra.food_tooltip.name": "Food Tooltip",
"noctra.food_tooltip.description": "Shows hunger, saturation, and effects in food item tooltips.",
"noctra.food_tooltip.show_saturation": "Show Saturation",
"noctra.food_tooltip.show_effects": "Show Effects",
"noctra.food_tooltip.show_effective_saturation": "Show Effective Saturation"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/food_tooltip.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Stilisierter Hunger-Bar (Hähnchenkeulen-Symbol) mit einem kleinen Info-"i" oder Lupe. Farbe: Orange/Gelb auf dunklem Hintergrund.
