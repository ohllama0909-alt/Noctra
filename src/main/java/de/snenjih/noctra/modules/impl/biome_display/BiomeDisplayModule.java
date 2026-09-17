package de.snenjih.noctra.modules.impl.biome_display;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.Optional;

public class BiomeDisplayModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showLabel;
    private final ModuleSetting<Integer> updateInterval;

    private String cachedBiomeName = "";
    private int tickCounter = 0;

    public BiomeDisplayModule() {
        super(
            "biome_display",
            "Biome Display",
            "Shows the name of your current biome on screen.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/biome_display")
        );
        showLabel      = addSetting(new BooleanSetting("show_label",      "Show Label",              true));
        updateInterval = addSetting(new IntSetting("update_interval",     "Update Interval (ticks)", 20, 1, 100));
    }

    @Override public String getHudId()      { return "biome_display"; }
    @Override public String getHudName()    { return "Biome Display"; }
    @Override public int getDefaultWidth()  { return 130; }
    @Override public int getDefaultHeight() { return 18; }

    @Override
    public void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        tickCounter++;
        if (tickCounter < updateInterval.get()) return;
        tickCounter = 0;

        BlockPos pos = client.player.getBlockPos();
        RegistryEntry<Biome> biomeEntry = client.world.getBiome(pos);

        Optional<RegistryKey<Biome>> keyOpt = biomeEntry.getKey();
        if (keyOpt.isEmpty()) {
            cachedBiomeName = "Unknown";
            return;
        }

        RegistryKey<Biome> key = keyOpt.get();
        String translationKey = "biome." + key.getValue().getNamespace() + "." + key.getValue().getPath();
        cachedBiomeName = Text.translatable(translationKey).getString();
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        if (cachedBiomeName.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        String display = showLabel.get() ? "Biome: " + cachedBiomeName : cachedBiomeName;

        drawBackground(ctx, x, y, w, h);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(display), x + 4, y + (h - 8) / 2, textColor.get());
    }
}
