# Zoom

**ID:** `zoom`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/zoom/ZoomModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.zoom`

## System Notes (Updated)

- Module Ordner: `modules/impl/zoom/ZoomModule.java`
- Package: `de.snenjih.noctra.modules.impl.zoom`
- Implementiert HudElement: Nein (kein HUD-Overlay; FOV-Override via Mixin)
- Benötigt 2 Mixins: `ZoomFovMixin` (GameRenderer) und `ZoomScrollMixin` (Mouse)
- `KeybindSetting` für Zoom-Taste verwenden

## Description

Ermöglicht einen Optifine-ähnlichen Zoom per Tastendruck, bei dem das Sichtfeld (FOV) stark verringert wird, um weit entfernte Objekte zu vergrößern. Unterstützt Smooth-Zoom mit sanftem Ein- und Ausblenden sowie Scroll-Zoom zum Anpassen des Zoomfaktors. Nein-Mixin-Ansatz über ein dediziertes Mixin auf `GameRenderer`.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `zoom_key` | — | `C` | — | "Zoom Key" | Taste zum Aktivieren (als KeyBinding registrieren) |
| `zoom_fov` | Float | `15.0` | `1.0–60.0` | "Zoom FOV" | Ziel-FOV beim Zoomen (kleiner = stärker) |
| `smooth_zoom` | Boolean | `true` | — | "Smooth Zoom" | Sanfter Übergang statt abrupter Sprung |
| `smooth_speed` | Float | `10.0` | `1.0–30.0` | "Smooth Speed" | Geschwindigkeit der FOV-Interpolation |
| `scroll_sensitivity` | Float | `1.0` | `0.1–5.0` | "Scroll Sensitivity" | Wie stark Scrollen den Zoomfaktor ändert |
| `cinematic_cam` | Boolean | `false` | — | "Cinematic Camera" | Kamera-Smoothing aktivieren während Zoom |

## Implementation

### Event Hooks

- `onEnable()` — `KeyBinding` registrieren (via `KeyBindingHelper` aus Fabric API), Mixin-Referenz aktivieren.
- `onDisable()` — Zoom zurücksetzen, FOV auf ursprünglichen Wert.
- `onClientTick(MinecraftClient client)` — Tastenzustand prüfen, `zoomActive` setzen, Scroll-Input verarbeiten.

### Required Mixins

**Mixin 1 — FOV-Override:**
- **Class:** `mixin/ZoomFovMixin.java`
- **Target:** `net.minecraft.client.render.GameRenderer`
- **Method + Injection Point:**
  ```java
  @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D",
          at = @At("RETURN"),
          cancellable = true)
  private void onGetFov(Camera camera, float tickDelta, boolean changingFov,
                        CallbackInfoReturnable<Double> cir)
  ```
- **Zweck:** Gibt den interpolierten Zoom-FOV-Wert zurück anstelle des Vanilla-FOV, wenn der Zoom aktiv ist. Nutzt eine statische Referenz auf die aktive `ZoomModule`-Instanz.

**Mixin 2 — Scroll-Override (optional, für Scroll-Zoom):**
- **Class:** `mixin/ZoomScrollMixin.java`
- **Target:** `net.minecraft.client.Mouse`
- **Method + Injection Point:**
  ```java
  @Inject(method = "onMouseScroll(JDD)V",
          at = @At("HEAD"),
          cancellable = true)
  private void onScroll(long window, double horizontal, double vertical,
                        CallbackInfo ci)
  ```
- **Zweck:** Wenn Zoom aktiv, Scroll-Input abfangen und Zoom-Level anpassen statt Hotbar zu wechseln.

### Core Algorithm

```
Felder in ZoomModule:
    public static ZoomModule INSTANCE = null   // Für Mixin-Zugriff
    private boolean zoomActive = false
    private double  currentFov = 70.0          // Interpolierter FOV
    private double  targetFov  = 70.0
    private double  baseFov    = 70.0          // Ursprünglicher Spieler-FOV

Felder als KeyBinding:
    private KeyBinding zoomKey

onEnable():
    INSTANCE = this
    // KeyBinding mit fabric-key-binding-api-v1:
    zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.mandatory.zoom",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_C,
        "key.categories.mandatory"
    ))
    baseFov = MinecraftClient.getInstance().options.getFov().getValue().doubleValue()
    currentFov = baseFov

onDisable():
    INSTANCE = null
    zoomActive = false
    currentFov = baseFov
    targetFov  = baseFov

onClientTick(MinecraftClient client):
    if (client.player == null) return

    boolean shouldZoom = zoomKey.isPressed()

    if (shouldZoom != zoomActive):
        zoomActive = shouldZoom
        if (!zoomActive):
            targetFov = baseFov   // Zurückzoomen
        else:
            baseFov   = client.options.getFov().getValue().doubleValue()  // Aktuellen FOV speichern
            targetFov = zoomFov.get().doubleValue()

    // Smooth-Interpolation
    if (smoothZoom.get()):
        double speed = smoothSpeed.get() / 100.0   // Normalisieren
        currentFov += (targetFov - currentFov) * speed * 2.0
        if (Math.abs(currentFov - targetFov) < 0.01) currentFov = targetFov
    else:
        currentFov = targetFov

In ZoomFovMixin.onGetFov():
    ZoomModule mod = ZoomModule.INSTANCE
    if (mod == null || !mod.isEnabled()) return
    cir.setReturnValue(mod.getCurrentFov())   // currentFov überschreibt Vanilla-FOV

Scroll-Zoom in ZoomScrollMixin.onScroll():
    ZoomModule mod = ZoomModule.INSTANCE
    if (mod == null || !mod.isEnabled() || !mod.isZoomActive()) return
    double delta = vertical * -mod.scrollSensitivity.get()
    double newTarget = Math.clamp(mod.targetFov + delta, 1.0, 60.0)
    mod.setTargetFov(newTarget)
    ci.cancel()   // Hotbar-Wechsel verhindern
```

**Hinweis Kinematik-Kamera:** `cinematicCam` kann über `client.options.smoothCameraEnabled` gesetzt werden. Bei `onEnable(zoomActive)` auf `true` setzen, beim Deaktivieren den vorherigen Wert wiederherstellen.

### Edge Cases

- `INSTANCE = null` in Mixin: Immer `null`-Check vor Zugriff im Mixin.
- Spieler stirbt / Welt verlassen: `onLeaveWorld()` → `zoomActive = false`, `currentFov = baseFov`.
- `baseFov` änderbar via Optionen-Screen während Zoom aktiv: Nicht nachverfolgt — beim nächsten Zoom-Start wird neu gespeichert.
- Sehr niedriger `zoomFov` (<5°): Kann zu Rendering-Artefakten führen. Minimum in Setting auf `1.0` gesetzt, aber Spieler trägt Verantwortung.
- Scroll-Zoom: Wird von `ZoomScrollMixin` abgefangen, `ci.cancel()` verhindert Slot-Wechsel.
- `KeyBindingHelper` Import: `net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper`.
- `InputUtil` und `GLFW`: `net.minecraft.client.util.InputUtil`, `org.lwjgl.glfw.GLFW`.
- Mixin-Registrierung: Beide Mixins in `noctra.mixins.json` unter `"client"` eintragen.

## Translation Keys

```json
"noctra.zoom.name": "Zoom",
"noctra.zoom.description": "Optifine-like zoom activated by holding a key.",
"noctra.zoom.zoom_fov": "Zoom FOV",
"noctra.zoom.smooth_zoom": "Smooth Zoom",
"noctra.zoom.smooth_speed": "Smooth Speed",
"noctra.zoom.scroll_sensitivity": "Scroll Sensitivity",
"noctra.zoom.cinematic_cam": "Cinematic Camera",
"key.mandatory.zoom": "Zoom"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/zoom.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Lupe mit einem "+" oder Fernrohr-Silhouette in Pixel-Art. Blaue/weiße Farben, klares Symbol.
