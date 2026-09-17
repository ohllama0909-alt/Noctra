# Rain Disable

**ID:** `rain_disable`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/rain_disable/RainDisableModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.rain_disable`

## System Notes (Updated)

- Module Ordner: `modules/impl/rain_disable/RainDisableModule.java`
- Package: `de.snenjih.noctra.modules.impl.rain_disable`
- Implementiert HudElement: Nein (Mixin-basiert — WorldRenderer oder GameRenderer)
- Benötigt Mixin auf Regen-Rendering

## Description

Unterdrückt den visuellen Regen- und Schnee-Effekt vollständig auf Client-Seite, ohne das tatsächliche Wetter zu beeinflussen. Nützlich bei Performance-Einbrüchen durch Regen oder wenn der visuelle Effekt störend ist. Das Wetter existiert weiterhin auf dem Server (Nass werden, Crop-Wachstum, etc.) — nur das Rendering wird deaktiviert.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `also_disable_thunder` | Boolean | `true` | — | "Disable Thunder Effect" | Auch Gewitterhimmel (dunkle Wolken) visuell deaktivieren |

## Implementation

### Event Hooks

Keine direkten Lifecycle-Event-Hooks. Logik steckt im Mixin.

### Required Mixins

**Mixin 1: Wetter-Rendering deaktivieren**
- **Class:** `mixin/RainDisableMixin.java`
- **Target:** `net.minecraft.client.render.WorldRenderer`
- **Methode:** `renderWeather(LightmapTextureManager lightmap, float tickDelta, double cameraX, double cameraY, double cameraZ)`
- **Injection:** `@Inject(at = @At("HEAD"), method = "renderWeather(Lnet/minecraft/client/render/LightmapTextureManager;FDDD)V", cancellable = true)`
- **Zweck:** Wenn Modul aktiv: `ci.cancel()` → komplettes Rendering von Regen/Schnee-Partikeln wird übersprungen.

**Mixin 2: Regen-Gradient auf 0 setzen (optional, für Himmelsverdunklung)**
- **Class:** `mixin/RainGradientMixin.java`
- **Target:** `net.minecraft.client.world.ClientWorld`
- **Methode:** `getRainGradient(float delta)`
- **Injection:** `@Inject(at = @At("HEAD"), method = "getRainGradient(F)F", cancellable = true)`
- **Zweck:** Wenn `alsoDisableThunder` aktiv: `cir.setReturnValue(0f)` → verhindert Himmelsverdunklung und dunkle Wolken.

*Hinweis: Mixin 1 allein reicht um Regen-Partikel zu deaktivieren. Mixin 2 ist optional für den Himmel-Effekt. Beide Mixins lesen `isEnabled()` vom Modul.*

### Core Algorithm

```
// RainDisableMixin:
@Inject(at = @At("HEAD"), method = "renderWeather(...)", cancellable = true)
private void onRenderWeather(..., CallbackInfo ci):
    RainDisableModule module = NoctraMod.getRegistry().getModule("rain_disable")
    if (module != null && module.isEnabled()):
        ci.cancel()

// RainGradientMixin (optional, nur wenn alsoDisableThunder aktiv):
@Inject(at = @At("HEAD"), method = "getRainGradient(F)F", cancellable = true)
private void onGetRainGradient(float delta, CallbackInfoReturnable<Float> cir):
    RainDisableModule module = NoctraMod.getRegistry().getModule("rain_disable")
    if (module != null && module.isEnabled() && module.isAlsoDisableThunder()):
        cir.setReturnValue(0f)
```

### Edge Cases

- Spieler wird trotzdem nass: Das ist korrekt — serverseitiges Wetter wird nicht beeinflusst. Die "nass werden"-Mechanik (`isWet()`) basiert auf dem Wetter-Status, nicht dem Rendering-Gradient.
- Schnee: `renderWeather` rendert sowohl Regen als auch Schnee. Beide werden gleichzeitig deaktiviert. Kein separates Setting nötig.
- `getRainGradient` wird von anderen Systemen (z. B. Ambient-Sound, Fog-Berechnung) aufgerufen: Wenn Gradient = 0, können Regen-Sounds leiser werden. Das ist ein Nebeneffekt — falls unerwünscht, Mixin 2 weglassen.
- Blitze: Blitz-Entities (`LightningEntity`) werden separat gerendert und sind nicht Teil von `renderWeather`. Blitze werden weiterhin sichtbar sein, auch wenn Regen deaktiviert ist.
- Modul deaktiviert: Mixins lesen `isEnabled()` live, also sofort wieder Regen sichtbar.
- Singleplayer vs. Multiplayer: Kein Unterschied. Beide nutzen denselben `WorldRenderer`.
- `alsoDisableThunder`-Setting geändert während Regen: Wird sofort im nächsten Frame angewendet.
- Biom-abhängige Wettereffekte (Schnee in Kälte-Biomen): Ebenfalls über `renderWeather` gerendert, also mit deaktiviert.

## Translation Keys

```json
"noctra.rain_disable.name": "Rain Disable",
"noctra.rain_disable.description": "Disables rain and snow rendering client-side without affecting server weather.",
"noctra.rain_disable.also_disable_thunder": "Disable Thunder Effect"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/rain_disable.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Regentropfen mit einem roten Durchstreichungs-X oder Wolke mit X. Farbe: Blau (Regen) mit rotem X auf dunklem Hintergrund.
