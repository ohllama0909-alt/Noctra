package de.snenjih.noctra.input;

import de.snenjih.noctra.menu.ModuleRegistry;
import de.snenjih.noctra.modules.api.Module;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class KeybindManager {

    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("noctra", "noctra"));

    private static final Map<KeyBinding, Module> BINDINGS = new LinkedHashMap<>();

    private KeybindManager() {}

    /**
     * Register a toggle keybind for a module. Call from the module's constructor,
     * before ModuleRegistry.register() is invoked.
     */
    public static KeyBinding register(Module module, String translationKey, int defaultGlfwKey) {
        KeyBinding kb = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(translationKey, defaultGlfwKey, CATEGORY));
        BINDINGS.put(kb, module);
        return kb;
    }

    /** Called once per client tick via ClientTickEvents.END_CLIENT_TICK. */
    public static void onTick() {
        BINDINGS.forEach((kb, module) -> {
            // Loop handles key events that stacked up during a lag spike
            while (kb.wasPressed()) {
                ModuleRegistry.getInstance().toggle(module);
            }
        });
    }
}
