package de.snenjih.noctra.modules.impl.coordinates_hud;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class CoordinatesHudModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showDirection;
    private final ModuleSetting<Boolean> showBiome;
    private final ModuleSetting<Boolean> compactMode;
    private final ModuleSetting<Boolean> showChunkPos;
    private final ModuleSetting<Boolean> showWithinChunk;
    private final ModuleSetting<Boolean> showNetherCoords;
    private final ModuleSetting<Boolean> showRegionFile;

    public CoordinatesHudModule() {
        super(
            "coordinates_hud",
            "Coordinates HUD",
            "Displays your X/Y/Z coordinates on the HUD.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/coordinates_hud")
        );
        showDirection    = addSetting(new BooleanSetting("show_direction",    "Show Direction",       true));
        showBiome        = addSetting(new BooleanSetting("show_biome",        "Show Biome",           false));
        compactMode      = addSetting(new BooleanSetting("compact_mode",      "Compact Mode",         false));
        beginSection("Chunk Info");
        showChunkPos     = addSetting(new BooleanSetting("show_chunk_pos",    "Show Chunk Position",  false));
        showWithinChunk  = addSetting(new BooleanSetting("show_within_chunk", "Show Within-Chunk Offset", true));
        showNetherCoords = addSetting(new BooleanSetting("show_nether_coords","Show Nether Coords",   false));
        showRegionFile   = addSetting(new BooleanSetting("show_region_file",  "Show Region File",     false));
    }

    // ── HudElement ────────────────────────────────────────────────────────────

    @Override public String getHudId()      { return "coordinates_hud"; }
    @Override public String getHudName()    { return "Coordinates HUD"; }
    @Override public int getDefaultWidth()  { return 130; }
    @Override public int getDefaultHeight() { return 40; }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        ClientPlayerEntity player = mc.player;
        var tr = mc.textRenderer;

        int bx = (int) player.getX();
        int by = (int) player.getY();
        int bz = (int) player.getZ();

        // Count lines for dynamic height
        boolean chunkPos    = showChunkPos.get();
        boolean netherCoord = showNetherCoords.get() && mc.world != null
                && (mc.world.getRegistryKey().equals(World.OVERWORLD) || mc.world.getRegistryKey().equals(World.NETHER));
        boolean regionFile  = showRegionFile.get();
        boolean biome       = showBiome.get();

        int extraLines = (biome ? 1 : 0) + (chunkPos ? 1 : 0) + (netherCoord ? 1 : 0) + (regionFile ? 1 : 0);
        int totalH = compactMode.get() ? 18 : (h + extraLines * 10);

        drawBackground(ctx, x, y, w, totalH);

        if (compactMode.get()) {
            String text = bx + " / " + by + " / " + bz;
            ctx.drawTextWithShadow(tr, text, x + 4, y + (totalH - 8) / 2, textColor.get());
        } else {
            ctx.drawTextWithShadow(tr, "X: " + bx, x + 4, y + 5,  textColor.get());
            ctx.drawTextWithShadow(tr, "Y: " + by, x + 4, y + 14, textColor.get());
            ctx.drawTextWithShadow(tr, "Z: " + bz, x + 4, y + 23, textColor.get());

            if (showDirection.get()) {
                String dir = player.getHorizontalFacing().asString().toUpperCase();
                ctx.drawTextWithShadow(tr, dir, x + w - tr.getWidth(dir) - 4, y + 14, 0xFF8899AA);
            }
        }

        int currentY = y + (compactMode.get() ? 20 : h - 5);

        if (biome && mc.world != null) {
            try {
                var biomeEntry = mc.world.getBiome(player.getBlockPos());
                String biomeName = biomeEntry.getKey()
                        .map(k -> k.getValue().getPath())
                        .orElse("unknown");
                biomeName = biomeName.replace('_', ' ');
                if (!biomeName.isEmpty()) {
                    biomeName = Character.toUpperCase(biomeName.charAt(0)) + biomeName.substring(1);
                }
                ctx.drawTextWithShadow(tr, biomeName, x + 4, currentY, 0xFF8899AA);
                currentY += 10;
            } catch (Exception ignored) {}
        }

        if (chunkPos) {
            int chunkX = bx >> 4;
            int chunkZ = bz >> 4;
            String chunkLine = "Chunk: " + chunkX + " / " + chunkZ;
            if (showWithinChunk.get()) {
                int offsetX = ((bx % 16) + 16) % 16;
                int offsetZ = ((bz % 16) + 16) % 16;
                chunkLine += "  [" + offsetX + ", " + offsetZ + "]";
            }
            ctx.drawTextWithShadow(tr, chunkLine, x + 4, currentY, 0xFF8899AA);
            currentY += 10;
        }

        if (netherCoord && mc.world != null) {
            boolean inNether = mc.world.getRegistryKey().equals(World.NETHER);
            String label;
            int eqX, eqZ;
            if (inNether) {
                label = "Overworld:";
                eqX = (int)(player.getX() * 8.0);
                eqZ = (int)(player.getZ() * 8.0);
            } else {
                label = "Nether:";
                eqX = (int)(player.getX() / 8.0);
                eqZ = (int)(player.getZ() / 8.0);
            }
            ctx.drawTextWithShadow(tr, label + " " + eqX + " / " + eqZ, x + 4, currentY, 0xFF88BBFF);
            currentY += 10;
        }

        if (regionFile) {
            int chunkX  = bx >> 4;
            int chunkZ  = bz >> 4;
            int regionX = chunkX >> 5;
            int regionZ = chunkZ >> 5;
            ctx.drawTextWithShadow(tr, "Region: r." + regionX + "." + regionZ + ".mca", x + 4, currentY, 0xFF667788);
        }
    }
}
