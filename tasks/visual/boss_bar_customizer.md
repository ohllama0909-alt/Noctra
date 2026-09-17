# Boss Bar Customizer

**Status:** [x] DONE  
**Module ID:** `boss_bar_customizer`  
**Class:** `BossBarCustomizerModule`  
**Category:** VISUAL  
**File:** `modules/impl/boss_bar_customizer/BossBarCustomizerModule.java`

## Description

Ermöglicht das Anpassen von Position, Größe und Sichtbarkeit der Vanilla-Bossbar. Die Standard-Bossbar erscheint immer oben in der Bildschirmmitte — dieses Modul erlaubt es, sie zu verschieben, zu verkleinern oder komplett auszublenden. Nützlich in Builds, Videos oder wenn die Bossbar zu viel HUD-Platz einnimmt.

## System Notes (Updated)

- Module Ordner: `modules/impl/boss_bar_customizer/BossBarCustomizerModule.java`
- Package: `de.snenjih.noctra.modules.impl.boss_bar_customizer`
- Implementiert HudElement: Nein (Mixin auf `BossBarHud` / `InGameHud`)
- Benötigt Mixin auf `BossBarHud.render()` um Position und Sichtbarkeit zu überschreiben
- Alternativ: Mixin auf `ClientBossBar` um die Render-Koordinaten zu verändern

## Settings

| ID | Type | Default | Label | Description |
|----|------|---------|-------|-------------|
| `hide_bar` | BooleanSetting | `false` | "Hide Boss Bar" | Bossbar vollständig ausblenden |
| `x_offset` | IntSetting | `0` | `-500–500` | "X Offset" | Horizontale Verschiebung vom Standard-Mittelpunkt (px) |
| `y_offset` | IntSetting | `0` | `-400–400` | "Y Offset" | Vertikale Verschiebung von der Standard-Position (px) |
| `scale` | FloatSetting | `1.0` | `0.25–2.0` | "Scale" | Skalierungsfaktor der Bossbar |
| `hide_text` | BooleanSetting | `false` | "Hide Text" | Nur den Text der Bossbar ausblenden |

## Implementation Notes

- Mixin-Target: `net.minecraft.client.gui.hud.BossBarHud`
- Methode: `render(DrawContext ctx, RenderTickCounter counter)` — `@Inject @At("HEAD") cancellable = true`
- Bei `hide_bar`: `ci.cancel()` sofort zurückgeben.
- Für Verschiebung/Skalierung: `@Redirect` auf die `fill`- und `drawGuiTexture`-Aufrufe, um X/Y-Koordinaten zu modifizieren, **oder** `MatrixStack`-Push vor den Render-Calls mit `translate` und `scale`.
- Der einfachste Ansatz: Bei `@At("HEAD")` `ci.cancel()`, dann manuell mit angepassten Koordinaten rendern (Aufwand: Vanilla-Render-Logik nachbauen).

## Edge Cases

- Keine aktive Bossbar: Kein Render, kein Problem.
- Mehrere Bossbars gleichzeitig: Vanilla rendert alle untereinander. Offset gilt für den gesamten Block.
- `scale < 0.5f`: Schrift kann unlesbar werden — Spieler trägt Verantwortung.
- Singleplayer mit Ender Dragon: Bossbar erscheint normal; Modul greift.
- `hide_text` ohne `hide_bar`: Nur Textausgabe unterdrücken, Balken sichtbar lassen.

## Translation Keys

```json
"noctra.module.boss_bar_customizer.name": "Boss Bar Customizer",
"noctra.module.boss_bar_customizer.description": "Move, scale, or hide the vanilla boss health bar."
```

## Icon

Path: `textures/gui/sprites/modules/boss_bar_customizer.png`  
32×32 PNG. Zeigt einen horizontalen lila/violetten Balken (wie Vanilla-Bossbar) mit einem kleinen Pfeil oder Zahnrad-Symbol darunter. Pixel-Art-Stil.
