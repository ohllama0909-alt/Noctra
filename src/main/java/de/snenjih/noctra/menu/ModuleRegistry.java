package de.snenjih.noctra.menu;

import de.snenjih.noctra.config.ModConfig;
import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.Module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ModuleRegistry {

    private static ModuleRegistry instance;

    private final List<Module> modules = new ArrayList<>();
    private final ModConfig    config;

    private ModuleRegistry(ModConfig config) {
        this.config = config;
    }

    public static ModuleRegistry create(ModConfig config) {
        instance = new ModuleRegistry(config);
        return instance;
    }

    public static ModuleRegistry getInstance() {
        if (instance == null) throw new IllegalStateException("ModuleRegistry not yet initialised");
        return instance;
    }

    public void register(Module module) {
        // Load settings before enabling so onEnable() can read them
        if (module instanceof BaseModule bm) {
            config.loadModuleSettings(bm);
        }
        boolean saved = config.isEnabled(module.getId(), false);
        module.setEnabled(saved); // BaseModule.setEnabled() triggers onEnable() if needed
        modules.add(module);
    }

    public void toggle(Module module) {
        boolean next = !module.isEnabled();
        module.setEnabled(next); // BaseModule.setEnabled() handles lifecycle calls
        config.setEnabled(module.getId(), next);
    }

    public List<Module> getAll() {
        return Collections.unmodifiableList(modules);
    }

    public Optional<Module> getById(String id) {
        return modules.stream().filter(m -> m.getId().equals(id)).findFirst();
    }

    public ModConfig getConfig() {
        return config;
    }
}
