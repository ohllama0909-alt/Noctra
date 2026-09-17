package de.snenjih.noctra.chat;

import com.google.gson.JsonPrimitive;
import de.snenjih.noctra.config.ModConfig;
import de.snenjih.noctra.hud.NotificationManager;
import de.snenjih.noctra.hud.NotificationManager.Type;
import de.snenjih.noctra.menu.ModuleRegistry;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.Module;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import de.snenjih.noctra.modules.impl.message_filter.MessageFilterModule;
import de.snenjih.noctra.modules.impl.quick_messages.QuickMessagesModule;
import de.snenjih.noctra.modules.impl.copy_coords.CopyCoordsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public final class ChatCommandDispatcher {

    private static final String PREFIX = ".";

    private ChatCommandDispatcher() {}

    /**
     * Handle a chat message. Returns true if the message was a client command
     * and should not be sent to the server.
     */
    public static boolean handle(String message) {
        if (!message.startsWith(PREFIX)) return false;
        String body = message.substring(PREFIX.length()).stripLeading();
        if (body.isEmpty()) return false;

        String[] parts = body.split(" ", 4);
        switch (parts[0]) {
            case "toggle"  -> handleToggle(parts);
            case "modules" -> handleModules();
            case "set"     -> handleSet(parts);
            case "help"    -> handleHelp();
            case "filter"  -> handleFilter(parts);
            case "qm"      -> handleQm(parts);
            case "coords"  -> handleCoords();
            default        -> NotificationManager.push("Unknown command: " + parts[0], Type.ERROR);
        }
        return true;
    }

    private static void handleToggle(String[] parts) {
        if (parts.length < 2 || parts[1].isBlank()) {
            NotificationManager.push("Usage: .toggle <module-id>", Type.ERROR);
            return;
        }
        Optional<Module> opt = ModuleRegistry.getInstance().getById(parts[1]);
        if (opt.isEmpty()) {
            NotificationManager.push("Module not found: " + parts[1], Type.ERROR);
            return;
        }
        Module m = opt.get();
        ModuleRegistry.getInstance().toggle(m);
        NotificationManager.push(
                m.getName() + (m.isEnabled() ? " enabled" : " disabled"),
                m.isEnabled() ? Type.SUCCESS : Type.INFO
        );
    }

    private static void handleModules() {
        var chatHud = MinecraftClient.getInstance().inGameHud.getChatHud();
        List<Module> modules = ModuleRegistry.getInstance().getAll();
        if (modules.isEmpty()) {
            chatHud.addMessage(Text.literal("§7[Noctra] No modules registered."));
            return;
        }
        chatHud.addMessage(Text.literal("§6[Noctra] Modules:"));
        for (Module m : modules) {
            String status = m.isEnabled() ? "§a[ON]§r" : "§c[OFF]§r";
            chatHud.addMessage(Text.literal("  " + status + " §f" + m.getName()
                    + " §8(" + m.getId() + ")"));
        }
    }

    private static void handleSet(String[] parts) {
        if (parts.length < 3) {
            NotificationManager.push("Usage: .set <id>.<setting> <value>", Type.ERROR);
            return;
        }
        String[] target = parts[1].split("\\.", 2);
        if (target.length < 2) {
            NotificationManager.push("Usage: .set <id>.<setting> <value>", Type.ERROR);
            return;
        }
        Optional<Module> opt = ModuleRegistry.getInstance().getById(target[0]);
        if (opt.isEmpty() || !(opt.get() instanceof BaseModule bm)) {
            NotificationManager.push("Module not found: " + target[0], Type.ERROR);
            return;
        }
        Optional<ModuleSetting<?>> settingOpt = bm.getSettings().stream()
                .filter(s -> s.getId().equals(target[1]))
                .findFirst();
        if (settingOpt.isEmpty()) {
            NotificationManager.push("Setting not found: " + target[1], Type.ERROR);
            return;
        }
        try {
            applyAndSave(bm, settingOpt.get(), parts[2].strip());
            NotificationManager.push(target[0] + "." + target[1] + " = " + parts[2].strip(),
                    Type.SUCCESS);
        } catch (Exception e) {
            NotificationManager.push("Invalid value: " + parts[2].strip(), Type.ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void applyAndSave(BaseModule module, ModuleSetting<T> setting, String raw) {
        T parsed = setting.fromJson(new JsonPrimitive(raw));
        setting.set(parsed);
        ModConfig.getInstance().saveModuleSettings(module);
    }

    private static void handleHelp() {
        var chatHud = MinecraftClient.getInstance().inGameHud.getChatHud();
        chatHud.addMessage(Text.literal("§6[Noctra] Commands:"));
        chatHud.addMessage(Text.literal("  §f.toggle <id>          §8toggle a module"));
        chatHud.addMessage(Text.literal("  §f.modules              §8list all modules"));
        chatHud.addMessage(Text.literal("  §f.set <id>.<key> <val> §8change a setting"));
        chatHud.addMessage(Text.literal("  §f.filter <add|remove|list|clear> §8manage chat filters"));
        chatHud.addMessage(Text.literal("  §f.qm <n|set|list|clear> §8quick messages"));
        chatHud.addMessage(Text.literal("  §f.coords               §8copy current position"));
        chatHud.addMessage(Text.literal("  §f.help                 §8show this help"));
    }

    // ---- .filter command -----------------------------------------------

    private static void handleFilter(String[] parts) {
        MessageFilterModule mod = MessageFilterModule.INSTANCE;
        if (mod == null || !mod.isEnabled()) {
            NotificationManager.push("Enable Message Filter first.", Type.ERROR);
            return;
        }
        if (parts.length < 2) {
            NotificationManager.push("Usage: .filter <add|remove|list|clear>", Type.ERROR);
            return;
        }
        var cfg = mod.getFilterConfig();
        switch (parts[1]) {
            case "add" -> {
                if (parts.length < 3 || parts[2].isBlank()) {
                    NotificationManager.push("Usage: .filter add <pattern>", Type.ERROR);
                    return;
                }
                boolean ok = cfg.add(parts[2]);
                NotificationManager.push(
                    ok ? "Filter added: " + parts[2] : "Filter list full (max " + cfg.maxPatterns() + ")",
                    ok ? Type.SUCCESS : Type.ERROR);
            }
            case "remove" -> {
                if (parts.length < 3) { NotificationManager.push("Usage: .filter remove <n>", Type.ERROR); return; }
                try {
                    int idx = Integer.parseInt(parts[2]) - 1;
                    boolean ok = cfg.remove(idx);
                    NotificationManager.push(ok ? "Filter removed." : "Invalid index.", ok ? Type.SUCCESS : Type.ERROR);
                } catch (NumberFormatException e) {
                    NotificationManager.push("Usage: .filter remove <n>", Type.ERROR);
                }
            }
            case "list" -> {
                var chatHud = MinecraftClient.getInstance().inGameHud.getChatHud();
                var pats = cfg.getPatterns();
                chatHud.addMessage(Text.literal("§6[Noctra] Filters (" + pats.size() + "/" + cfg.maxPatterns() + "):"));
                for (int i = 0; i < pats.size(); i++)
                    chatHud.addMessage(Text.literal("  §f" + (i + 1) + ". §7" + pats.get(i)));
            }
            case "clear" -> {
                cfg.clear();
                NotificationManager.push("All filters cleared.", Type.INFO);
            }
            default -> NotificationManager.push("Unknown filter sub-command.", Type.ERROR);
        }
    }

    // ---- .qm command ---------------------------------------------------

    private static void handleQm(String[] parts) {
        QuickMessagesModule mod = QuickMessagesModule.INSTANCE;
        if (mod == null || !mod.isEnabled()) {
            NotificationManager.push("Enable Quick Messages first.", Type.ERROR);
            return;
        }
        if (parts.length < 2) {
            NotificationManager.push("Usage: .qm <n> | set <n> <text> | list | clear <n>", Type.ERROR);
            return;
        }
        var cfg = mod.getConfig();
        switch (parts[1]) {
            case "set" -> {
                if (parts.length < 4) { NotificationManager.push("Usage: .qm set <n> <text>", Type.ERROR); return; }
                try {
                    int slot = Integer.parseInt(parts[2]);
                    boolean ok = cfg.set(slot, parts[3]);
                    NotificationManager.push(ok ? "Slot " + slot + " set." : "Invalid slot (1-5).", ok ? Type.SUCCESS : Type.ERROR);
                } catch (NumberFormatException e) {
                    NotificationManager.push("Slot must be a number (1-5).", Type.ERROR);
                }
            }
            case "list" -> {
                var chat = MinecraftClient.getInstance().inGameHud.getChatHud();
                chat.addMessage(Text.literal("§6[Noctra] Quick Messages:"));
                for (int i = 1; i <= cfg.slotCount(); i++) {
                    String msg = cfg.get(i);
                    String display = msg.isEmpty() ? "§8(empty)" : "§f" + msg;
                    chat.addMessage(Text.literal("  §7" + i + ". " + display));
                }
            }
            case "clear" -> {
                if (parts.length < 3) { NotificationManager.push("Usage: .qm clear <n>", Type.ERROR); return; }
                try {
                    int slot = Integer.parseInt(parts[2]);
                    boolean ok = cfg.set(slot, "");
                    NotificationManager.push(ok ? "Slot " + slot + " cleared." : "Invalid slot.", ok ? Type.SUCCESS : Type.ERROR);
                } catch (NumberFormatException e) {
                    NotificationManager.push("Slot must be a number (1-5).", Type.ERROR);
                }
            }
            default -> {
                try {
                    int slot = Integer.parseInt(parts[1]);
                    mod.sendSlot(MinecraftClient.getInstance(), slot);
                } catch (NumberFormatException e) {
                    NotificationManager.push("Usage: .qm <n> | set <n> <text> | list | clear <n>", Type.ERROR);
                }
            }
        }
    }

    // ---- .coords command -----------------------------------------------

    private static void handleCoords() {
        Optional<Module> opt = ModuleRegistry.getInstance().getById("copy_coords");
        if (opt.isEmpty() || !(opt.get() instanceof CopyCoordsModule mod)) {
            NotificationManager.push("copy_coords not registered.", Type.ERROR);
            return;
        }
        if (!mod.isEnabled()) {
            NotificationManager.push("Enable Copy Coords first.", Type.INFO);
            return;
        }
        mod.execute(MinecraftClient.getInstance());
    }
}
