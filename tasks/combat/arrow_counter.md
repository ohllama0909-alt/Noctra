# Arrow Counter

**ID:** `arrow_counter`  
**Category:** COMBAT  
**Status:** [x] DONE  
**Class:** `modules/impl/arrow_counter/ArrowCounterModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.arrow_counter`

## System Notes (Updated)

- Module Ordner: `modules/impl/arrow_counter/ArrowCounterModule.java`
- Package: `de.snenjih.noctra.modules.impl.arrow_counter`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Zeigt die Gesamtanzahl der Pfeile im Inventar auf dem HUD an, sofern der Spieler einen Bogen oder eine Armbrust in einer der beiden Hände hält. Das Element erscheint nur bei relevanter Waffe in der Hand und verschwindet automatisch, wenn keine Fernkampfwaffe aktiv ist. Hilft dabei, Pfeile nicht unbemerkt zu verbrauchen.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `hud_x` | Int | `-1` | `-1–1920` | "HUD X" | X-Position (-1 = auto: zentriert unter Hotbar) |
| `hud_y` | Int | `-1` | `-1–1080` | "HUD Y" | Y-Position (-1 = auto: unter der Hotbar) |
| `show_spectral` | Boolean | `true` | — | "Count Spectral Arrows" | Spektralpfeile mitzählen |
| `show_tipped` | Boolean | `true` | — | "Count Tipped Arrows" | Trank-Pfeile mitzählen |
| `always_show` | Boolean | `false` | — | "Always Show" | Immer anzeigen (nicht nur bei Bogen in Hand) |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, RenderTickCounter counter)` — Prüft ob Fernkampfwaffe in Hand, zählt Pfeile, rendert HUD.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
onRenderHud(DrawContext ctx, RenderTickCounter counter):
    MinecraftClient mc = MinecraftClient.getInstance()
    if (mc.player == null) return
    ClientPlayerEntity player = mc.player

    // Prüfen ob Bogen/Armbrust in einer Hand
    ItemStack mainHand = player.getMainHandStack()
    ItemStack offHand  = player.getOffHandStack()

    boolean hasRangedWeapon =
        mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem
     || offHand.getItem() instanceof BowItem  || offHand.getItem() instanceof CrossbowItem

    if (!hasRangedWeapon && !alwaysShow.get()) return

    // Pfeile im gesamten Inventar zählen
    int totalArrows = 0
    int regularArrows = 0
    int spectralArrows = 0
    int tippedArrows = 0

    PlayerInventory inv = player.getInventory()
    for (int i = 0; i < inv.size(); i++):
        ItemStack stack = inv.getStack(i)
        if (stack.isEmpty()) continue
        Item item = stack.getItem()
        if (item == Items.ARROW):
            regularArrows += stack.getCount()
        else if (item == Items.SPECTRAL_ARROW && showSpectral.get()):
            spectralArrows += stack.getCount()
        else if (item == Items.TIPPED_ARROW && showTipped.get()):
            tippedArrows += stack.getCount()

    totalArrows = regularArrows + spectralArrows + tippedArrows

    // Farbe basierend auf Anzahl
    int color
    if (totalArrows >= 64):
        color = 0x55FF55  // Grün: genug
    else if (totalArrows >= 16):
        color = 0xFFFF55  // Gelb: wenig
    else if (totalArrows > 0):
        color = 0xFF5555  // Rot: sehr wenig
    else:
        color = 0xFF5555  // Rot: keine Pfeile

    // Position berechnen
    TextRenderer tr = mc.textRenderer
    String text = totalArrows + " ✦"  // oder "⬥" als Pfeil-Symbol, oder einfach "x" + count
    // Alternativ ohne Emoji: text = totalArrows + " arrows"

    int screenW = mc.getWindow().getScaledWidth()
    int screenH = mc.getWindow().getScaledHeight()

    int x = hudX.get() >= 0 ? hudX.get() : (screenW / 2 - tr.getWidth(text) / 2)
    // Vanilla Hotbar ist bei screenH - 22 (Höhe 22px), Pfeil-Counter darunter bei screenH - 35
    int y = hudY.get() >= 0 ? hudY.get() : (screenH - 35)

    ctx.drawTextWithShadow(tr, Text.literal(text), x, y, color)

    // Detail-Anzeige wenn gemischte Pfeil-Typen vorhanden
    if (totalArrows > 0 && (spectralArrows > 0 || tippedArrows > 0)):
        String detail = ""
        if (regularArrows > 0) detail += regularArrows + "N "
        if (spectralArrows > 0) detail += spectralArrows + "S "
        if (tippedArrows > 0) detail += tippedArrows + "T"
        detail = detail.trim()
        ctx.drawTextWithShadow(tr, Text.literal(detail).styled(s -> s.withColor(0xAAAAAA)),
            x, y + 10, 0xAAAAAA)
```

### Edge Cases

- `player.getInventory().size()` umfasst alle Slots (Haupt- + Rüstung + Offhand). Kein Problem, da `Items.ARROW`-Checks nie auf Rüstungs-Slots matchen.
- Armbrust mit geladenem Pfeil: Eine geladene Armbrust hat den Pfeil als `Charged`-Component gespeichert, nicht im Inventar. Dieser Pfeil wird nicht mitgezählt. Das ist korrekt (er ist verbraucht, wenn die Armbrust entladen).
- Unendlichkeit (Infinity-Enchantment): Mit Infinity braucht man nur einen Pfeil im Inventar. Der Counter zeigt trotzdem die tatsächliche Anzahl — der Spieler weiß selbst ob er Infinity hat.
- `BowItem` und `CrossbowItem` in 1.21.11: Beide existieren als normale Klassen. `instanceof` funktioniert korrekt.
- Spectral/Tipped-Arrow-Check: `Items.TIPPED_ARROW` ist in 1.21.11 ein einzelner Item-Typ (mit Potion-Component). `instanceof`-Check auf Item-Klasse nicht nötig, direkter `==`-Vergleich reicht.
- Keine Pfeile: `totalArrows = 0` → Roter Text mit "0". Wenn `alwaysShow = false` und keine Waffe in Hand: nicht angezeigt.
- F1-Mode (HUD ausgeblendet): `onRenderHud` wird nicht aufgerufen. Kein Edge Case.
- Creative Mode: Spieler hat unendlich Pfeile (oder keine im Inventar). Counter zeigt Inventar-Anzahl korrekt — in Creative ist das 0, aber Infinity gilt implizit. Kein Fix nötig.

## Translation Keys

```json
"noctra.arrow_counter.name": "Arrow Counter",
"noctra.arrow_counter.description": "Shows arrow count on the HUD when holding a bow or crossbow.",
"noctra.arrow_counter.hud_x": "HUD X",
"noctra.arrow_counter.hud_y": "HUD Y",
"noctra.arrow_counter.show_spectral": "Count Spectral Arrows",
"noctra.arrow_counter.show_tipped": "Count Tipped Arrows",
"noctra.arrow_counter.always_show": "Always Show"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/arrow_counter.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Stilisierter Pfeil (diagonal nach oben rechts) mit einer Zahl darunter. Farbe: Grau/Weiß Pfeil auf dunklem Hintergrund, gelbe Zahl.
