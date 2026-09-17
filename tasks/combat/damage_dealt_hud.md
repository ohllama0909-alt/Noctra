# Damage Dealt HUD

**ID:** `damage_dealt_hud`  
**Category:** COMBAT  
**Status:** [x] DONE  
**Class:** `modules/impl/damage_dealt_hud/DamageDealtHudModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.damage_dealt_hud`

## Description

Zeigt den zuletzt ausgeteilten Schaden auf dem HUD an. Anders als flüchtige Schadensanzeigen die kurz aufblinken und verschwinden, hält dieses Modul den letzten Schadenswert für eine konfigurierbare Zeit sichtbar. Optional wird auch der Gesamt-Schaden der aktuellen Session und ein gleitender Durchschnitt angezeigt. Nützlich für PvP-Training und DPS-Analyse.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `bg_color` | Color | `0xCC0D1B2A` | ARGB Hex | "Background Color" | Farbe des Hintergrundrechtecks |
| `border_color` | Color | `0xFF1E3A5F` | ARGB Hex | "Border Color" | Rahmenfarbe |
| `text_color` | Color | `0xFFFFFFFF` | ARGB Hex | "Text Color" | Standardtextfarbe |
| `text_shadow` | Boolean | `true` | — | "Text Shadow" | Textschatten aktivieren |
| `text_scale` | Float | `1.0` | `0.5–2.0` | "Text Scale" | Textskalierung |
| `show_background` | Boolean | `true` | — | "Show Background" | Hintergrund und Rahmen zeichnen |
| `display_time` | Int | `3` | `1–30` | "Display Time (s)" | Sekunden wie lange letzter Schaden sichtbar bleibt |
| `show_total` | Boolean | `false` | — | "Show Session Total" | Gesamt-Schaden der Session anzeigen |
| `show_max_hit` | Boolean | `false` | — | "Show Max Hit" | Höchster Einzeltreffer der Session |
| `show_dps` | Boolean | `false` | — | "Show DPS" | Schaden pro Sekunde (Durchschnitt letzte 5s) |
| `fade_out` | Boolean | `true` | — | "Fade Out" | Anzeige blendet sanft aus bevor sie verschwindet |
| `damage_color` | Color | `0xFFFF5555` | ARGB Hex | "Damage Color" | Farbe des Schadenswertes |
| `crit_color` | Color | `0xFFFFAA00` | ARGB Hex | "Crit Hit Color" | Farbe bei kritischem Treffer |
| `decimal_places` | Int | `1` | `0–2` | "Decimal Places" | Nachkommastellen des Schadenswertes |
| `hide_when_zero` | Boolean | `true` | — | "Hide When Zero" | Verstecken wenn seit display_time kein Treffer |

## Implementation

### Event Hooks

- `onClientTick(MinecraftClient client)` — Schaden aus Entity-Daten ableiten, Timer aktualisieren.
- `onRenderHud(DrawContext ctx, float tickDelta)` — Letzten Schaden anzeigen.

### Required Mixins

**Mixin erforderlich** um den tatsächlich verursachten Schaden abzufangen:

```java
// mixin/LivingEntityDamageMixin.java
// Target: LivingEntity.damage() oder ClientWorld-seitig:
// Besser: EntityDamageCallback aus Fabric Events (falls verfügbar)
// In 1.21.x: ServerEntityCombatEvents existiert nicht client-seitig.
// Empfohlener Ansatz: Health-Delta-Tracking.
```

**Alternativer Ansatz ohne Mixin (Health-Delta):**

In `onClientTick()`: Zielentität aus `mc.targetedEntity` holen, letzte bekannte Health speichern, Differenz = Schaden.

```java
// Felder:
private Map<UUID, Float> lastEntityHealth = new HashMap<>()
private float lastDamageDealt = 0f
private boolean lastWasCrit   = false
private long    lastHitTime   = 0L

// In onClientTick():
if (mc.targetedEntity instanceof LivingEntity target):
    UUID id = target.getUuid()
    float currentHp = target.getHealth()
    float prevHp    = lastEntityHealth.getOrDefault(id, currentHp)
    
    float delta = prevHp - currentHp
    if (delta > 0.01f):   // Schaden wurde zugefügt
        lastDamageDealt = delta
        lastHitTime     = System.currentTimeMillis()
        sessionTotal   += delta
        maxHit          = Math.max(maxHit, delta)
        dpsWindow.add(new DpsEntry(System.currentTimeMillis(), delta))
        
        // Krit-Erkennung: Player-Velocity & airborne heuristisch
        // (mc.player.fallDistance > 0 && !mc.player.isOnGround() && !mc.player.isSprinting())
        lastWasCrit = detectCrit(mc.player)
    
    lastEntityHealth.put(id, currentHp)

// Cleanup toter Entitäten aus der Map
lastEntityHealth.entrySet().removeIf(e -> mc.world.getEntityById(e.getKey().hashCode()) == null)
```

**DPS-Berechnung:**

```java
// dpsWindow: ArrayDeque<DpsEntry(long timestamp, float damage)>
// In onClientTick: Einträge älter als 5000ms entfernen
// dps = sum(damage in window) / 5.0f
```

### Rendering

```
onRenderHud(ctx, tickDelta):
    if (mc.player == null) return
    
    long elapsed = System.currentTimeMillis() - lastHitTime
    long maxMs   = displayTime.get() * 1000L
    if (hideWhenZero.get() && elapsed > maxMs) return
    
    // Fade-Out Alpha berechnen
    float alpha = 1.0f
    if (fadeOut.get() && elapsed > maxMs - 1000L):
        alpha = Math.max(0f, (maxMs - elapsed) / 1000.0f)
    
    int baseColor = lastWasCrit ? critColor.get() : damageColor.get()
    int color = applyAlpha(baseColor, alpha)
    
    String format = "%." + decimalPlaces.get() + "f"
    String dmgStr = "Hit: " + String.format(format, lastDamageDealt) + " ♥"
    
    int lineY = y + 4
    drawText(ctx, dmgStr, x + 4, lineY, color);  lineY += 10
    if (showTotal.get()):
        drawText(ctx, "Total: " + String.format(format, sessionTotal), x + 4, lineY, textColor.get());  lineY += 10
    if (showMaxHit.get()):
        drawText(ctx, "Max: " + String.format(format, maxHit), x + 4, lineY, critColor.get());  lineY += 10
    if (showDps.get()):
        drawText(ctx, "DPS: " + String.format(format, currentDps), x + 4, lineY, textColor.get())
```

### Edge Cases

- Entität heilt sich während man zuschlägt: Delta kann negativ sein → `if (delta > 0.01f)` Guard.
- `targetedEntity` wechselt schnell: Map speichert Health per UUID, kein Problem.
- Entität stirbt: `lastEntityHealth` aufräumen um Memory-Leak zu vermeiden.
- Krit-Erkennung ist eine Heuristik — kann falsch positiv sein (Sprung ohne Krit). Kein direktes Client-Event verfügbar.
- Session-Reset: `onDisable()` / `onEnable()` setzt `sessionTotal`, `maxHit` und `dpsWindow` zurück.

## Translation Keys

```json
"noctra.damage_dealt_hud.name": "Damage Dealt",
"noctra.damage_dealt_hud.description": "Shows the last damage dealt and session combat statistics.",
"noctra.damage_dealt_hud.bg_color": "Background Color",
"noctra.damage_dealt_hud.border_color": "Border Color",
"noctra.damage_dealt_hud.text_color": "Text Color",
"noctra.damage_dealt_hud.text_shadow": "Text Shadow",
"noctra.damage_dealt_hud.text_scale": "Text Scale",
"noctra.damage_dealt_hud.show_background": "Show Background",
"noctra.damage_dealt_hud.display_time": "Display Time (s)",
"noctra.damage_dealt_hud.show_total": "Show Session Total",
"noctra.damage_dealt_hud.show_max_hit": "Show Max Hit",
"noctra.damage_dealt_hud.show_dps": "Show DPS",
"noctra.damage_dealt_hud.fade_out": "Fade Out",
"noctra.damage_dealt_hud.damage_color": "Damage Color",
"noctra.damage_dealt_hud.crit_color": "Crit Hit Color",
"noctra.damage_dealt_hud.decimal_places": "Decimal Places",
"noctra.damage_dealt_hud.hide_when_zero": "Hide When Zero"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/damage_dealt_hud.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Rotes Herz mit einem Schwert-Symbol darüber und einer Zahl darunter. Aggressive Pixel-Art.
