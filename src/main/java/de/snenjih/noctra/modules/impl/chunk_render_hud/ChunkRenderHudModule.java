package de.snenjih.noctra.modules.impl.chunk_render_hud;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ChunkRenderHudModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showChunks;
    private final ModuleSetting<Boolean> showEntities;
    private final ModuleSetting<Boolean> showBlockEntities;
    private final ModuleSetting<Boolean> compactMode;
    private final ModuleSetting<Integer> colorWarnEntities;
    private final ModuleSetting<Integer> colorEntityWarn;

    public ChunkRenderHudModule() {
        super(
            "chunk_render_hud",
            "Chunk Render Stats",
            "Shows chunk and entity render stats on the HUD.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/chunk_render_hud")
        );
        showChunks        = addSetting(new BooleanSetting("show_chunks",         "Show Chunks",        true));
        showEntities      = addSetting(new BooleanSetting("show_entities",       "Show Entities",      true));
        showBlockEntities = addSetting(new BooleanSetting("show_block_entities", "Show Block Entities",true));
        compactMode       = addSetting(new BooleanSetting("compact_mode",        "Compact Mode",       false));
        beginSection("Thresholds");
        colorWarnEntities = addSetting(new IntSetting    ("color_warn_entities", "Entity Warn Count",  200, 50, 2000));
        beginSection("Colors");
        colorEntityWarn   = addSetting(new ColorSetting  ("color_entity_warn",   "Entity Warn Color",  0xFFFFFF55));
    }

    @Override public String getHudId()      { return "chunk_render_hud"; }
    @Override public String getHudName()    { return "Chunk Render Stats"; }
    @Override public int getDefaultWidth()  { return 160; }
    @Override public int getDefaultHeight() { return 50; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.worldRenderer == null) return;

        var tr = mc.textRenderer;

        int completedChunks = mc.worldRenderer.getCompletedChunkCount();
        int totalChunks     = (int) mc.worldRenderer.getChunkCount();
        int entityCount     = mc.world.getRegularEntityCount();
        int beCount         = mc.world.getBlockEntities().size();

        int entityColor = entityCount >= colorWarnEntities.get() ? colorEntityWarn.get() : textColor.get();

        if (compactMode.get()) {
            StringBuilder sb = new StringBuilder();
            if (showChunks.get())        sb.append("C:").append(completedChunks).append("/").append(totalChunks).append("  ");
            if (showEntities.get())      sb.append("E:").append(entityCount).append("  ");
            if (showBlockEntities.get()) sb.append("BE:").append(beCount);
            String line = sb.toString().stripTrailing();
            drawBackground(ctx, x, y, Math.max(w, tr.getWidth(line) + 8), 18);
            ctx.drawTextWithShadow(tr, line, x + 4, y + 5, textColor.get());
            return;
        }

        List<String> lines  = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        if (showChunks.get()) {
            lines.add("Chunks: " + completedChunks + " / " + totalChunks);
            colors.add(textColor.get());
        }
        if (showEntities.get()) {
            lines.add("Entities: " + entityCount);
            colors.add(entityColor);
        }
        if (showBlockEntities.get()) {
            lines.add("Block Ent: " + beCount);
            colors.add(textColor.get());
        }

        if (lines.isEmpty()) return;

        int totalH = 8 + lines.size() * 10;
        drawBackground(ctx, x, y, w, totalH);

        int lineY = y + 4;
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawTextWithShadow(tr, lines.get(i), x + 4, lineY, colors.get(i));
            lineY += 10;
        }
    }
}
