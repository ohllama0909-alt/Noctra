package de.snenjih.noctra.modules.impl.mod_settings;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import net.minecraft.util.Identifier;

public class ModSettingsModule extends BaseModule {

    public static ModSettingsModule INSTANCE;

    private final FloatSetting transparency = new FloatSetting("transparency",  "Menu Transparency", 0.85f, 0.2f, 1.0f);
    private final ColorSetting primaryColor = new ColorSetting("primary_color", "Primary Color",     0xFF4A7CF8);
    private final ColorSetting menuBgColor  = new ColorSetting("menu_bg_color", "Menu Background",   0xFF0D1B2A);

    public ModSettingsModule() {
        super("mod_settings", "Mod Settings",
              "Global appearance settings for the Noctra client.",
              ModuleCategory.UTILITY,
              Identifier.of("noctra", "modules/mod_settings"));
        INSTANCE = this;
        addSetting(transparency);
        addSetting(primaryColor);
        addSetting(menuBgColor);
    }

    public float getTransparency() { return transparency.get(); }
    public int   getPrimaryColor() { return primaryColor.get(); }
    public int   getMenuBgColor()  { return menuBgColor.get(); }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}
}
