# Reach Display

**ID:** `reach_display`  
**Category:** COMBAT  
**Status:** [x] DONE  
**Class:** `modules/impl/reach_display/ReachDisplayModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.reach_display`

## System Notes (Updated)

- Module Ordner: `modules/impl/reach_display/ReachDisplayModule.java`
- Package: `de.snenjih.noctra.modules.impl.reach_display`
- Implementiert HudElement: Ja — `extends BaseModule implements HudElement`
- In `NoctraMod.onInitializeClient()`: `HudRegistry.register(module, defaultX, defaultY)`

## Description

Zeigt die aktuellen Angriffs- und Interaktions-Reichweiten des Spielers auf dem HUD an. Die Reichweite variiert je nach Spielmodus (Survival: 3 Blöcke Angriff / 4.5 Blöcke Interaktion; Creative: 5 / 5), durch Attribute-Modifikatoren (z.B. potions, enchantments) und mögliche Server-Plugins. Dieses Modul macht die tatsächlich aktive Reichweite sichtbar — hilfreich beim PvP-Testing, beim Debuggen von Servern und für Speedrunner.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `x_pos` | Int | `5` | `0–1920` | "X Position" | Horizontale Position des HUD-Elements |
| `y_pos` | Int | `40` | `0–1080` | "Y Position" | Vertikale Position des HUD-Elements |
| `show_attack` | Boolean | `true` | — | "Show Attack Reach" | Angriffs-Reichweite anzeigen |
| `show_interact` | Boolean | `true` | — | "Show Interact Reach" | Interaktions-Reichweite anzeigen |
| `decimal_places` | Int | `2` | `0–3` | "Decimal Places" | Nachkommastellen der Reichweitenanzeige |
| `background` | Boolean | `true` | — | "Background" | Halbtransparenten Hintergrund zeichnen |
| `text_color` | Int | `0xFFFFFFFF` | ARGB | "Text Color" | Farbe des Textes |

## Implementation

### Event Hooks

- `onRenderHud(DrawContext ctx, RenderTickCounter counter)` — Reichweiten-Werte aus Player-Attributen lesen und rendern.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
onRenderHud(ctx, counter):
  mc = MinecraftClient.getInstance()
  player = mc.player
  if player == null → return
  if mc.options.hudHidden → return

  int x = xPos.get()
  int y = yPos.get()
  String format = "%." + decimalPlaces.get() + "f"
  int lineY = y

  if showAttack.get():
    double attackReach = getAttackReach(player)
    String label = "Attack: " + String.format(format, attackReach) + " b"
    renderLine(ctx, mc, label, x, lineY)
    lineY += 12

  if showInteract.get():
    double interactReach = getInteractReach(mc, player)
    String label = "Reach: " + String.format(format, interactReach) + " b"
    renderLine(ctx, mc, label, x, lineY)

private void renderLine(DrawContext ctx, MinecraftClient mc, String label, int x, int y):
  if background.get():
    int w = mc.textRenderer.getWidth(label)
    ctx.fill(x - 2, y - 2, x + w + 2, y + 10, 0x88000000)
  ctx.drawTextWithShadow(mc.textRenderer, Text.literal(label), x, y, textColor.get())

private double getAttackReach(ClientPlayerEntity player):
  // Attack reach basiert auf dem EntityInteractionRange-Attribut
  // In 1.21.x: EntityAttributes.ENTITY_INTERACTION_RANGE (für Angriff auf Entities)
  EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE)
  if attr != null:
    return attr.getValue()
  // Fallback: Survival = 3.0, Creative = 5.0
  return player.isCreative() ? 5.0 : 3.0

private double getInteractReach(MinecraftClient mc, ClientPlayerEntity player):
  // Block-Interaktions-Reichweite:
  // ClientPlayerInteractionManager.getReachDistance() gibt die konfigurierte Reichweite zurück
  // (beeinflusst durch Creative-Modus, kann durch Server-Side Modding variieren)
  if mc.interactionManager != null:
    return mc.interactionManager.getReachDistance()
  return player.isCreative() ? 5.0 : 4.5
```

**Reach-Attribute in 1.21.11:**

| Attribut | Bedeutung | Default Survival | Default Creative |
|---|---|---|---|
| `EntityAttributes.ENTITY_INTERACTION_RANGE` | Angriffs-Reichweite auf Entities | 3.0 | 5.0 |
| `EntityAttributes.BLOCK_INTERACTION_RANGE` | Block-Interaktions-Reichweite | 4.5 | 5.0 |

`ClientPlayerInteractionManager.getReachDistance()` liest intern `BLOCK_INTERACTION_RANGE` aus (mit Creative-Override). Für Entity-Reach besser direkt das Attribut abfragen.

**Vollständige Attribut-Abfrage:**
```java
private double getAttackReach(ClientPlayerEntity player) {
    EntityAttributeInstance attackRange = player.getAttributeInstance(
        EntityAttributes.ENTITY_INTERACTION_RANGE
    );
    if (attackRange != null) {
        return attackRange.getValue();
    }
    return player.isCreative() ? 5.0 : 3.0;
}

private double getBlockReach(MinecraftClient mc, ClientPlayerEntity player) {
    // Option 1: Über InteractionManager (einfacher)
    if (mc.interactionManager != null) {
        return mc.interactionManager.getReachDistance();
    }
    // Option 2: Direkt über Attribut
    EntityAttributeInstance blockRange = player.getAttributeInstance(
        EntityAttributes.BLOCK_INTERACTION_RANGE
    );
    if (blockRange != null) {
        return blockRange.getValue();
    }
    return player.isCreative() ? 5.0 : 4.5;
}
```

**Hinweis zu `EntityAttributes`-Klasse in 1.21.11:**
`EntityAttributes` liegt in `net.minecraft.entity.attribute.EntityAttributes`. Die Felder `ENTITY_INTERACTION_RANGE` und `BLOCK_INTERACTION_RANGE` wurden in 1.20.5 eingeführt und sind in 1.21.x verfügbar. Falls das Mapping abweicht: im Yarn-Mapping-Browser unter `1.21.11+build.6` nachschlagen.

**Warum zwei unterschiedliche Reichweiten:**
- `ENTITY_INTERACTION_RANGE` (ehemals "attack range"): Für das Treffen von Entitäten via linker Maustaste.
- `BLOCK_INTERACTION_RANGE` (ehemals "reach distance"): Für Blockinteraktionen (Rechtsklick, Abbau).
- In Vanilla Survival: Entity-Range = 3.0b, Block-Range = 4.5b. Creative: beide 5.0b.
- Auf Servern mit Plugins (Paper, Velocity) können diese Werte abweichen; das Attribut spiegelt den tatsächlichen Client-seitig bekannten Wert wider.

### Edge Cases

- **`player.getAttributeInstance()` gibt `null` zurück:** Passiert wenn das Attribut nicht registriert ist (modifizierter Server ohne standard Attribute). Fallback auf Hard-Coded-Defaults (3.0 / 4.5).
- **Spectator-Modus:** Spectators haben keine Angriffs-Reichweite (sie können nicht angreifen). `player.isSpectator()` prüfen und entsprechend "N/A" anzeigen oder Spectator-Reichweite (5.0b) nutzen.
- **Sehr kleine Werte (< 0.1):** Theoretisch möglich durch Modifikatoren. `Math.max(0, value)` als Guard vor dem Rendern.
- **Sehr viele Dezimalstellen:** `decimal_places = 3` gibt z.B. `3.000 b`. Sauber durch Format-String gehandelt.
- **HUD ausgeblendet:** `mc.options.hudHidden` → return.
- **`mc.interactionManager == null`:** Passiert in seltenen Zuständen beim Verbindungsaufbau. Fallback-Wert verwenden.

## Translation Keys

```json
"noctra.reach_display.name": "Reach Display",
"noctra.reach_display.description": "Shows your current attack and interaction reach on the HUD.",
"noctra.reach_display.x_pos": "X Position",
"noctra.reach_display.y_pos": "Y Position",
"noctra.reach_display.show_attack": "Show Attack Reach",
"noctra.reach_display.show_interact": "Show Interact Reach",
"noctra.reach_display.decimal_places": "Decimal Places",
"noctra.reach_display.background": "Background",
"noctra.reach_display.text_color": "Text Color"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/reach_display.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Ein Messliniensymbol — eine horizontale Linie mit zwei vertikalen Endmarkierungen (wie ein Lineal), mittig eine Zahl oder Pfeilspitze. Alternativ: ein Fadenkreuz mit Radius-Kreisen in verschiedenen Abständen, die unterschiedliche Reichweiten darstellen.
