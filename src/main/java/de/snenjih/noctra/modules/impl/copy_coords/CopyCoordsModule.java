package de.snenjih.noctra.modules.impl.copy_coords;

import de.snenjih.noctra.hud.NotificationManager;
import de.snenjih.noctra.hud.NotificationManager.Type;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.EnumSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CopyCoordsModule extends BaseModule {

    public enum CoordsFormat {
        FULL, COMPACT, ARROW
    }

    private final ModuleSetting<Boolean>     postToChat;
    private final ModuleSetting<Boolean>     showNether;
    private final ModuleSetting<CoordsFormat> format;

    public CopyCoordsModule() {
        super(
            "copy_coords",
            "Copy Coords",
            "Copy your position to the clipboard with .coords.",
            ModuleCategory.CHAT,
            Identifier.of("noctra", "modules/copy_coords")
        );
        postToChat = addSetting(new BooleanSetting ("post_to_chat", "Post to Chat",       false));
        showNether = addSetting(new BooleanSetting ("show_nether",  "Show Nether Coords", true));
        format     = addSetting(new EnumSetting<>  ("format",       "Format", CoordsFormat.FULL, CoordsFormat.class));
    }

    public void execute(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            NotificationManager.push("Not in a world.", Type.ERROR);
            return;
        }

        BlockPos pos = mc.player.getBlockPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        boolean inNether = mc.world.getRegistryKey() == World.NETHER;

        String coords = formatCoords(x, y, z, format.get());

        String full = coords;
        if (showNether.get()) {
            if (inNether) {
                int ox = x * 8, oz = z * 8;
                full += "  [Overworld: " + formatCoords(ox, y, oz, format.get()) + "]";
            } else if (mc.world.getRegistryKey() == World.OVERWORLD) {
                int nx = x / 8, nz = z / 8;
                full += "  [Nether: " + formatCoords(nx, y, nz, format.get()) + "]";
            }
        }

        // Trim to 256 chars for chat safety
        if (full.length() > 256) full = full.substring(0, 256);

        // Copy to clipboard
        if (mc.keyboard != null) {
            mc.keyboard.setClipboard(full);
        }

        // Local feedback
        mc.inGameHud.getChatHud().addMessage(
            net.minecraft.text.Text.literal("§7[Noctra] §fCoords copied: §e" + full));

        // Post to chat if enabled
        if (postToChat.get()) {
            mc.player.networkHandler.sendChatMessage(full);
        }
    }

    private String formatCoords(int x, int y, int z, CoordsFormat fmt) {
        return switch (fmt) {
            case FULL    -> "X: " + x + "  Y: " + y + "  Z: " + z;
            case COMPACT -> x + ", " + y + ", " + z;
            case ARROW   -> x + " → " + y + " → " + z;
        };
    }
}
