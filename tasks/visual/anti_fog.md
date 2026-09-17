# Anti Fog

**ID:** `anti_fog`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/anti_fog/AntiFogModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.anti_fog`

## System Notes (Updated)

- Module Ordner: `modules/impl/anti_fog/AntiFogModule.java`
- Package: `de.snenjih.noctra.modules.impl.anti_fog`
- Implementiert HudElement: Nein (Mixin-basiert — BackgroundRenderer oder FogShape)
- Benötigt Mixin auf Fog-Rendering in `BackgroundRenderer`

## Description

Deaktiviert den Welt-Nebel vollständig oder erhöht die Fog-Sichtweite auf den konfigurierten Wert. Entfernt Fog in Lava, Wasser und bei Blindheit optional ebenfalls. Verbessert die Sicht auf lange Distanzen und entfernt den Blindness-Effekt visuell (ohne den Effekt selbst zu deaktivieren).

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `fog_start` | Float | `900.0` | `0.0–1000.0` | "Fog Start" | Distanz (Blöcke) ab der Fog beginnt |
| `fog_end` | Float | `1000.0` | `1.0–1000.0` | "Fog End" | Distanz (Blöcke) ab der Fog maximal dicht ist |
| `disable_lava_fog` | Boolean | `true` | — | "Disable Lava Fog" | Fog in Lava deaktivieren |
| `disable_water_fog` | Boolean | `false` | — | "Disable Water Fog" | Fog im Wasser deaktivieren |
| `disable_blindness_fog` | Boolean | `true` | — | "Disable Blindness Fog" | Blindness-Effekt-Fog deaktivieren |

## Implementation

### Event Hooks

Keine direkten Lifecycle-Event-Hooks. Logik steckt im Mixin.

### Required Mixins

- **Class:** `mixin/AntiFogMixin.java`
- **Target:** `net.minecraft.client.render.BackgroundRenderer`
- **Methode:** `applyFog(Camera camera, BackgroundRenderer.FogType fogType, Vec3d color, float viewDistance, boolean thickFog, float tickDelta)`
- **Injection:** `@Inject(at = @At("RETURN"), method = "applyFog(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/BackgroundRenderer$FogType;Lorg/joml/Vector3f;FZF)V")`
- **Zweck:** Nach dem Vanilla-Fog-Setup: Überschreibt `RenderSystem.setShaderFogStart()` und `RenderSystem.setShaderFogEnd()` mit den konfigurierten Werten.

*Hinweis: In 1.21.11 hat `BackgroundRenderer.applyFog()` möglicherweise eine andere Signatur. Prüfe Yarn-Mappings. Alternativ: Mixin auf `FogData`-Record oder `FogShape` Manipulation.*

*Alternative (robuster): Mixin auf `BackgroundRenderer.applyFog()` mit `@ModifyVariable` oder `@Redirect` um die `start`/`end` Werte direkt zu modifizieren bevor `RenderSystem` aufgerufen wird.*

### Core Algorithm

```
// Im Mixin @Inject(at = @At("RETURN")):
AntiFogModule module = NoctraMod.getRegistry().getModule("anti_fog")
if (module == null || !module.isEnabled()) return

// Kamera-Medium bestimmen
CameraSubmersionType submersion = camera.getSubmersionType()

boolean isLava = submersion == CameraSubmersionType.LAVA
boolean isWater = submersion == CameraSubmersionType.WATER
boolean hasBlinndess = false
if (camera.getFocusedEntity() instanceof LivingEntity le):
    hasBlindness = le.hasStatusEffect(StatusEffects.BLINDNESS)

// Entscheiden ob dieser Fog-Typ überschrieben werden soll
boolean shouldOverride = true
if (isLava && !module.getDisableLavaFog()) shouldOverride = false
if (isWater && !module.getDisableWaterFog()) shouldOverride = false
if (hasBlindness && fogType == BackgroundRenderer.FogType.FOG_SKY) shouldOverride = false
// Blindness-Fog ist FogType.FOG_SKY mit sehr kurzem End — nur bei Blindness und Flag
if (hasBlindness && module.getDisableBlindnessFog()) shouldOverride = true  // override Blindness

if (shouldOverride && !isLava && !isWater):
    RenderSystem.setShaderFogStart(module.getFogStart())
    RenderSystem.setShaderFogEnd(module.getFogEnd())
else if (isLava && module.getDisableLavaFog()):
    RenderSystem.setShaderFogStart(990f)
    RenderSystem.setShaderFogEnd(1000f)
else if (isWater && module.getDisableWaterFog()):
    RenderSystem.setShaderFogStart(990f)
    RenderSystem.setShaderFogEnd(1000f)
else if (hasBlindness && module.getDisableBlindnessFog()):
    RenderSystem.setShaderFogStart(900f)
    RenderSystem.setShaderFogEnd(1000f)
```

### Edge Cases

- `BackgroundRenderer.applyFog()` Signatur in 1.21.11: Yarn-Mappings sorgfältig prüfen. Die Methode kann umbenannt sein. Falls nötig, decompile mit `./gradlew genSources` und suche nach `setShaderFogStart` Aufruf in `BackgroundRenderer`.
- `RenderSystem`-Aufrufe außerhalb des Render-Threads: Diese Methode wird immer auf dem Render-Thread aufgerufen. Kein Problem.
- Fog im Nether: Nether hat starken Fog über `DimensionEffects`. `BackgroundRenderer.applyFog` wird trotzdem aufgerufen. Override greift.
- Powder Snow (FogType ändern sich): Powder Snow hat eigenen Fog-Typ (`CameraSubmersionType.POWDER_SNOW`). Nicht explizit behandelt — Fog-Override greift trotzdem, da kein spezifischer Check.
- `fog_end` < `fog_start`: Validerung beim Setting notwendig oder Guard im Mixin: `if (end <= start) end = start + 1f`.
- Spieler mit Render-Distance 2: Fog-Override kann dazu führen, dass ungeladene Chunks sichtbar werden (schwarze Lücken). Kein Fix nötig — das ist erwünschtes Verhalten mit Anti-Fog.
- Optifine/Iris (andere Mods): Kann mit Shader-Mods kollidieren. Kein Fix in dieser Spec.

## Translation Keys

```json
"noctra.anti_fog.name": "Anti Fog",
"noctra.anti_fog.description": "Reduces or removes world fog to increase render visibility.",
"noctra.anti_fog.fog_start": "Fog Start",
"noctra.anti_fog.fog_end": "Fog End",
"noctra.anti_fog.disable_lava_fog": "Disable Lava Fog",
"noctra.anti_fog.disable_water_fog": "Disable Water Fog",
"noctra.anti_fog.disable_blindness_fog": "Disable Blindness Fog"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/anti_fog.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Auge mit klarer Sicht (kein Dunst) oder Landschaft mit durchgestrichenem Nebel. Farbe: Hellblau/Weiß auf dunklem Hintergrund, X oder Durchstreichung in Rot.
