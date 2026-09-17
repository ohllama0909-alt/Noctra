package de.snenjih.noctra.modules.impl.quick_messages;

import de.snenjih.noctra.config.QuickMessageConfig;
import de.snenjih.noctra.hud.NotificationManager;
import de.snenjih.noctra.hud.NotificationManager.Type;
import de.snenjih.noctra.input.KeybindManager;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class QuickMessagesModule extends BaseModule {

    public static QuickMessagesModule INSTANCE;

    private static final int SLOTS = 5;

    private final ModuleSetting<Boolean> keybindsActive;

    // Keybinds registered once per session in constructor
    private final KeyBinding[] keybinds = new KeyBinding[SLOTS + 1]; // index 1..5

    private QuickMessageConfig config;

    public QuickMessagesModule() {
        super(
            "quick_messages",
            "Quick Messages",
            "Send pre-defined chat messages via keybind or command.",
            ModuleCategory.CHAT,
            Identifier.of("noctra", "modules/quick_messages")
        );
        keybindsActive = addSetting(new BooleanSetting("keybinds_active", "Enable Keybinds", true));

        // Register keybinds once at construction time (Fabric does not support unregistration)
        for (int i = 1; i <= SLOTS; i++) {
            keybinds[i] = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.noctra.quick_msg_" + i,
                               GLFW.GLFW_KEY_UNKNOWN, KeybindManager.CATEGORY));
        }
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        config = new QuickMessageConfig();
        config.load();
    }

    @Override
    public void onDisable() {
        INSTANCE = null;
        config = null;
    }

    public QuickMessageConfig getConfig() { return config; }

    @Override
    public void onClientTick(MinecraftClient mc) {
        if (!keybindsActive.get() || mc.player == null || mc.currentScreen != null) return;
        for (int i = 1; i <= SLOTS; i++) {
            while (keybinds[i] != null && keybinds[i].wasPressed()) {
                sendSlot(mc, i);
            }
        }
    }

    public void sendSlot(MinecraftClient mc, int slot) {
        if (mc.player == null || mc.world == null) {
            NotificationManager.push("Not in a world.", Type.ERROR);
            return;
        }
        if (config == null) {
            NotificationManager.push("Quick Messages not loaded.", Type.ERROR);
            return;
        }
        String msg = config.get(slot);
        if (msg.isEmpty()) {
            NotificationManager.push("Slot " + slot + " is empty.", Type.ERROR);
            return;
        }
        // Trim to 256 chars max
        if (msg.length() > 256) {
            msg = msg.substring(0, 256);
            NotificationManager.push("Message trimmed to 256 chars.", Type.INFO);
        }
        mc.player.networkHandler.sendChatMessage(msg);
    }
}
