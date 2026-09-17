# Hit Color

**ID:** `hit_color`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/hit_color/HitColorModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.hit_color`

## System Notes (Updated)

- Module Ordner: `modules/impl/hit_color/HitColorModule.java`
- Package: `de.snenjih.noctra.modules.impl.hit_color`
- Implementiert HudElement: Nein (Mixin-basiert)
- `ColorSetting` für Trefferfarbe verwenden

## Description

Ermöglicht das Anpassen der Farbe des Schmerz-Blitz-Effekts (der rote Bildschirmrand-Flash, wenn der Spieler Schaden erleidet). Kann die Farbe zu jeder beliebigen ARGB-Farbe ändern oder den Flash komplett deaktivieren. Nützlich für Spieler, die den roten Flash störend finden (z. B. bei Rotblindheit oder bei hoher visueller Empfindlichkeit).

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `hit_color` | Int | `0xFFFF0000` | ARGB Hex | "Hit Color" | Farbe des Hurt-Flash-Effekts (ARGB) |
| `opacity_factor` | Float | `1.0` | `0.0–2.0` | "Opacity Factor" | Multiplikator für die Stärke des Effekts (0 = unsichtbar) |
| `disable_flash` | Boolean | `false` | — | "Disable Flash" | Hurt-Flash komplett deaktivieren |

## Implementation

### Event Hooks

- `onEnable()` — Mixin-Referenz aktivieren (statische INSTANCE setzen).
- `onDisable()` — Mixin-Referenz deaktivieren (INSTANCE auf null).

### Required Mixins

**Mixin — Hurt-Overlay-Farbe überschreiben:**
- **Class:** `mixin/HitColorMixin.java`
- **Target:** `net.minecraft.client.gui.hud.InGameHud`
- **Method + Injection Point:**
  ```java
  @Inject(method = "renderVignetteOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/Entity;)V",
          at = @At("HEAD"),
          cancellable = true)
  private void onRenderVignette(DrawContext ctx, Entity entity, CallbackInfo ci)
  ```
- **Zweck:** Wenn Modul aktiv und `player.hurtTime > 0`, das Vanilla-Vignetten-Rendering überspringen (`ci.cancel()`) und einen eigenen gefärbten Overlay zeichnen.

**Alternative Methode (robuster):**

Falls `renderVignetteOverlay` keinen klar isolierten Hurt-Flash rendert, stattdessen:

- **Target:** `net.minecraft.client.gui.hud.InGameHud`
- **Method:** `renderOverlay(DrawContext ctx, Identifier texture, float opacity)` — diese Methode rendert sowohl den Feuer-Overlay als auch den Hurt-Flash.
- **Injection Point:** `@At("HEAD"), cancellable = true`
- **Bedingung im Mixin:** Nur eingreifen wenn `texture` dem Hurt-Flash-Identifier entspricht.

**Hinweis zum Yarn-Methodennamen:** In Yarn 1.21.11 muss die exakte Signatur von `renderVignetteOverlay` geprüft werden. Die Methode heißt in MC 1.21.x möglicherweise `renderVignetteOverlay` oder ist in `InGameHud.render()` inliniert. Alternativ den Hurt-Overlay via `BackgroundRenderer` hooking angehen. **Vor der Implementierung:** `./gradlew compileJava` mit einem minimalen Test-Inject durchführen, um die korrekte Methode zu identifizieren.

### Core Algorithm

```
Feld in HitColorModule:
    public static HitColorModule INSTANCE = null

onEnable():  INSTANCE = this
onDisable(): INSTANCE = null

In HitColorMixin.onRenderVignette(DrawContext ctx, Entity entity, CallbackInfo ci):
    HitColorModule mod = HitColorModule.INSTANCE
    if (mod == null) return

    MinecraftClient mc = MinecraftClient.getInstance()
    if (mc.player == null) return

    int hurtTime = mc.player.hurtTime   // Zählt runter von maxHurtTime (10) nach 0

    if (hurtTime <= 0):
        // Kein Hurt — Vanilla läuft normal (kein cancel)
        return

    ci.cancel()   // Vanilla-Hurt-Overlay unterdrücken

    if (mod.disableFlash.get()):
        return    // Komplett deaktiviert — nichts rendern

    // Intensität basierend auf hurtTime (10 = frischer Treffer, 1 = fast abgeklungen)
    float progress = hurtTime / 10.0f   // 0.0–1.0
    float opacity  = progress * mod.opacityFactor.get()
    opacity = Math.min(opacity, 1.0f)

    int baseColor = mod.hitColor.get()
    // Alpha aus baseColor extrahieren und mit opacity multiplizieren
    int baseAlpha = (baseColor >> 24) & 0xFF
    int finalAlpha = (int)(baseAlpha * opacity)
    int finalColor = (finalAlpha << 24) | (baseColor & 0x00FFFFFF)

    int w = ctx.getScaledWindowWidth()
    int h = ctx.getScaledWindowHeight()

    // Bildschirmdeckendes Rechteck zeichnen
    // Hinweis: RenderSystem.enableBlend() muss aktiv sein für ARGB-Transparenz
    RenderSystem.enableBlend()
    RenderSystem.defaultBlendFunc()
    ctx.fill(0, 0, w, h, finalColor)
    RenderSystem.disableBlend()
```

**Alternative für DrawContext-Breite/-Höhe:** In 1.21.11 kann `ctx.getScaledWindowWidth()` / `ctx.getScaledWindowHeight()` fehlen. Dann stattdessen `MinecraftClient.getInstance().getWindow().getScaledWidth()/Height()` verwenden.

### Edge Cases

- `INSTANCE == null` im Mixin: Vanilla-Rendering läuft normal weiter (kein `ci.cancel()`).
- `hurtTime <= 0`: Kein aktiver Treffer → Vanilla-Methode läuft normal (kein `ci.cancel()`). Dies ist wichtig damit der normale Vignetten-Effekt (Hunger, Dunkelheit) nicht unterdrückt wird.
- `opacityFactor == 0.0`: `disableFlash` nicht nötig — ergibt `finalAlpha = 0`, unsichtbar.
- `opacityFactor > 1.0`: Stärkt den Effekt über Vanilla hinaus — nur durch Setting-Clamp begrenzt (Max 2.0).
- `RenderSystem.enableBlend()` vergessen: Farbe wird opak gerendert, ignoriert Alpha-Kanal.
- Spieler ist in Wasser und verliert Hunger gleichzeitig: Beide Trigger rufen `renderVignetteOverlay` auf. Der `hurtTime`-Check trennt Hurt-Flash vom restlichen Overlay-Rendering.
- Mixin-Registrierung: `HitColorMixin` in `noctra.mixins.json` eintragen.

## Translation Keys

```json
"noctra.hit_color.name": "Hit Color",
"noctra.hit_color.description": "Customize or disable the red hurt flash when taking damage.",
"noctra.hit_color.hit_color": "Hit Color",
"noctra.hit_color.opacity_factor": "Opacity Factor",
"noctra.hit_color.disable_flash": "Disable Flash"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/hit_color.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Quadratischer Rahmen mit farbigem Rand (rot/orange Verlauf von außen nach innen), der den Bildschirm-Flash andeutet. Alternativ: Herz-Icon mit einem Blitz-Symbol.
