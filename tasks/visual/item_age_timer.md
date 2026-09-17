# Item Age Timer

**ID:** `item_age_timer`  
**Category:** VISUAL  
**Status:** [x] DONE  
**Class:** `modules/impl/item_age_timer/ItemAgeTimerModule.java`  
**Package:** `de.snenjih.noctra.modules.impl.item_age_timer`

## System Notes (Updated)

- Module Ordner: `modules/impl/item_age_timer/ItemAgeTimerModule.java`
- Package: `de.snenjih.noctra.modules.impl.item_age_timer`
- Implementiert HudElement: Nein (Mixin auf Item-Entity-Rendering)
- Benötigt Mixin auf `ItemEntityRenderer` oder `onRenderWorld`

## Description

Zeigt über gedropten Items (ItemEntity) die verbleibende Zeit bis zum Despawn an. In Vanilla despawnen Items nach 5 Minuten (6000 Ticks). Die Anzeige hilft dabei, Prioritäten beim Einsammeln zu setzen und verhindert den unbeabsichtigten Verlust wichtiger Items. Die Restzeit wird im 3D-Weltraum als Billboard-Text über dem jeweiligen Item gerendert.

## Settings

| Setting ID | Type | Default | Range / Options | Label | Beschreibung |
|---|---|---|---|---|---|
| `render_radius` | Int | `16` | `4–64` | "Render Radius" | Radius in Blöcken, innerhalb dem Items angezeigt werden |
| `show_only_low_time` | Boolean | `false` | — | "Only Show Expiring" | Nur Items mit weniger als 60 Sekunden Restzeit anzeigen |
| `low_time_threshold` | Int | `60` | `5–120` | "Expiry Threshold (s)" | Schwelle in Sekunden für "ablaufend" (Farbe ändert sich zu Rot) |
| `text_scale` | Float | `0.5` | `0.2–1.0` | "Text Scale" | Skalierung des 3D-Textes |

## Implementation

### Event Hooks

- `onRenderWorld(WorldRenderContext ctx)` — Iteriert über alle sichtbaren `ItemEntity` im Radius, berechnet Restzeit und rendert Text im Weltraum.

### Required Mixins

Kein Mixin erforderlich.

### Core Algorithm

```
Felder in der Klasse:
    private static final int MAX_ITEM_AGE = 6000;  // Ticks bis Despawn (vanilla)

onRenderWorld(WorldRenderContext ctx):
    MinecraftClient mc = MinecraftClient.getInstance()
    if (mc.player == null || mc.world == null) return

    Vec3d playerPos = mc.player.getPos()
    double radius = renderRadius.get()

    // Suchebereich als Box
    Box searchBox = new Box(
        playerPos.x - radius, playerPos.y - radius, playerPos.z - radius,
        playerPos.x + radius, playerPos.y + radius, playerPos.z + radius
    )
    List<ItemEntity> items = mc.world.getEntitiesByClass(
        ItemEntity.class, searchBox, e -> !e.isRemoved()
    )

    MatrixStack matrices = ctx.matrixStack()
    Camera camera = ctx.camera()
    Vec3d camPos = camera.getPos()

    for (ItemEntity item : items):
        int ageTicks = item.getAge()
        int remainingTicks = MAX_ITEM_AGE - ageTicks
        if (remainingTicks <= 0) continue   // bereits abgelaufen, noch nicht despawned

        int remainingSeconds = remainingTicks / 20

        if (showOnlyLowTime.get() && remainingSeconds > lowTimeThreshold.get()) continue

        // Farbe: Grün → Gelb → Rot basierend auf Restzeit
        int color
        if (remainingSeconds > lowTimeThreshold.get()):
            color = 0x55FF55  // Grün
        else if (remainingSeconds > lowTimeThreshold.get() / 2):
            color = 0xFFFF55  // Gelb
        else:
            color = 0xFF5555  // Rot

        // Text: "4:32" (MM:SS) oder "<60s" für niedrige Werte
        String text
        if (remainingSeconds >= 60):
            text = (remainingSeconds / 60) + ":" + String.format("%02d", remainingSeconds % 60)
        else:
            text = remainingSeconds + "s"

        // Position: über dem Item (Y + 0.5 Blöcke)
        Vec3d itemPos = item.getPos().add(0, 0.5, 0)
        Vec3d relative = itemPos.subtract(camPos)

        matrices.push()
        matrices.translate(relative.x, relative.y, relative.z)

        // Billboard: immer zur Kamera ausrichten
        matrices.multiply(camera.getRotation())
        matrices.scale(-textScale.get(), -textScale.get(), textScale.get())

        // Hintergrund-Rechteck (optional, für bessere Lesbarkeit)
        // Text rendern
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers()
        mc.textRenderer.draw(
            text,
            -mc.textRenderer.getWidth(text) / 2f,
            0,
            color,
            true,           // shadow
            matrices.peek().getPositionMatrix(),
            immediate,
            TextRenderer.TextLayerType.NORMAL,
            0x44000000,     // Hintergrund (semi-transparent schwarz)
            LightmapTextureManager.MAX_LIGHT_COORDINATE
        )
        immediate.draw()
        matrices.pop()
```

### Edge Cases

- `item.getAge()` gibt 0 zurück wenn Item frisch gedroppt ist: Restzeit = 6000 Ticks = 300s. Korrekt.
- Items mit `PickupDelay` (frisch gedroppt vom Spieler haben kurze Delay-Phase): Werden korrekt angezeigt, können aber noch nicht eingesammelt werden. Kein Edge Case im Rendering.
- `item.isRemoved()`: Muss geprüft werden, da Entities kurz nach Despawn noch in der Liste sein können.
- Items im Unloaded Chunk: `getEntitiesByClass` gibt nur geladene Chunks zurück. Kein Problem.
- Sehr viele Items (Farmen, Explosionen): `renderRadius` und `showOnlyLowTime` als Performance-Filter. Bei > 100 Items im Radius kann Rendering teuer werden — Limit auf 50 Items per Frame mit Priorisierung nach niedrigster Restzeit.
- `MatrixStack` / Koordinatensystem: In 1.21.11 `WorldRenderContext` → `ctx.matrixStack()` liefert die World-Matrix. Camera-relative Positionierung ist korrekt wie oben beschrieben.
- `textRenderer` in `onRenderWorld`: `MinecraftClient.getInstance().textRenderer` ist immer verfügbar.
- Despawn-Timer für Item-Frames oder andere Entities: Diese sind keine `ItemEntity`-Instanzen und werden nicht erfasst.
- Items aufgesammelt: Sie verschwinden aus der World-Entity-Liste, bevor sie despawnen. Kein Edge Case im Timer.

## Translation Keys

```json
"noctra.item_age_timer.name": "Item Age Timer",
"noctra.item_age_timer.description": "Shows remaining despawn time above dropped items in the world.",
"noctra.item_age_timer.render_radius": "Render Radius",
"noctra.item_age_timer.show_only_low_time": "Only Show Expiring",
"noctra.item_age_timer.low_time_threshold": "Expiry Threshold (s)",
"noctra.item_age_timer.text_scale": "Text Scale"
```

## Icon

**Pfad:** `src/main/resources/assets/noctra/textures/gui/sprites/modules/item_age_timer.png`  
**Größe:** 32×32 PNG  
**Vorschlag:** Item-Drop-Silhouette (kleines Quadrat/Würfel) mit einer Uhr oder Sanduhr darunter. Farbe: Orange/Gelb mit rotem Akzent auf dunklem Hintergrund.
