# Held Item Info

**Status:** [x] DONE  
**Module ID:** `held_item_info`  
**Class:** `HeldItemInfoModule`  
**Category:** VISUAL  
**File:** `modules/impl/held_item_info/HeldItemInfoModule.java`

## Description

Zeigt detaillierte Informationen über das aktuell gehaltene Item als kompaktes HUD-Element an: Itemname, Stack-Größe, NBT-Tags (z. B. CustomName), und bei Werkzeugen den Angriffswert und Mining-Speed. Ergänzt das `item_info_hud`-Modul, fokussiert aber auf Spielwert-Daten statt visueller Darstellung.

## System Notes (Updated)

- Module Ordner: `modules/impl/held_item_info/HeldItemInfoModule.java`
- Package: `de.snenjih.noctra.modules.impl.held_item_info`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`
- `ColorSetting` für Textfarbe verwenden

## Settings

| ID | Type | Default | Label | Description |
|----|------|---------|-------|-------------|
| `show_stack_size` | BooleanSetting | `true` | "Show Stack Size" | Anzahl der Items im Stack anzeigen |
| `show_attack_damage` | BooleanSetting | `true` | "Show Attack Damage" | Angriffswert des Werkzeugs/der Waffe anzeigen |
| `show_mining_speed` | BooleanSetting | `false` | "Show Mining Speed" | Mining-Speed-Attribut anzeigen |
| `show_food_value` | BooleanSetting | `true` | "Show Food Value" | Hunger-Punkte bei Nahrungsmitteln anzeigen |
| `text_color` | ColorSetting | `0xFFFFFFFF` | "Text Color" | Textfarbe (ARGB) |
| `label_color` | ColorSetting | `0xFFAAAAAA` | "Label Color" | Farbe der Beschriftungen (ARGB) |
| `background` | BooleanSetting | `true` | "Background" | Halbtransparenten Hintergrund zeichnen |

## Implementation Notes

- `onRenderHud`: Item aus `mc.player.getMainHandStack()` lesen.
- Angriffswert: `stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS)` → `AttributeModifiersComponent` → nach `EntityAttributes.ATTACK_DAMAGE` filtern.
- Mining-Speed: `stack.get(DataComponentTypes.TOOL)` → `ToolComponent#getMiningSpeed(BlockState)` — einfach den default speed verwenden.
- Nahrungswert: `stack.get(DataComponentTypes.FOOD)` → `FoodComponent#nutrition()`.
- Stack-Größe: `stack.getCount()`.
- Kein Mixin nötig.
- Zeilen nur rendern wenn die jeweiligen Infos vorhanden und das Setting aktiv ist.

## Edge Cases

- Leere Hand (`stack.isEmpty()`): Nichts rendern.
- Item ohne Angriffsattribut (z. B. Block): `show_attack_damage` zeigt nichts.
- Nahrung mit komplexen Effekten: Nur Hunger-Punkte anzeigen, keine Effektliste.
- Offhand-Item: Nur Haupthand wird ausgewertet.

## Translation Keys

```json
"noctra.module.held_item_info.name": "Held Item Info",
"noctra.module.held_item_info.description": "Shows attack damage, stack size and food value of your held item."
```

## Icon

Path: `textures/gui/sprites/modules/held_item_info.png`  
32×32 PNG. Zeigt eine Hand die ein Schwert hält, mit einem kleinen Stat-Panel (Zahlen) daneben. Pixel-Art-Stil, helle Linien auf dunklem Hintergrund.
