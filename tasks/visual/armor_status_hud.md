# Armor Status HUD

**ID:** `armor_status_hud`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/armor_status_hud/ArmorStatusHudModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.armor_status_hud`

## System Notes (Updated)

- Module Ordner: `modules/impl/armor_status_hud/ArmorStatusHudModule.java`
- Package: `de.snenjih.noctra.modules.impl.armor_status_hud`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Zeigt alle vier Rüstungsteile (Helm, Brustplatte, Hose, Schuhe) als HUD-Element mit je einem Durability-Balken und optionalem Item-Icon an. Farbliche Warnung wenn ein Rüstungsteil kurz vor dem Bruch steht. Hilft dabei, Rüstungen rechtzeitig zu reparieren und teure Items nicht zu verlieren.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `2` | `0–1920` | "X Position" | Horizontale Position des HUD-Elements |
| `y_pos` | Int | `60` | `0–1080` | "Y Position" | Vertikale Position (oberste Rüstung) |
| `show_icons` | Boolean | `true` | — | "Show Icons" | Item-Sprites neben den Balken anzeigen |
| `bar_width` | Int | `60` | `20–200` | "Bar Width" | Breite der Durability-Balken in Pixeln |
| `warn_threshold` | Float | `0.2` | `0.05–0.5` | "Warn Threshold" | Anteil verbleibender Durability für Warnfarbe |
| `color_full` | Int | `0xFF55FF55` | ARGB Hex | "Color Full" | Balkenfarbe bei voller Durability (ARGB) |
| `color_warn` | Int | `0xFFFF5555` | ARGB Hex | "Color Warn" | Balkenfarbe bei niedrigem Durability (ARGB) |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund zeichnen |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, float tickDelta)` — Alle vier Rüstungsslots auslesen und als Balkensatz rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
Konstanten:
    EquipmentSlot[] SLOTS = {HEAD, CHEST, LEGS, FEET}
    int ROW_HEIGHT = 14    // Pixel pro Rüstungsteil
    int ICON_SIZE  = 12    // Sprite-Größe

onRenderHud(DrawContext ctx, float tickDelta):
    if (mc.player == null) return

    int x = xPos.get()
    int y = yPos.get()

    for (int i = 0; i < 4; i++):
        ItemStack stack = mc.player.getEquippedStack(SLOTS[i])
        int rowY = y + i * ROW_HEIGHT

        if (stack.isEmpty()):
            // Grau leeren Balken zeichnen
            ctx.fill(iconOffset + x, rowY + 2, iconOffset + x + barWidth, rowY + 9, 0xFF444444)
            continue

        // Icon zeichnen (optional)
        if (showIcons.get()):
            ctx.drawItem(stack, x, rowY)
            // drawItem zeichnet 16x16 Item-Sprite; kein GuiTexture-Aufruf nötig
            iconOffset = 18  // Balken nach rechts verschieben

        // Durability berechnen
        int maxDmg  = stack.getMaxDamage()
        if (maxDmg <= 0):  // unzerstörbares Item
            ctx.fill(iconOffset + x, rowY + 2, iconOffset + x + barWidth, rowY + 9, 0xFF888888)
            continue
        float fraction = 1.0f - (float) stack.getDamage() / maxDmg

        // Balkenfarbe
        int color = fraction <= warnThreshold.get() ? colorWarn.get() : colorFull.get()

        // Hintergrundbalken
        ctx.fill(iconOffset + x, rowY + 2, iconOffset + x + barWidth, rowY + 9, 0xFF222222)
        // Füllbalken
        int fillWidth = Math.round(fraction * barWidth.get())
        ctx.fill(iconOffset + x, rowY + 2, iconOffset + x + fillWidth, rowY + 9, color)

    // Optionaler Hintergrund hinter allem
    if (background.get()):
        ctx.fill(x - 2, y - 2, x + totalWidth + 2, y + 4 * ROW_HEIGHT + 2, 0x88000000)
```

**Hinweis zu `drawItem`:** `DrawContext.drawItem(ItemStack, int x, int y)` rendert das Item-Icon als 16×16 Sprite inklusive Enchantment-Glitzer. Verfügbar über `DrawContext` direkt — kein zusätzlicher Import nötig.

### Edge Cases

- `mc.player == null`: Guard-Clause am Anfang.
- `ItemStack.isEmpty()`: Leerer Balken in Grau (kein Item angelegt).
- `getMaxDamage() == 0`: Item ist unzerstörbar (z. B. Netherit-Kopf mit bestimmten Components) → Balken zeigt grau-neutral an, keine Warnung.
- Rüstung mit maximaler Durability (nagelneu): `fraction == 1.0`, Balken voll grün.
- Rüstung komplett kaputt (damage == maxDamage): `fraction == 0`, Balken leer + Warnfarbe.
- Breite und Position können durch Settings außerhalb des Bildschirms zeigen — der Spieler ist selbst verantwortlich.

## Translation Keys

```json
"noctra.armor_status_hud.name": "Armor Status HUD",
"noctra.armor_status_hud.description": "Shows durability bars for all equipped armor pieces.",
"noctra.armor_status_hud.x_pos": "X Position",
"noctra.armor_status_hud.y_pos": "Y Position",
"noctra.armor_status_hud.show_icons": "Show Icons",
"noctra.armor_status_hud.bar_width": "Bar Width",
"noctra.armor_status_hud.warn_threshold": "Warn Threshold",
"noctra.armor_status_hud.color_full": "Color Full",
"noctra.armor_status_hud.color_warn": "Color Warn",
"noctra.armor_status_hud.background": "Background"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/armor_status_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Vier horizontale Balken untereinander (Helm-Icon + Balken, Brust-Icon + Balken etc.), der unterste Balken rot als Warnung. Stil: Pixel-Art passend zum Minecraft-Inventar.
