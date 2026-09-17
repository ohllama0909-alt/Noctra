# Item Info HUD

**Status:** [x] DONE  
**Module ID:** `item_info_hud`  
**Class:** `ItemInfoHudModule`  
**Category:** VISUAL  
**File:** `modules/impl/item_info_hud/ItemInfoHudModule.java`

## Description

Zeigt das aktuell gehaltene Item als großes HUD-Element mit Name, Durability-Balken und Enchantment-Liste an. Erscheint kurz nach dem Wechseln des Slots (ähnlich wie die Vanilla-Item-Name-Anzeige) oder dauerhaft, je nach Konfiguration. Praktisch beim schnellen Überprüfen von Werkzeug-Durability und Verzauberungen ohne das Inventar zu öffnen.

## System Notes (Updated)

- Module Ordner: `modules/impl/item_info_hud/ItemInfoHudModule.java`
- Package: `de.snenjih.noctra.modules.impl.item_info_hud`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`
- Neue Setting-Typen: `ColorSetting` für Textfarbe, `BooleanSetting` für Anzeigeoptionen

## Settings

| ID | Type | Default | Label | Description |
|----|------|---------|-------|-------------|
| `show_name` | BooleanSetting | `true` | "Show Name" | Item-Namen anzeigen |
| `show_durability` | BooleanSetting | `true` | "Show Durability" | Durability-Balken und Zahlenwert anzeigen |
| `show_enchantments` | BooleanSetting | `true` | "Show Enchantments" | Verzauberungsliste anzeigen |
| `always_show` | BooleanSetting | `false` | "Always Show" | Immer anzeigen, nicht nur nach Slot-Wechsel |
| `display_seconds` | IntSetting | `3` | `1–10` | "Display Duration (s)" | Wie lange das HUD nach Slot-Wechsel sichtbar bleibt |
| `text_color` | ColorSetting | `0xFFFFFFFF` | "Text Color" | Haupttextfarbe (ARGB) |
| `bar_color_full` | ColorSetting | `0xFF55FF55` | "Bar Color Full" | Durability-Balken bei hoher Durability |
| `bar_color_low` | ColorSetting | `0xFFFF5555` | "Bar Color Low" | Durability-Balken bei niedriger Durability |
| `background` | BooleanSetting | `true` | "Background" | Halbtransparenten Hintergrund zeichnen |

## Implementation Notes

- `onClientTick`: Slot-Wechsel erkennen (`lastSelectedSlot != player.getInventory().getSelectedSlot()`); bei Wechsel `displayTimer` auf `displaySeconds * 20` setzen und herunterzählen.
- `renderHud`: Wenn `alwaysShow` oder `displayTimer > 0`, Item-Infos rendern.
- Item-Name: `stack.getName().getString()` — bei benutzerdefinierten Namen (Amboss) erscheint der custom Name.
- Durability: `stack.isDamageable()` prüfen; `fraction = 1.0 - (float)stack.getDamage() / stack.getMaxDamage()`.
- Enchantments: `stack.getEnchantments()` → `EnchantmentHelper` → Namen via `Enchantment#description()`.
- Kein Mixin nötig.

## Edge Cases

- Item ohne Durability (`!stack.isDamageable()`): Kein Durability-Balken rendern.
- Item ohne Enchantments: Keine Enchantment-Zeilen rendern.
- Stack ist leer (`stack.isEmpty()`): Nichts rendern.
- Spieler in Menü (Inventory offen): HUD trotzdem rendern, falls `alwaysShow`.
- Timer läuft ab: `displayTimer <= 0` → nur wenn `alwaysShow` weiters rendern.

## Translation Keys

```json
"noctra.module.item_info_hud.name": "Item Info HUD",
"noctra.module.item_info_hud.description": "Shows held item name, durability, and enchantments as a HUD overlay."
```

## Icon

Path: `textures/gui/sprites/modules/item_info_hud.png`  
32×32 PNG. Zeigt ein stilisiertes Schwert/Werkzeug mit einem kleinen Infopanel daneben (Name + Balken). Pixel-Art-Stil.
