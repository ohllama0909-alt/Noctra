# Hit Indicator

**ID:** `hit_indicator`  
**Category:** COMBAT  
**Status:** [x] DONE  
**Class:** `modules/impl/hit_indicator/HitIndicatorModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.hit_indicator`

## System Notes (Updated)

- Module Ordner: `modules/impl/hit_indicator/HitIndicatorModule.java`
- Package: `de.snenjih.noctra.modules.impl.hit_indicator`
- Implementiert HudElement: Ja — rendert Treffer-Overlay auf dem HUD
- `ColorSetting` für Indikatorfarbe verwenden

## Description

Gibt visuelles Feedback, wenn der Spieler einen Treffer auf ein Entity landet. Das Feedback erscheint als kurzer Bildschirm-Flash (halbtransparente Einfärbung des Bildschirms) und/oder als Text-Overlay ("Hit!" / Schadenszahl). Vanilla zeigt bei Treffern keine besonderen Effekte für den Angreifer — nur das getroffene Entity blinkt rot. Dieses Modul macht Treffer für den Spieler spürbar und verbessert das Kampfgefühl.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `flash_enabled` | Boolean | `true` | — | "Screen Flash" | Kurzen Bildschirm-Flash bei Treffer anzeigen |
| `flash_color` | Int | `0x33FF0000` | ARGB | "Flash Color" | Farbe des Bildschirm-Flashes (Standard: halbtransparentes Rot) |
| `flash_duration` | Int | `5` | `1–20` | "Flash Duration" | Dauer des Flashes in Ticks (1 Tick = 50ms) |
| `show_text` | Boolean | `false` | — | "Show Hit Text" | "Hit!" als Text-Overlay anzeigen |
| `show_damage` | Boolean | `false` | — | "Show Damage" | Erlittenen Schaden des Gegners als Zahl anzeigen |
| `text_color` | Int | `0xFFFF4444` | ARGB | "Text Color" | Farbe des Hit-Textes |

## Implementation

### Event Hooks

- `onAttackEntity(ClientPlayerEntity player, Entity target)` — Wird aufgerufen wenn der Spieler ein Entity angreift. Hier wird der Hit-Zustand ausgelöst.
- `onRenderHud(DrawContext ctx, RenderTickCounter counter)` — Flash und Text rendern, solange `flashTicksRemaining > 0`.

### Required Mixins

**Für Schadenszahl-Anzeige** (`show_damage`):
- **Class:** `mixin/LivingEntityMixin.java`
- **Target:** `net.minecraft.entity.LivingEntity`
- **Injection:** `@Inject(at = @At("HEAD"), method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z")`

Hinweis: `damage()` wird serverseitig aufgerufen. Auf dem Client ist der HP-Verlust nur indirekt über `entity.getHealth()` messbar. Statt Mixin: Gesundheit des Targets in `onAttackEntity` speichern und im nächsten `onClientTick` vergleichen.

**Für einfaches Hit-Flash ohne Schadenszahl:** Kein Mixin erforderlich.

### Core Algorithm

```
// Felder in der Modul-Klasse:
private int flashTicksRemaining = 0
private float lastTargetHealth = -1f   // für Schadens-Berechnung
private Entity lastTarget = null
private float lastDamageDealt = 0f
private int hitTextTicksRemaining = 0

onAttackEntity(player, target):
  // Flash starten
  if flashEnabled.get():
    flashTicksRemaining = flashDuration.get()

  // Gesundheit merken für Schadens-Berechnung
  if (showDamage.get() && target instanceof LivingEntity living):
    lastTargetHealth = living.getHealth()
    lastTarget = target

  if showText.get():
    hitTextTicksRemaining = 40  // 2 Sekunden anzeigen

onClientTick(client):
  // Flash-Timer herunterzählen
  if flashTicksRemaining > 0:
    flashTicksRemaining--

  // Schadens-Berechnung: Im nächsten Tick nach dem Angriff HP vergleichen
  if lastTarget != null && lastTarget instanceof LivingEntity living:
    float currentHp = living.getHealth()
    if currentHp < lastTargetHealth:
      lastDamageDealt = lastTargetHealth - currentHp
      lastTargetHealth = currentHp
    // Target-Referenz nach ein paar Ticks aufräumen
    // (Entity könnte sterben oder despawnen)
    if living.isRemoved() || living.getHealth() <= 0:
      lastTarget = null

  if hitTextTicksRemaining > 0:
    hitTextTicksRemaining--

onRenderHud(ctx, counter):
  mc = MinecraftClient.getInstance()
  if mc.player == null → return

  // Bildschirm-Flash
  if flashEnabled.get() && flashTicksRemaining > 0:
    // Fade-out: Alpha linear von voll bis 0 über die Dauer
    float fadeProgress = flashTicksRemaining / (float) flashDuration.get()
    int baseColor = flashColor.get()
    int alpha = (int)((baseColor >> 24 & 0xFF) * fadeProgress)
    int fadedColor = (alpha << 24) | (baseColor & 0x00FFFFFF)
    int sw = mc.getWindow().getScaledWidth()
    int sh = mc.getWindow().getScaledHeight()
    ctx.fill(0, 0, sw, sh, fadedColor)  // Ganzer Bildschirm

  // Hit-Text
  if showText.get() && hitTextTicksRemaining > 0:
    int sw = mc.getWindow().getScaledWidth()
    int sh = mc.getWindow().getScaledHeight()
    String hitLabel = "Hit!"
    if showDamage.get() && lastDamageDealt > 0:
      hitLabel = String.format("-%.1f", lastDamageDealt)
    float fadeText = hitTextTicksRemaining / 40f
    // Text zentriert, leicht oberhalb der Mitte
    int textX = sw / 2 - mc.textRenderer.getWidth(hitLabel) / 2
    int textY = sh / 2 - 30
    ctx.drawTextWithShadow(mc.textRenderer, Text.literal(hitLabel), textX, textY, textColor.get())
```

**Wichtig: Schadens-Detektion auf dem Client**
Der Client kennt den tatsächlichen Schaden nicht direkt (Damage Calculation läuft auf dem Server). Die Annäherung über `entity.getHealth()` vor und nach dem Angriff ist die einzige client-seitige Methode ohne Server-Mods. Sie funktioniert, solange der Server die HP-Änderung zeitnah synchronisiert (passiert nach jedem Treffer über `EntityTrackerUpdateS2CPacket`).

### Edge Cases

- **Treffer auf totes Entity:** `onAttackEntity` wird trotzdem aufgerufen. Guard: `if (target instanceof LivingEntity l && l.getHealth() <= 0) return` — Flash trotzdem zeigen, Schadenszahl ist 0.
- **Mehrere Treffer schnell hintereinander:** `flashTicksRemaining` wird bei jedem Treffer zurückgesetzt (neu gestartet). `lastDamageDealt` wird beim letzten gemessenen Treffer aktualisiert.
- **Entity stirbt am Treffer:** `living.getHealth()` gibt 0 zurück. `lastDamageDealt = lastTargetHealth - 0 = lastTargetHealth`. Das ist korrekt.
- **Splash-Potions / Knockback:** `onAttackEntity` zählt nur direkte Angriffe (linke Maustaste). AoE-Schaden wird nicht erfasst — das ist beabsichtigt.
- **`mc.options.hudHidden`:** Beim Rendering prüfen; wenn HUD versteckt, nicht rendern.
- **Mehrere Entities im Ziel:** `lastTarget` hält nur das zuletzt angegriffene Entity; bei Tab-Target-Wechsel ist das sauber.

## Translation Keys

```json
"noctra.hit_indicator.name": "Hit Indicator",
"noctra.hit_indicator.description": "Shows a visual flash and text when you land a hit on an entity.",
"noctra.hit_indicator.flash_enabled": "Screen Flash",
"noctra.hit_indicator.flash_color": "Flash Color",
"noctra.hit_indicator.flash_duration": "Flash Duration",
"noctra.hit_indicator.show_text": "Show Hit Text",
"noctra.hit_indicator.show_damage": "Show Damage",
"noctra.hit_indicator.text_color": "Text Color"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/hit_indicator.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Ein rotes Ausrufezeichen oder stilisierter Einschlag-Effekt (konzentrische Wellen in Rot/Orange) auf dunklem Hintergrund. Alternativ: ein Schwert-Silhouette, das einen roten Funken beim Aufprall zeigt.
