# Damage Indicator

**Status:** [x] DONE  
**Module ID:** `damage_indicator`  
**Class:** `DamageIndicatorModule`  
**Category:** VISUAL  
**File:** `modules/impl/damage_indicator/DamageIndicatorModule.java`

## Description

Zeigt schwebende Schadenszahlen über Entities an, wenn diese Schaden nehmen oder heilen. Die Zahlen erscheinen an der Position des getroffenen Entities im Weltraum und animieren nach oben, bevor sie verschwinden. Gibt im Kampf sofortiges Feedback über die Höhe des zugefügten Schadens.

## System Notes (Updated)

- Module Ordner: `modules/impl/damage_indicator/DamageIndicatorModule.java`
- Package: `de.snenjih.noctra.modules.impl.damage_indicator`
- Implementiert HudElement: Nein (Welt-Rendering via `onRenderWorld`)
- Benötigt Mixin auf `LivingEntity.damage()` oder `onDamage`-Event um Schadensevents zu erfassen
- Verwendet `WorldRenderContext` aus `net.fabricmc.fabric.api.client.rendering.v1.world`
- `ColorSetting` für Schadens- und Heilungsfarbe

## Settings

| ID | Type | Default | Label | Description |
|----|------|---------|-------|-------------|
| `show_damage` | BooleanSetting | `true` | "Show Damage" | Schadenzahlen anzeigen |
| `show_healing` | BooleanSetting | `true` | "Show Healing" | Heilungszahlen anzeigen |
| `damage_color` | ColorSetting | `0xFFFF5555` | "Damage Color" | Farbe der Schadenzahlen (ARGB) |
| `healing_color` | ColorSetting | `0xFF55FF55` | "Healing Color" | Farbe der Heilungszahlen (ARGB) |
| `float_speed` | FloatSetting | `1.0` | `0.1–3.0` | "Float Speed" | Geschwindigkeit, mit der die Zahlen nach oben steigen |
| `duration_ticks` | IntSetting | `30` | `10–80` | "Duration (ticks)" | Wie lange die Zahlen sichtbar bleiben |
| `show_player_damage` | BooleanSetting | `true` | "Show Own Damage" | Auch Schaden am lokalen Spieler anzeigen |

## Implementation Notes

- Interne Datenstruktur: `List<DamageParticle>` mit Feldern: `Vec3d pos`, `float amount`, `boolean isHeal`, `int ticksLeft`.
- Mixin-Target: `net.minecraft.entity.LivingEntity` — Methode `damage(ServerWorld, DamageSource, float)` — `@Inject @At("HEAD")` um Betrag zu erfassen und Partikel zu erzeugen. Nur auf Client-Side via `mc.world` prüfen.
- Alternativ: Mixin auf `LivingEntity.onDeath` und HP-Delta verfolgen via `onClientTick`.
- `onRenderWorld(WorldRenderContext ctx)`: Für jeden aktiven `DamageParticle` die Zahl im 3D-Raum rendern via `WorldRenderer.renderText` oder Bill-Board-Rendering mit `MatrixStack`.
- `ticksLeft` in `onClientTick` dekrementieren; abgelaufene Partikel entfernen.
- Positions-Offset: Zahl erscheint über dem Entity (`pos.add(0, entity.getHeight() + 0.5, 0)`) und steigt mit `ticksLeft`-Faktor nach oben.

## Edge Cases

- Sehr viele Entities gleichzeitig: Partikel-Liste auf max. 50 Einträge begrenzen (älteste zuerst entfernen).
- Entity stirbt im selben Tick: Partikel wird trotzdem erzeugt und nach oben animiert.
- Spieler selbst (lokaler Spieler): Via `show_player_damage`-Setting konfigurierbar.
- Schaden = 0 (Absorption/Rüstung): Trotzdem anzeigen oder per Mindest-Threshold (`>= 0.5`) filtern.
- Welt noch nicht geladen: Guard in `onRenderWorld` auf `mc.world != null`.

## Translation Keys

```json
"noctra.module.damage_indicator.name": "Damage Indicator",
"noctra.module.damage_indicator.description": "Shows floating damage numbers above entities when they take damage."
```

## Icon

Path: `textures/gui/sprites/modules/damage_indicator.png`  
32×32 PNG. Zeigt eine rote Zahl (z. B. "-8") mit einem kleinen roten Pfeil nach oben, das über einem Entity-Silhouette schwebt. Pixel-Art-Stil.
