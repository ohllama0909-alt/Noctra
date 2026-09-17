# Crosshair Customizer

**ID:** `crosshair_customizer`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/crosshair_customizer/CrosshairCustomizerModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.crosshair_customizer`

## System Notes (Updated)

- Module Ordner: `modules/impl/crosshair_customizer/CrosshairCustomizerModule.java`
- Package: `de.snenjih.noctra.modules.impl.crosshair_customizer`
- Implementiert HudElement: Ja — rendert eigenes Crosshair via `onRenderHud`
- `ColorSetting` für Crosshair-Farbe verwenden

## Description

Ersetzt das Vanilla-Fadenkreuz durch ein vollständig anpassbares eigenes Crosshair mit konfigurierbarer Farbe, Größe, Linienstärke und einem optionalen Mittelpunkt. Unterstützt auch einen Dot-Only-Modus. Gibt Spielern mehr Kontrolle über die Genauigkeit und Sichtbarkeit des Fadenkreuzes — besonders nützlich in hellem oder unruhigem Terrain.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `color` | Int | `0xFFFFFFFF` | ARGB Hex | "Color" | Farbe des Fadenkreuzes (ARGB) |
| `size` | Int | `8` | `2–30` | "Size" | Halbbreite des Fadenkreuzes in Pixeln (von Mitte bis Ende) |
| `thickness` | Int | `2` | `1–6` | "Thickness" | Linienstärke in Pixeln |
| `gap` | Int | `2` | `0–10` | "Gap" | Abstand vom Zentrum bis zum Beginn der Linie (0 = lückenlos) |
| `dot` | Boolean | `true` | — | "Center Dot" | Mittelpunkt-Dot anzeigen |
| `dot_size` | Int | `2` | `1–6` | "Dot Size" | Größe des Mittelpunkts in Pixeln |
| `outline` | Boolean | `false` | — | "Outline" | Schwarzen Umriss um das Crosshair |
| `outline_color` | Int | `0xFF000000` | ARGB Hex | "Outline Color" | Farbe des Umrisses |
| `dynamic_color` | Boolean | `false` | — | "Dynamic Color" | Farbe je nach anvisierten Entity-Typ ändern |
| `enemy_color` | Int | `0xFFFF5555` | ARGB Hex | "Enemy Color" | Farbe beim Anvisieren feindlicher Mobs |

## Implementation

### Event Hooks

- `onEnable()` — Mixin-Referenz aktivieren (statische INSTANCE setzen).
- `onDisable()` — Mixin-Referenz deaktivieren (INSTANCE auf null).
- `onRenderHud` wird **nicht** direkt genutzt — Crosshair-Rendering läuft über Mixin.

### Required Mixins

**Mixin — Vanilla-Crosshair unterdrücken und eigenes rendern:**
- **Class:** `mixin/CrosshairMixin.java`
- **Target:** `net.minecraft.client.gui.hud.InGameHud`
- **Method + Injection Point:**
  ```java
  @Inject(method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
          at = @At("HEAD"),
          cancellable = true)
  private void onRenderCrosshair(DrawContext ctx, RenderTickCounter counter, CallbackInfo ci)
  ```
- **Zweck:** Wenn `CrosshairCustomizerModule.INSTANCE != null && INSTANCE.isEnabled()`, Vanilla-Crosshair abbrechen (`ci.cancel()`) und eigenes Crosshair zeichnen.

### Core Algorithm

```
Feld in CrosshairCustomizerModule:
    public static CrosshairCustomizerModule INSTANCE = null

onEnable():  INSTANCE = this
onDisable(): INSTANCE = null

In CrosshairMixin.onRenderCrosshair(DrawContext ctx, RenderTickCounter counter, CallbackInfo ci):
    CrosshairCustomizerModule mod = CrosshairCustomizerModule.INSTANCE
    if (mod == null) return

    ci.cancel()   // Vanilla-Fadenkreuz unterdrücken

    MinecraftClient mc = MinecraftClient.getInstance()
    int cx = mc.getWindow().getScaledWidth()  / 2   // Bildschirmmitte X
    int cy = mc.getWindow().getScaledHeight() / 2   // Bildschirmmitte Y

    // Dynamic Color
    int color = mod.color.get()
    if (mod.dynamicColor.get() && mc.targetedEntity instanceof MobEntity mob):
        if (mob.isHostileTo(mc.player)):   // mob ist feindlich
            color = mod.enemyColor.get()

    int size      = mod.size.get()
    int thick     = mod.thickness.get()
    int gap       = mod.gap.get()
    int half      = thick / 2

    // Outline zuerst (falls aktiviert), dann das farbige Crosshair drüber
    if (mod.outline.get()):
        int oc = mod.outlineColor.get()
        int p = 1  // Outline-Breite
        // Horizontale Linie Outline
        drawLine(ctx, cx - size - p, cy - half - p, cx + size + p + 1, cy + half + p + 1, oc, gap + p)
        // Vertikale Linie Outline
        drawLine(ctx, cx - half - p, cy - size - p, cx + half + p + 1, cy + size + p + 1, oc, gap + p)

    // Horizontale Linien (links + rechts von gap)
    ctx.fill(cx - size, cy - half, cx - gap, cy + half, color)      // links
    ctx.fill(cx + gap,  cy - half, cx + size, cy + half, color)     // rechts

    // Vertikale Linien (oben + unten von gap)
    ctx.fill(cx - half, cy - size, cx + half, cy - gap, color)      // oben
    ctx.fill(cx - half, cy + gap,  cx + half, cy + size, color)     // unten

    // Mittelpunkt-Dot
    if (mod.dot.get()):
        int ds = mod.dotSize.get() / 2
        if (mod.outline.get()):
            ctx.fill(cx - ds - 1, cy - ds - 1, cx + ds + 1, cy + ds + 1, mod.outlineColor.get())
        ctx.fill(cx - ds, cy - ds, cx + ds, cy + ds, color)

Hilfsmethode drawLine(ctx, x1, y1, x2, y2, color, gap):
    // Horizontale Linie mit Lücke in der Mitte
    // Wird intern für Outline genutzt
    int cx = (x1 + x2) / 2
    ctx.fill(x1, y1, cx - gap, y2, color)
    ctx.fill(cx + gap, y1, x2, y2, color)
```

### Edge Cases

- `INSTANCE == null` in Mixin: Vanilla-Crosshair wird normal gerendert.
- Vanilla deaktiviert Crosshair in bestimmten Zuständen (z. B. Spectator, Karte in der Hand): Mixin läuft trotzdem. Prüfe `mc.options.getPerspective()` — in 3rd-Person kein Crosshair erwünscht.
- 3rd-Person-Kamera: `if (mc.options.getPerspective() != Perspective.FIRST_PERSON) return` in Mixin ergänzen.
- `gap >= size`: Crosshair wäre unsichtbar (Lücke größer als Arm). Kein Crash, aber keine sichtbare Linie.
- `thickness` gerade vs. ungerade: `half = thick / 2` arbeitet mit Ganzzahlen — ungerade Thickness führt zu 1px-Asymmetrie, akzeptabel.
- `dynamicColor` ohne angezieltes Entity (`mc.targetedEntity == null`): Standard-`color` wird verwendet.
- Mixin-Registrierung: `CrosshairMixin` in `noctra.mixins.json` eintragen.

## Translation Keys

```json
"noctra.crosshair_customizer.name": "Crosshair Customizer",
"noctra.crosshair_customizer.description": "Replaces the vanilla crosshair with a fully customizable one.",
"noctra.crosshair_customizer.color": "Color",
"noctra.crosshair_customizer.size": "Size",
"noctra.crosshair_customizer.thickness": "Thickness",
"noctra.crosshair_customizer.gap": "Gap",
"noctra.crosshair_customizer.dot": "Center Dot",
"noctra.crosshair_customizer.dot_size": "Dot Size",
"noctra.crosshair_customizer.outline": "Outline",
"noctra.crosshair_customizer.outline_color": "Outline Color",
"noctra.crosshair_customizer.dynamic_color": "Dynamic Color",
"noctra.crosshair_customizer.enemy_color": "Enemy Color"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/crosshair_customizer.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Fadenkreuz (+) in der Mitte, farbig (cyan oder weiß) auf dunklem Hintergrund. Das Crosshair hat einen kleinen Dot in der Mitte und erkennbare Lücken zwischen Zentrum und Armen.
