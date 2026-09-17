# Anti Vignette

**ID:** `anti_vignette`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/anti_vignette/AntiVignetteModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.anti_vignette`

## System Notes (Updated)

- Module Ordner: `modules/impl/anti_vignette/AntiVignetteModule.java`
- Package: `de.snenjih.noctra.modules.impl.anti_vignette`
- Implementiert HudElement: Nein (Mixin auf `InGameHud.renderVignette` oder `renderOverlays`)
- Benötigt Mixin auf Vignette-Rendering in `InGameHud`

## Description

Deaktiviert die Rand-Vignette (dunkle Ränder auf dem Bildschirm) und optional das Unterwasser-Overlay. Die Vignette tritt bei niedrigen Lebenspunkten und generell permanent auf. Das Unterwasser-Overlay trübt die Sicht beim Tauchen. Beide Effekte beeinträchtigen die Sicht und können störend sein.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `disable_underwater` | Boolean | `false` | — | "Disable Underwater Overlay" | Blaues Unterwasser-Überlagerungs-Overlay ebenfalls deaktivieren |
| `disable_pumpkin` | Boolean | `false` | — | "Disable Pumpkin Overlay" | Kürbis-Helm-Overlay deaktivieren |

## Implementation

### Event Hooks

Keine direkten Lifecycle-Event-Hooks. Logik steckt im Mixin.

### Required Mixins

**Mixin 1: Vignette deaktivieren**
- **Class:** `mixin/AntiVignetteMixin.java`
- **Target:** `net.minecraft.client.gui.hud.InGameHud`
- **Methode:** `renderVignetteOverlay(DrawContext context, Entity entity)`
- **Injection:** `@Inject(at = @At("HEAD"), method = "renderVignetteOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/Entity;)V", cancellable = true)`
- **Zweck:** Wenn Modul aktiv: `ci.cancel()` → Vignette wird nicht gerendert.

**Mixin 2: Unterwasser-Overlay deaktivieren (optional)**
- **Class:** `mixin/AntiVignetteUnderwaterMixin.java`
- **Target:** `net.minecraft.client.gui.hud.InGameHud`
- **Methode:** `renderUnderwaterOverlay(DrawContext context)`
- **Injection:** `@Inject(at = @At("HEAD"), method = "renderUnderwaterOverlay(Lnet/minecraft/client/gui/DrawContext;)V", cancellable = true)`
- **Zweck:** Wenn Modul aktiv und `disableUnderwater` true: `ci.cancel()`.

**Mixin 3: Kürbis-Overlay deaktivieren (optional)**
- **Class:** `mixin/AntiVignettePumpkinMixin.java`
- **Target:** `net.minecraft.client.gui.hud.InGameHud`
- **Methode:** `renderOverlayTexture(DrawContext context, Identifier texture, float alpha)`
- **Injection:** `@Inject(at = @At("HEAD"), method = "renderOverlayTexture(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V", cancellable = true)`
- **Zweck:** Wenn Modul aktiv und `disablePumpkin` true und `texture` == `InGameHud.PUMPKIN_BLUR`: `ci.cancel()`.

*Hinweis: In 1.21.11 kann `renderVignetteOverlay` eine andere Signatur haben. Prüfe ob `Entity`-Parameter noch vorhanden ist oder ob er zu `Camera` geändert wurde. `InGameHud.renderVignetteOverlay` heißt in Yarn-Mappings möglicherweise anders — Decompile und Suche nach `vignette` in `InGameHud`.*

### Core Algorithm

```
// AntiVignetteMixin:
@Inject(at = @At("HEAD"), method = "renderVignetteOverlay(...)", cancellable = true)
private void onRenderVignette(..., CallbackInfo ci):
    AntiVignetteModule module = NoctraMod.getRegistry().getModule("anti_vignette")
    if (module != null && module.isEnabled()):
        ci.cancel()

// AntiVignetteUnderwaterMixin:
@Inject(at = @At("HEAD"), method = "renderUnderwaterOverlay(Lnet/minecraft/client/gui/DrawContext;)V", cancellable = true)
private void onRenderUnderwater(DrawContext context, CallbackInfo ci):
    AntiVignetteModule module = NoctraMod.getRegistry().getModule("anti_vignette")
    if (module != null && module.isEnabled() && module.isDisableUnderwater()):
        ci.cancel()

// AntiVignettePumpkinMixin:
@Inject(at = @At("HEAD"), method = "renderOverlayTexture(...)", cancellable = true)
private void onRenderOverlay(DrawContext context, Identifier texture, float alpha, CallbackInfo ci):
    AntiVignetteModule module = NoctraMod.getRegistry().getModule("anti_vignette")
    if (module != null && module.isEnabled() && module.isDisablePumpkin()):
        // Pumpkin-Blur Identifier: "minecraft:textures/misc/pumpkinblur.png"
        if (texture.toString().contains("pumpkin") || texture.toString().contains("pumpkinblur")):
            ci.cancel()
```

### Edge Cases

- Lebenspunkte-Vignette: In Vanilla wird die Vignette intensiver wenn der Spieler wenig HP hat. Das Deaktivieren via Mixin entfernt diesen visuellen Hinweis. Kein technisches Problem, aber potenziell gefährlich (übersieht niedrige HP). Kein Fix nötig — ist Intention.
- Blindness-Effekt: `renderVignetteOverlay` rendert auch den Blindness-Schwarzbildschirm teilweise. Wenn Vignette deaktiviert wird, kann Blindheit weniger stark erscheinen (guter Nebeneffekt oder schlechter je nach Perspektive).
- Unterwasser-Sicht: Das Unterwasser-Overlay ist ein separater Rendering-Pass von `renderUnderwaterOverlay`. Es ist nicht dasselbe wie der Blindness-Fog. Korrekt separiert.
- Kürbis-Helm: `renderOverlayTexture` wird mit verschiedenen Texturen aufgerufen (Kürbis, Powder Snow). Nur Kürbis wird gecancelt wenn `disablePumpkin` aktiv — Powder Snow bleibt aktiv.
- `renderOverlayTexture`-Methode in 1.21.11: Diese Methode existiert möglicherweise nicht direkt mit diesem Namen. Suche in `InGameHud` nach Kürbis-Blur-Rendering. Alternative: Mixin auf die Methode die `PUMPKIN_BLUR` Identifier verwendet.
- Modul deaktiviert: Alle Mixins lesen `isEnabled()` live → sofort wieder volle Vignette.
- Multiplayer / Singleplayer: Kein Unterschied, alles client-seitig.

## Translation Keys

```json
"noctra.anti_vignette.name": "Anti Vignette",
"noctra.anti_vignette.description": "Disables the screen edge vignette and optional overlays.",
"noctra.anti_vignette.disable_underwater": "Disable Underwater Overlay",
"noctra.anti_vignette.disable_pumpkin": "Disable Pumpkin Overlay"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/anti_vignette.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Quadrat mit klarem Innenbereich und durchgestrichenen dunklen Rändern. Farbe: Weißes Zentrum, rote Durchstreichungs-X auf dunklem Rand.
