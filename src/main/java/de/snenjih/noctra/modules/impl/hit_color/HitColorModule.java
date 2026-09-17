package de.snenjih.noctra.modules.impl.hit_color;

import de.snenjih.noctra.modules.api.BaseModule;
import de.snenjih.noctra.modules.api.ModuleCategory;
import de.snenjih.noctra.modules.api.settings.BooleanSetting;
import de.snenjih.noctra.modules.api.settings.ColorSetting;
import de.snenjih.noctra.modules.api.settings.FloatSetting;
import de.snenjih.noctra.modules.api.settings.ModuleSetting;
import net.minecraft.util.Identifier;

public class HitColorModule extends BaseModule {

    public static HitColorModule INSTANCE = null;

    public final ModuleSetting<Integer> hitColor;
    public final ModuleSetting<Float>   opacityFactor;
    public final ModuleSetting<Boolean> disableFlash;

    public HitColorModule() {
        super(
            "hit_color",
            "Hit Color",
            "Customize or disable the red hurt flash when taking damage.",
            ModuleCategory.VISUAL,
            Identifier.of("noctra", "modules/hit_color")
        );
        hitColor      = addSetting(new ColorSetting("hit_color",      "Hit Color",      0xFFFF0000));
        opacityFactor = addSetting(new FloatSetting("opacity_factor",  "Opacity Factor", 1.0f, 0.0f, 2.0f));
        disableFlash  = addSetting(new BooleanSetting("disable_flash", "Disable Flash",  false));
    }

    @Override public void onEnable()  { INSTANCE = this; }
    @Override public void onDisable() { INSTANCE = null; }
}
