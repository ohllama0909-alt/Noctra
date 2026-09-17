package de.snenjih.noctra.modules.impl.server_address_hud;

import de.snenjih.noctra.modules.api.BaseHudModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import de.snenjih.noctra.modules.api.settings.TextSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;

public class ServerAddressHudModule extends BaseHudModule {

    private final ModuleSetting<Boolean> showIp;
    private final ModuleSetting<String>  customAlias;
    private final ModuleSetting<Boolean> hidePort;
    private final ModuleSetting<Boolean> onlyOnMultiplayer;
    private final ModuleSetting<Boolean> showOnSingleplayer;
    private final ModuleSetting<Integer> truncateLength;
    private final ModuleSetting<Boolean> showPing;
    private final ModuleSetting<String>  prefix;

    private String  cachedAddress   = null;
    private String  cachedName      = null;
    private boolean isSingleplayer  = false;

    public ServerAddressHudModule() {
        super(
            "server_address_hud",
            "Server Address",
            "Displays the current server address or name on the HUD.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/server_address_hud")
        );
        showIp             = addSetting(new BooleanSetting("show_ip",              "Show IP",               true));
        customAlias        = addSetting(new TextSetting   ("custom_alias",         "Custom Alias",          "", 32));
        hidePort           = addSetting(new BooleanSetting("hide_port",            "Hide Port",             true));
        onlyOnMultiplayer  = addSetting(new BooleanSetting("only_on_multiplayer",  "Only Multiplayer",      true));
        showOnSingleplayer = addSetting(new BooleanSetting("show_on_singleplayer", "Show in Singleplayer",  false));
        truncateLength     = addSetting(new IntSetting    ("truncate_length",      "Max Length",            30, 5, 64));
        showPing           = addSetting(new BooleanSetting("show_ping",            "Show Ping",             false));
        prefix             = addSetting(new TextSetting   ("prefix",               "Prefix",                "", 16));
    }

    @Override public String getHudId()       { return "server_address_hud"; }
    @Override public String getHudName()     { return "Server Address"; }
    @Override public int getDefaultWidth()   { return 150; }
    @Override public int getDefaultHeight()  { return 18; }

    @Override
    public void onJoinWorld(ClientWorld world) {
        MinecraftClient mc = MinecraftClient.getInstance();
        isSingleplayer = mc.isInSingleplayer();

        if (isSingleplayer) {
            if (mc.getServer() != null) {
                cachedName    = mc.getServer().getSaveProperties().getLevelName();
                cachedAddress = "Singleplayer";
            }
        } else {
            ServerInfo info = mc.getCurrentServerEntry();
            if (info != null) {
                cachedAddress = info.address;
                cachedName    = info.name;
            } else {
                // Direct Connect — fall back to NetworkHandler
                try {
                    var nh = mc.getNetworkHandler();
                    if (nh != null) {
                        var conn = nh.getConnection();
                        var addr = conn.getAddress();
                        if (addr != null) {
                            String raw = addr.toString();
                            // InetSocketAddress.toString() gives "hostname/ip:port" or "/ip:port"
                            int slash = raw.lastIndexOf('/');
                            cachedAddress = slash >= 0 ? raw.substring(slash + 1) : raw;
                            cachedName    = null;
                        } else {
                            cachedAddress = "Unknown";
                        }
                    }
                } catch (Exception e) {
                    cachedAddress = "Unknown";
                }
            }
        }
    }

    @Override
    public void onLeaveWorld() {
        cachedAddress = null;
        cachedName    = null;
    }

    @Override
    protected void renderHudContent(DrawContext ctx, float tickDelta, int x, int y, int w, int h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (isSingleplayer && onlyOnMultiplayer.get()) return;
        if (isSingleplayer && !showOnSingleplayer.get()) return;

        // Determine display text
        String display;
        String alias = customAlias.get();
        if (!alias.isEmpty()) {
            display = alias;
        } else if (!showIp.get() && cachedName != null && !cachedName.isEmpty()) {
            display = cachedName;
        } else if (cachedAddress != null) {
            display = cachedAddress;
            if (hidePort.get()) {
                display = display.replace(":25565", "");
            }
            int maxLen = truncateLength.get();
            if (display.length() > maxLen) {
                display = display.substring(0, maxLen) + "…";
            }
        } else {
            return; // Nothing to display
        }

        String fullText = prefix.get() + display;

        // Optionally append ping
        if (showPing.get() && !isSingleplayer && mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) {
                fullText += " [" + entry.getLatency() + "ms]";
            }
        }

        var tr = mc.textRenderer;
        int neededW = Math.max(w, tr.getWidth(fullText) + 8);
        drawBackground(ctx, x, y, neededW, h);
        ctx.drawTextWithShadow(tr, fullText, x + 4, y + 4, textColor.get());
    }
}
