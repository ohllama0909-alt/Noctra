package de.snenjih.noctra.modules.impl.crosshair_customizer;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.IntSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.util.Identifier;

public class CrosshairCustomizerModule extends BaseModule {

    public static CrosshairCustomizerModule INSTANCE = null;

    public final ModuleSetting<Integer> color;
    public final ModuleSetting<Integer> size;
    public final ModuleSetting<Integer> thickness;
    public final ModuleSetting<Integer> gap;
    public final ModuleSetting<Boolean> dot;
    public final ModuleSetting<Integer> dotSize;
    public final ModuleSetting<Boolean> outline;
    public final ModuleSetting<Integer> outlineColor;
    public final ModuleSetting<Boolean> dynamicColor;
    public final ModuleSetting<Integer> enemyColor;

    public CrosshairCustomizerModule() {
        super(
            "crosshair_customizer",
            "Crosshair Customizer",
            "Replaces the vanilla crosshair with a fully customizable one.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/crosshair_customizer")
        );
        color        = addSetting(new ColorSetting("color",         "Color",         0xFFFFFFFF));
        size         = addSetting(new IntSetting("size",            "Size",          8, 2, 30));
        thickness    = addSetting(new IntSetting("thickness",       "Thickness",     2, 1, 6));
        gap          = addSetting(new IntSetting("gap",             "Gap",           2, 0, 10));
        dot          = addSetting(new BooleanSetting("dot",         "Center Dot",    true));
        dotSize      = addSetting(new IntSetting("dot_size",        "Dot Size",      2, 1, 6));
        outline      = addSetting(new BooleanSetting("outline",     "Outline",       false));
        outlineColor = addSetting(new ColorSetting("outline_color", "Outline Color", 0xFF000000));
        beginSection("Dynamic Color");
        dynamicColor = addSetting(new BooleanSetting("dynamic_color", "Dynamic Color",  false));
        enemyColor   = addSetting(new ColorSetting("enemy_color",     "Enemy Color",    0xFFFF5555));
    }

    @Override public void onEnable()  { INSTANCE = this; }
    @Override public void onDisable() { INSTANCE = null; }
}
