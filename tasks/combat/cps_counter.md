# CPS Counter

**ID:** `cps_counter`  
**Category:** COMBAT  
**Status:** [x] DONE  
**Class:** `modules/impl/cps_counter/CpsCounterModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.cps_counter`

## System Notes (Updated)

- Module Ordner: `modules/impl/cps_counter/CpsCounterModule.java`
- Package: `de.snenjih.noctra.modules.impl.cps_counter`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Zeigt die Klicks pro Sekunde (CPS — Clicks Per Second) des Spielers auf dem HUD an. Verwendet ein gleitendes 1-Sekunden-Zeitfenster: Es werden nur Klicks der letzten 1000ms gezählt, sodass der Wert in Echtzeit aktuell bleibt. Zeigt linke Maustaste (Angriff), rechte Maustaste (Interaktion) oder beide getrennt an. Essentiell für PvP-Spieler, die ihre Klickrate überwachen möchten.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `5` | `0–1920` | "X Position" | Horizontale Position des HUD-Elements |
| `y_pos` | Int | `50` | `0–1080` | "Y Position" | Vertikale Position des HUD-Elements |
| `track_left` | Boolean | `true` | — | "Track Left Click" | Linke Maustaste zählen (Angriff-CPS) |
| `track_right` | Boolean | `false` | — | "Track Right Click" | Rechte Maustaste zählen (separate RCps-Anzeige) |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund zeichnen |
| `text_color` | Int | `0xFFFFFFFF` | ARGB | "Text Color" | Farbe des CPS-Textes |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, RenderTickCounter counter)` — CPS-Wert rendern.
- `onClientTick(MinecraftClient client)` — Alte Einträge aus dem Sliding Window bereinigen.

### Required Mixins

**Mixin ist erforderlich** für das Zählen von Mausklicks. Fabric bietet kein direktes Mouse-Click-Event für den In-Game-Zustand.

**Linke Maustaste (Angriff):**
- **Class:** `mixin/MouseButtonMixin.java`
- **Target:** `net.minecraft.client.Mouse`
- **Methode:** `onMouseButton(long window, int button, int action, int mods)` (private, wird via `@Inject` zugänglich gemacht)
- **Injection:** `@Inject(at = @At("HEAD"), method = "onMouseButton(JIII)V")`

Alternativ über `AttackEntityCallback.EVENT` für Angriffs-CPS (zählt nur tatsächliche Treffer, nicht alle Klicks):
```java
// Einfachere Option — zählt nur Klicks die ein Entity treffen:
AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
    recordClick(lClickTimestamps);
    return ActionResult.PASS;
});
```

Empfehlung: Mixin-Ansatz für echte Klick-Zählung (auch Fehlklicks ins Leere zählen); `AttackEntityCallback` für "Hit-CPS" (nur Treffer).

**Mixin-Implementierung:**
```java
@Mixin(Mouse.class)
public class MouseButtonMixin {
    @Inject(at = @At("HEAD"), method = "onMouseButton(JIII)V")
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        // action 1 = PRESS, 0 = RELEASE
        if (action != GLFW.GLFW_PRESS) return;
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null) return;
        
        CpsCounterModule module = ModuleRegistry.getInstance().getModule(CpsCounterModule.class);
        if (module == null || !module.isEnabled()) return;
        
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && module.trackLeft.get()) {
            module.recordLeftClick();
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && module.trackRight.get()) {
            module.recordRightClick();
        }
    }
}
```

### Core Algorithm — Sliding Window

```
// Felder in der Modul-Klasse:
// ArrayDeque ist effizienter als ArrayList für FIFO-Zugriff
public final ArrayDeque<Long> lClickTimestamps = new ArrayDeque<>()  // Zeitstempel in ms
public final ArrayDeque<Long> rClickTimestamps = new ArrayDeque<>()

public void recordLeftClick():
  lClickTimestamps.addLast(System.currentTimeMillis())

public void recordRightClick():
  rClickTimestamps.addLast(System.currentTimeMillis())

// Zugriff aus dem Mixin:
// ModuleRegistry benötigt eine getModule(Class<T>) Methode, oder
// CpsCounterModule als statisches Singleton halten (less ideal).
// Besser: Statische Hilfsmethode oder Event-Bus.

onClientTick(client):
  // Sliding Window bereinigen — Einträge älter als 1000ms entfernen
  long cutoff = System.currentTimeMillis() - 1000L
  while (!lClickTimestamps.isEmpty() && lClickTimestamps.peekFirst() < cutoff):
    lClickTimestamps.pollFirst()
  while (!rClickTimestamps.isEmpty() && rClickTimestamps.peekFirst() < cutoff):
    rClickTimestamps.pollFirst()

onRenderHud(ctx, counter):
  mc = MinecraftClient.getInstance()
  if mc.player == null → return
  if mc.options.hudHidden → return

  int x = xPos.get()
  int y = yPos.get()
  int lineY = y

  // Aktuellen CPS-Wert ablesen (Größe des Windows = Klicks in den letzten 1000ms)
  if trackLeft.get():
    int lcps = lClickTimestamps.size()
    String text = "CPS: " + lcps
    if background.get():
      ctx.fill(x - 2, lineY - 2, x + mc.textRenderer.getWidth(text) + 2, lineY + 10, 0x88000000)
    ctx.drawTextWithShadow(mc.textRenderer, Text.literal(text), x, lineY, textColor.get())
    lineY += 12

  if trackRight.get():
    int rcps = rClickTimestamps.size()
    String text = "RCPS: " + rcps
    if background.get():
      ctx.fill(x - 2, lineY - 2, x + mc.textRenderer.getWidth(text) + 2, lineY + 10, 0x88000000)
    ctx.drawTextWithShadow(mc.textRenderer, Text.literal(text), x, lineY, textColor.get())
```

**Sliding Window Erklärung:**
- Jeder Klick speichert `System.currentTimeMillis()` in einer `ArrayDeque<Long>`.
- Beim Rendering (oder im Tick): Alle Einträge vor `now - 1000ms` werden vom Kopf der Deque entfernt.
- Die Anzahl verbleibender Einträge = CPS im letzten 1-Sekunden-Fenster.
- `ArrayDeque.peekFirst()` / `pollFirst()` ist O(1) — kein Performance-Problem auch bei hohen CPS.
- Das Fenster ist "gleitend": Wenn jemand 20 mal in 1 Sekunde klickt, zeigt es "20 CPS". Eine Sekunde nach dem letzten Klick sinkt es auf "0".

**Mixin-Zugriff auf das Modul:**
Das Mixin muss auf die `CpsCounterModule`-Instanz zugreifen. Optionen:
1. **Statisches Instanz-Feld:** `public static CpsCounterModule INSTANCE` im Modul, gesetzt im Konstruktor. Einfach, aber nicht ideal für mehrere Instanzen (hier kein Problem).
2. **ModuleRegistry-Lookup:** `ModuleRegistry.getInstance().getModule("cps_counter")` — benötigt eine solche Methode in der Registry.
3. **Static Helper/Event:** Eine statische `onMouseClick(int button)` Methode im Modul, die vom Mixin aufgerufen wird.

Empfehlung: Option 3 (statische Methode), da sie kein Registry-API-Refactoring erfordert.

### Edge Cases

- **Screen offen (`mc.currentScreen != null`):** Im Mixin prüfen — Klicks im Inventar/Chat nicht zählen.
- **Spieler stirbt:** `mc.player.isDead()` — Klicks im Death-Screen nicht zählen.
- **Modul deaktiviert:** Im Mixin prüfen `module.isEnabled()`. In `onDisable()` die Deques leeren.
- **Sehr hohes CPS (Autoclicker):** `ArrayDeque` wächst proportional zu CPS × Fensterbreite. Bei 50 CPS = maximal 50 Einträge in der Deque. Kein Memory-Problem.
- **System-Zeit springt zurück (z.B. NTP):** Einträge mit Timestamp in der Zukunft würden ewig im Window bleiben. Praktisch irrelevant, aber als Notiz: `now - 1000L` könnte negativ werden falls `currentTimeMillis()` spring. `Math.max(0, cutoff)` als Guard.
- **`onClientTick` wird nicht bei jedem Frame aufgerufen:** Ticks sind 20/s, Frames können 60–300/s sein. Die Deque wird im Tick bereinigt, der Wert wird im Frame gelesen. Der angezeigte CPS-Wert kann daher zwischen Ticks minimal veraltet sein (max. 50ms). Akzeptabel.

## Translation Keys

```json
"noctra.cps_counter.name": "CPS Counter",
"noctra.cps_counter.description": "Shows your clicks per second using a 1-second sliding window.",
"noctra.cps_counter.x_pos": "X Position",
"noctra.cps_counter.y_pos": "Y Position",
"noctra.cps_counter.track_left": "Track Left Click",
"noctra.cps_counter.track_right": "Track Right Click",
"noctra.cps_counter.background": "Background",
"noctra.cps_counter.text_color": "Text Color"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/cps_counter.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Eine stilisierte Maus von oben, mit einem Pfeil oder Zähler-Symbol darüber. Alternativ: Drei nach rechts zunehmend große vertikale Balken (wie ein "schnelles" Balkendiagramm) mit einem kleinen Klick-Cursor-Symbol.
